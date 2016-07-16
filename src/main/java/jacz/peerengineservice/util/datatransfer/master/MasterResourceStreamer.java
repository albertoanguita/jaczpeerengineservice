package jacz.peerengineservice.util.datatransfer.master;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.datatransfer.DownloadProgressNotificationHandler;
import jacz.peerengineservice.util.datatransfer.GenericPriorityManagerStakeholder;
import jacz.peerengineservice.util.datatransfer.ResourceStreamingManager;
import jacz.peerengineservice.util.datatransfer.TransfersConfig;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceLink;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceProvider;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceWriter;
import jacz.peerengineservice.util.datatransfer.slave.ResourceChunk;
import org.aanguita.jacuzzi.concurrency.daemon.Daemon;
import org.aanguita.jacuzzi.concurrency.daemon.DaemonAction;
import org.aanguita.jacuzzi.concurrency.task_executor.ThreadExecutor;
import org.aanguita.jacuzzi.hash.HashFunction;
import org.aanguita.jacuzzi.id.AlphaNumFactory;
import org.aanguita.jacuzzi.notification.ProgressNotification;
import org.aanguita.jacuzzi.numeric.range.LongRange;
import org.aanguita.jacuzzi.numeric.range.LongRangeList;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class handles one resource download process. It communicates with all slaves offering the resource to
 */
public class MasterResourceStreamer extends GenericPriorityManagerStakeholder implements ResourceStreamingManager.SubchannelOwner {

    private final class WriteDaemon implements DaemonAction {

        private final ResourceStreamingManager resourceStreamingManager;

        public WriteDaemon(ResourceStreamingManager resourceStreamingManager) {
            this.resourceStreamingManager = resourceStreamingManager;
        }

        @Override
        public boolean solveState() {
            try {
                resourceStreamingManager.acquireWriteDataLock();
                List<WriteDataBuffer.DataElement> dataElements = writeDataBuffer.getDataElements();
                for (WriteDataBuffer.DataElement dataElement : dataElements) {
                    writeDataInBackground(dataElement);
                }
                return true;
            } finally {
                resourceStreamingManager.releaseWriteDataLock();
            }
        }
    }

    private static final String RESOURCE_WRITER_STATE_FIELD = "MASTER_RESOURCE_STREAMER@STATE";

    private static final String RESOURCE_WRITER_STREAMING_NEED_FIELD = "MASTER_RESOURCE_STREAMER@STREAMING_NEED";

    private static final String RESOURCE_WRITER_PRIORITY_FIELD = "MASTER_RESOURCE_STREAMER@PRIORITY_NEED";

    public static final String RESOURCE_WRITER_STORE_NAME_FIELD = "MASTER_RESOURCE_STREAMER@STORE_NAME";

    public static final String RESOURCE_WRITER_RESOURCE_ID_FIELD = "MASTER_RESOURCE_STREAMER@RESOURCE_ID";

    public static final String RESOURCE_WRITER_TOTAL_HASH_FIELD = "MASTER_RESOURCE_STREAMER@TOTAL_HASH";

    public static final String RESOURCE_WRITER_HASH_ALGORITHM_FIELD = "MASTER_RESOURCE_STREAMER@HASH_ALGORITHM";

    private static final float DEFAULT_PRIORITY = 10f;

    /**
     * private ID for proper hashing of objects
     */
    private final String id;

    /**
     * The resource streaming manager that created this object (will assign incoming channels)
     */
    private final ResourceStreamingManager resourceStreamingManager;

    /**
     * Interface for retrieving accuracy
     */
    private final TransfersConfig transfersConfig;

    /**
     * Download manager for controlling this download
     */
    private DownloadManager downloadManager;

    private final DownloadProgressNotificationHandler downloadProgressNotificationHandler;

    /**
     * Download reports object for sending events to the client
     */
    private DownloadReports downloadReports;

    /**
     * This attribute indicates if the download is performed from a unique peer (non null value) or from as many peers as possible (null)
     */
    private final PeerId specificPeerDownload;

    /**
     * Name of the store holding the resource
     */
    private final String storeName;

    /**
     * ID of the resource being downloaded (usually a hash)
     */
    private final String resourceId;

    /**
     * Size of the resource being downloaded (null means unknown)
     */
    private Long resourceSize;

    /**
     * Writer for the requested resource
     */
    private final ResourceWriter resourceWriter;

    /**
     * Buffer of data to be written
     */
    private final WriteDataBuffer writeDataBuffer;

    /**
     * Daemon for writing data in background
     */
    private final Daemon writeDaemon;

    /**
     * Scheduler that assigns segments to each active slave
     */
    private final ResourcePartScheduler resourcePartScheduler;

    /**
     * Slave controller objects, with their assigned subchannel
     */
    private final Map<Short, SlaveController> activeSlaves;

    private final String totalHash;

    private final String totalHashAlgorithm;

    /**
     * Priority for this download
     */
    private float priority;

    /**
     * Statistics of this download
     */
    private ResourceDownloadStatistics resourceDownloadStatistics;

    /**
     * Whether this resource download is active or paused (by the user)
     */
    private final AtomicBoolean active;

    /**
     * Whether this download is alive (is running or paused). A download stops being alive when it is stopped, cancelled or completed.
     * A dead download cannot be set alive again
     */
    private final AtomicBoolean alive;

    /**
     * The state of the download
     */
    private DownloadState state;

    private final String threadExecutorClientId;

    public MasterResourceStreamer(
            ResourceStreamingManager resourceStreamingManager,
            TransfersConfig transfersConfig,
            PeerId specificPeerDownload,
            String storeName,
            String resourceId,
            ResourceWriter resourceWriter,
            DownloadProgressNotificationHandler downloadProgressNotificationHandler,
            double streamingNeed,
            String totalHash,
            String totalHashAlgorithm) {
        this(
                resourceStreamingManager,
                transfersConfig,
                specificPeerDownload,
                storeName,
                resourceId,
                resourceWriter,
                downloadProgressNotificationHandler,
                streamingNeed,
                totalHash,
                totalHashAlgorithm,
                null);
    }

    public MasterResourceStreamer(MasterResourceStreamer oldMasterResourceStreamer, DownloadManager downloadManager) {
        this(
                oldMasterResourceStreamer.resourceStreamingManager,
                oldMasterResourceStreamer.transfersConfig,
                oldMasterResourceStreamer.specificPeerDownload,
                oldMasterResourceStreamer.storeName,
                oldMasterResourceStreamer.resourceId,
                oldMasterResourceStreamer.resourceWriter,
                oldMasterResourceStreamer.downloadProgressNotificationHandler,
                0d,
                oldMasterResourceStreamer.totalHash,
                oldMasterResourceStreamer.totalHashAlgorithm,
                downloadManager);
    }

    public MasterResourceStreamer(
            ResourceStreamingManager resourceStreamingManager,
            TransfersConfig transfersConfig,
            PeerId specificPeerDownload,
            String storeName,
            String resourceId,
            ResourceWriter resourceWriter,
            DownloadProgressNotificationHandler downloadProgressNotificationHandler,
            double streamingNeed,
            String totalHash,
            String totalHashAlgorithm,
            DownloadManager downloadManager
    ) {
        id = AlphaNumFactory.getStaticId();
        this.resourceStreamingManager = resourceStreamingManager;
        this.transfersConfig = transfersConfig;
        this.specificPeerDownload = specificPeerDownload;
        this.storeName = storeName;
        this.resourceId = resourceId;
        this.resourceWriter = resourceWriter;
        writeDataBuffer = new WriteDataBuffer();
        writeDaemon = new Daemon(new WriteDaemon(resourceStreamingManager));
        activeSlaves = new HashMap<>();
        LongRangeList availableSegments = null;
        priority = DEFAULT_PRIORITY;
        Exception initialWriteException = null;
        try {
            resourceSize = resourceWriter.getSize();
            availableSegments = resourceWriter.getAvailableSegments();
            Map<String, Serializable> downloadParameters = resourceWriter.getSystemDictionary();
            if (downloadParameters != null && downloadParameters.containsKey(RESOURCE_WRITER_STREAMING_NEED_FIELD) && downloadParameters.containsKey(RESOURCE_WRITER_PRIORITY_FIELD)) {
                // the resource writer had download parameters stored (from previous uses) -> ignore the given ones and use these
                streamingNeed = (Double) downloadParameters.get(RESOURCE_WRITER_STREAMING_NEED_FIELD);
                priority = (float) downloadParameters.get(RESOURCE_WRITER_PRIORITY_FIELD);
                state = (DownloadState) downloadParameters.get(RESOURCE_WRITER_STATE_FIELD);
            } else {
                // the resource writer had no download parameters stored, so this is the first time this resource writer is used
                // use the given streaming need and, additionally, store it in the resource writer
                resourceWriter.setSystemField(RESOURCE_WRITER_STREAMING_NEED_FIELD, streamingNeed);
                resourceWriter.setSystemField(RESOURCE_WRITER_PRIORITY_FIELD, priority);

                // in addition, write some information that can be used by the user: store name, resource id and hash info
                resourceWriter.setSystemField(RESOURCE_WRITER_STORE_NAME_FIELD, storeName);
                resourceWriter.setSystemField(RESOURCE_WRITER_RESOURCE_ID_FIELD, resourceId);
                resourceWriter.setSystemField(RESOURCE_WRITER_TOTAL_HASH_FIELD, totalHash);
                resourceWriter.setSystemField(RESOURCE_WRITER_HASH_ALGORITHM_FIELD, totalHashAlgorithm);
                setState(DownloadState.RUNNING, true);
            }
        } catch (IOException e) {
            initialWriteException = e;
        }
        if (downloadManager == null) {
            this.downloadManager = new DownloadManager(this, resourceStreamingManager);
        } else {
            this.downloadManager = downloadManager;
        }
        this.downloadProgressNotificationHandler = downloadProgressNotificationHandler;
        downloadReports = new DownloadReports(this.downloadManager, resourceId, storeName, downloadProgressNotificationHandler);
        resourcePartScheduler = new ResourcePartScheduler(this, transfersConfig, resourceSize, availableSegments, streamingNeed);
        this.totalHash = totalHash;
        this.totalHashAlgorithm = totalHashAlgorithm;
        active = new AtomicBoolean(state == DownloadState.RUNNING);
        alive = new AtomicBoolean(state != DownloadState.STOPPED);
        if (alive.get()) {
            try {
                resourceDownloadStatistics = new ResourceDownloadStatistics(resourceWriter);
                if (downloadManager == null) {
                    // this is a new download -> report start
                    downloadReports.initializeWriting();
                } else {
                    // this is an existing stopped download that was resumed -> report file resumed
                    downloadReports.reportResumed();
                }
            } catch (IOException e) {
                initialWriteException = e;
            }
        }
        //state = DownloadState.RUNNING;
        threadExecutorClientId = ThreadExecutor.registerClient(this.getClass().getName());
        if (initialWriteException != null) {
            reportErrorWriting(initialWriteException);
        }
    }

    private void initializeWritingReport() throws IOException {
        downloadReports.initializeWriting();
    }

    void setState(DownloadState downloadState, boolean writeThrough) {
        this.state = downloadState;
        if (writeThrough) {
            try {
                resourceWriter.setSystemField(RESOURCE_WRITER_STATE_FIELD, downloadState);
            } catch (IOException e) {
                // error writing the state in the resource writer -> cancel download and report error
                reportErrorWriting(e);
            }
        }
    }

    public DownloadManager getDownloadManager() {
        return downloadManager;
    }

    public ResourceWriter getResourceWriter() {
        return resourceWriter;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "MasterResourceStreamer{" +
                "resId=" + resourceId +
                '}';
    }

    public String getStoreName() {
        return storeName;
    }

    public String getResourceId() {
        return resourceId;
    }

    public synchronized Long getResourceSize() {
        return resourceSize;
    }

    public PeerId getSpecificPeerDownload() {
        return specificPeerDownload;
    }

    public synchronized DownloadState getState() {
        return state;
    }

    DownloadProgressNotificationHandler getDownloadProgressNotificationHandler() {
        return downloadProgressNotificationHandler;
    }

    String getTotalHash() {
        return totalHash;
    }

    String getTotalHashAlgorithm() {
        return totalHashAlgorithm;
    }

    public ResourceDownloadStatistics getStatistics() {
        return resourceDownloadStatistics;
    }

    /**
     * Provides information about all resource providers offering this resource. This method will check if there is any
     * new provider, and use it if so. The collection given as parameter will not be modified. This method is thread-safe
     *
     * @param resourceProviders collection of resource providers offering the desired resource
     */
    public void reportAvailableResourceProviders(final Collection<? extends ResourceProvider> resourceProviders) {
        ThreadExecutor.submit(() -> reportAvailableResourceProvidersSynch(resourceProviders));
    }

    /**
     * Not thread-safe implementation of the previous method.
     *
     * @param resourceProviders collection of resource providers offering the desired resource
     */
    private synchronized void reportAvailableResourceProvidersSynch(Collection<? extends ResourceProvider> resourceProviders) {
        if (alive.get()) {
            // add the resource providers which are not active providers or active requests
            Set<PeerId> activeResourceProviders = getActiveResourceProviders();
            for (ResourceProvider resourceProvider : resourceProviders) {
                // check that the given resource provider is not null, as the resource streaming manager might include null providers due to those
                // not being available
                if (resourceProvider != null && !activeResourceProviders.contains(resourceProvider.getPeerId())) {
                    newResourceProvider(resourceProvider);
                }
            }
        }
    }

    /**
     * This method gives a set of the already registered active slaves
     *
     * @return a set containing resource providers corresponding to the currently active slaves
     */
    private synchronized Set<PeerId> getActiveResourceProviders() {
        Set<PeerId> activeResourceProviders = new HashSet<>(activeSlaves.size());
        for (SlaveController slave : activeSlaves.values()) {
            activeResourceProviders.add(slave.getResourceProviderId());
        }
        return activeResourceProviders;
    }

    /**
     * Adds a new resource provider to the list of active slaves
     *
     * @param resourceProvider the new resource provider
     */
    private synchronized void newResourceProvider(ResourceProvider resourceProvider) {
        // try to grab a subchannel for it and submit a request for a resource
        // link. A corresponding slave controller is created for the new resource link
        Short subchannel = resourceStreamingManager.requestIncomingSubchannel(this);
        if (subchannel != null) {
            ResourceLink resourceLink = resourceProvider.requestResource(storeName, resourceId, subchannel);
            addSlave(resourceLink, resourceProvider, subchannel);
        }
    }

    private synchronized void addSlave(ResourceLink resourceLink, ResourceProvider resourceProvider, short subchannel) {
        final SlaveController slaveController = new SlaveController(this, resourceSize != null, resourceLink, resourceProvider, subchannel, resourceLink.recommendedMillisForRequest(), resourcePartScheduler, active.get());
        activeSlaves.put(subchannel, slaveController);
        ThreadExecutor.submit(() -> resourceStreamingManager.getDownloadPriorityManager().addRegulatedResource(MasterResourceStreamer.this, slaveController));
    }

    /**
     * Removes an existing resource provider
     *
     * @param subchannel      subchannel that this resource provider owns
     * @param mustReportSlave boolean variable indicating if the slave must be notified about his removal
     */
    synchronized void removeSlave(final short subchannel, boolean mustReportSlave) {
        System.out.println("Removing slave at " + subchannel);
        // remove provider from active providers list and free subchannel
        if (activeSlaves.containsKey(subchannel)) {
            if (mustReportSlave) {
                activeSlaves.get(subchannel).getResourceLink().die();
            }
            final SlaveController slaveController = activeSlaves.remove(subchannel);
            System.out.println("slave: " + slaveController);
            ThreadExecutor.submit(slaveController::finishExecution);
            resourceStreamingManager.freeSubchannel(subchannel);
            ThreadExecutor.submit(() -> resourceStreamingManager.getDownloadPriorityManager().removeRegulatedResource(MasterResourceStreamer.this, slaveController));
        }
        System.out.println("Finished removing slave at " + subchannel);
    }

    /**
     * Incoming object message. It is redirected to the corresponding slave. The same thread invokes this method every time
     *
     * @param subchannel subchannel by which the message arrived
     * @param message    actual message
     */
    public synchronized void processMessage(short subchannel, Object message) {
        if (alive.get()) {
            // it this slave is still waiting for an initialization message, give it to him, as it presumably such message.
            // if not, give it to the slave as well for proper processing
            if (activeSlaves.containsKey(subchannel)) {
                activeSlaves.get(subchannel).processMessage(message);
            }
        }
    }

    /**
     * Incoming byte array message. It is redirected to the corresponding slave. The same thread invokes this method every time
     *
     * @param subchannel subchannel by which the message arrived
     * @param data       actual message
     */
    public synchronized void processMessage(short subchannel, byte[] data) {
        if (alive.get()) {
            // if this slave is still waiting for initialization data, kill it as we did not expect bytes. Otherwise,
            // give it to the slave so he processes it
            if (activeSlaves.containsKey(subchannel)) {
                activeSlaves.get(subchannel).processMessage(data);
            }
        }
    }

    void reportAddedProvider(ResourceProvider resourceProvider) {
        ProviderStatistics providerStatistics = resourceDownloadStatistics.addProvider(resourceProvider);
        downloadReports.addProvider(providerStatistics, resourceProvider.getPeerId());
    }

    void reportSetProviderShare(ResourceProvider resourceProvider, ResourcePart sharedPart) {
        ProviderStatistics providerStatistics = resourceDownloadStatistics.reportSharedPart(resourceProvider, sharedPart);
        downloadReports.reportSharedPart(providerStatistics, sharedPart);
    }

    void reportRemovedProvider(ResourceProvider resourceProvider) {
        ProviderStatistics providerStatistics = resourceDownloadStatistics.removeProvider(resourceProvider);
        downloadReports.removeProvider(providerStatistics, resourceProvider.getPeerId());
    }

    void reportAssignedProviderSegment(ResourceProvider resourceProvider, LongRange assignedSegment) {
        ProviderStatistics providerStatistics = resourceDownloadStatistics.reportAssignedPart(resourceProvider, assignedSegment);
        downloadReports.reportAssignedSegment(providerStatistics, assignedSegment);
    }

    void reportClearedProviderAssignation(ResourceProvider resourceProvider) {
        ProviderStatistics providerStatistics = resourceDownloadStatistics.reportClearedAssignation(resourceProvider);
        downloadReports.reportClearedAssignation(providerStatistics);
    }

    /**
     * We are notified about the size of the resource, invoked by a slave controller
     *
     * @param size resource size in bytes
     */
    synchronized void reportResourceSize(long size) {
        if (resourceSize == null) {
            // update size and let know all slaves that we know the size
            resourceSize = size;
            try {
                resourceWriter.init(resourceSize);
            } catch (IOException e) {
                reportErrorWriting(e);
            }
            resourcePartScheduler.reportResourceSize(size);
            for (SlaveController slaveController : activeSlaves.values()) {
                slaveController.reportSizeIsKnown();
            }
            downloadReports.reportResourceSize(size);
        }
    }

    /**
     * Write a data chunk to the resource writer, invoked by a slave controller
     *
     * @param resourceChunk the resource chunk to write to disk
     * @throws IllegalArgumentException the received chunk has something wrong in it
     */
    synchronized void writeData(ResourceChunk resourceChunk) throws IllegalArgumentException {
        if (resourceSize != null) {
//                resourceWriter.write(resourceChunk.getFirstByte(), resourceChunk.getData());
            writeDataBuffer.addResourceChunk(resourceChunk);
            writeDaemon.stateChange();
        }
    }

    private void writeDataInBackground(WriteDataBuffer.DataElement dataElement) {
        try {
            resourceWriter.write(dataElement.firstByte, dataElement.data);
        } catch (IOException | IndexOutOfBoundsException e) {
            reportErrorWriting(e);
        }
    }

    private void flushWriteData() {
        writeDaemon.stateChange();
        writeDaemon.blockUntilStateIsSolved();
        writeDaemon.stop();
    }

    synchronized void reportDownloadedSegment(ResourceProvider resourceProvider, LongRange downloadedSegment) {
        resourceStreamingManager.reportDownloadedSize(resourceProvider.getPeerId(), downloadedSegment.size());
        resourceDownloadStatistics.reportDownloadedPart(resourceProvider, downloadedSegment);
        downloadReports.reportDownloadedSegment(downloadedSegment);
    }

    /**
     * The download is complete, so resources must be freed and notifications must be submitted. Invoked by the scheduler, from a slave controller
     * thread
     */
    synchronized void reportDownloadComplete() {
        if (alive.get()) {
            try {
                flushWriteData();
                resourceWriter.complete();
                checkTotalHash();
                resourceDownloadStatistics.downloadComplete(resourceSize);
                resourceDownloadStatistics.stop();
                downloadReports.reportCompleted(resourceWriter);
                setState(DownloadState.COMPLETED, false);
            } catch (IOException e) {
                reportErrorWriting(e);
            } finally {
                freeAssignedResources();
            }
        }
    }

    private void checkTotalHash() {
        if (totalHash != null) {
            File file = new File(resourceWriter.getPath());
            HashFunction hashFunction;
            try {
                hashFunction = new HashFunction(totalHashAlgorithm);
            } catch (NoSuchAlgorithmException e) {
                reportInvalidHashAlgorithm(null, totalHashAlgorithm);
                return;
            }
            String hash;
            try {
                hashFunction.update(file, new ProgressNotification<Integer>() {
                    @Override
                    public void beginTask() {
                        // do nothing
                    }

                    @Override
                    public void addNotification(Integer message) {
                        downloadReports.reportCheckingTotalHash(message);
                    }

                    @Override
                    public void completeTask() {
                        // do nothing
                    }
                });
                hash = hashFunction.digestAsHex();
            } catch (IOException e) {
                // put a fixed hash, the hash comparison will fail
                hash = "";
            }
            if (totalHash.equalsIgnoreCase(hash)) {
                downloadReports.reportCorrectTotalHash();
            } else {
                downloadReports.reportFailedTotalHash();
            }
        }
    }

    synchronized void reportInvalidHashAlgorithm(LongRange segment, String hashAlgorithm) {
        downloadReports.reportInvalidTotalHashAlgorithm(hashAlgorithm);
    }

    /**
     * There was an error writing the resource
     */
    private synchronized void reportErrorWriting(Exception e) {
        if (alive.get()) {
            cancel(DownloadProgressNotificationHandler.CancellationReason.IO_FAILURE, e);
        }
    }

    /**
     * The user pauses the download
     */
    synchronized void pause() {
        if (alive.get() && active.get()) {
            active.set(false);
            downloadReports.reportPaused();
            setState(DownloadState.PAUSED, true);
            for (SlaveController slaveController : activeSlaves.values()) {
                slaveController.pause();
            }
        }
    }

    /**
     * The user resumes the download
     */
    synchronized void resume() {
        if (alive.get() && !active.get()) {
            active.set(true);
            downloadReports.reportResumed();
            setState(DownloadState.RUNNING, true);
            for (SlaveController slaveController : activeSlaves.values()) {
                slaveController.resume();
            }
        }
    }

    /**
     * The user cancels the download
     */
    synchronized void cancel(DownloadProgressNotificationHandler.CancellationReason cancellationReason, Exception e) {
        if (alive.get()) {
            try {
                flushWriteData();
                resourceWriter.cancel();
                resourceDownloadStatistics.stop();
                downloadReports.reportCancelled(cancellationReason, e);
                setState(DownloadState.CANCELLED, false);
            } finally {
                freeAssignedResources();
            }
        }
    }

    /**
     * The user stops the download
     */
    synchronized void stop(boolean writeStateThrough) {
        if (alive.get()) {
            try {
                flushWriteData();
                resourceWriter.stop();
                resourceDownloadStatistics.stopSession();
                downloadReports.reportStopped();
                setState(DownloadState.STOPPED, writeStateThrough);
            } catch (IOException e) {
                // error saving the download session -> cancel the download
                cancel(DownloadProgressNotificationHandler.CancellationReason.IO_FAILURE, e);
            } finally {
                freeAssignedResources();
            }
        }
    }

    /**
     * Return the current streaming need for this resource
     *
     * @return the streaming need for this resource
     */
    public synchronized float getMasterPriority() {
        return priority;
    }

    synchronized float getSlaveControllerAchievedSpeed(SlaveController slaveController) {
        ProviderStatistics providerStatistics = resourceDownloadStatistics.getProviders().get(slaveController.getResourceProvider().getPeerId());
        if (providerStatistics != null) {
            return (float) providerStatistics.getSpeed();
        } else {
            return 0f;
        }
    }

    /**
     * Sets the streaming need for this resource
     *
     * @param priority the new priority, between 0 and 10000
     */
    synchronized void setPriority(float priority) {
        if (alive.get()) {
            this.priority = priority;
            try {
                resourceWriter.setSystemField(RESOURCE_WRITER_PRIORITY_FIELD, priority);
            } catch (IOException e) {
                // error writing the streaming need in the resource writer -> cancel download and report error
                reportErrorWriting(e);
            }
        }
    }

    /**
     * Return the current streaming need for this resource
     *
     * @return the streaming need for this resource
     */
    synchronized double getStreamingNeed() {
        return resourcePartScheduler.getStreamingNeed();
    }

    /**
     * Sets the streaming need for this resource
     *
     * @param streamingNeed the new streaming need, between 0 and 1
     */
    synchronized void setStreamingNeed(double streamingNeed) {
        if (alive.get()) {
            resourcePartScheduler.setStreamingNeed(streamingNeed);
            try {
                resourceWriter.setSystemField(RESOURCE_WRITER_STREAMING_NEED_FIELD, streamingNeed);
            } catch (IOException e) {
                // error writing the streaming need in the resource writer -> cancel download and report error
                reportErrorWriting(e);
            }
        }
    }

    /**
     * Remove all slaves and free all resources (subchannels). The download dies
     */
    private synchronized void freeAssignedResources() {
        if (alive.getAndSet(false)) {
            // free all subchannels and report the ResourceStreamingManager that this download must be removed. We parallelize this call to avoid
            // locks
            ThreadExecutor.submit(() -> {
                resourceStreamingManager.freeAllSubchannels(MasterResourceStreamer.this);
                resourceStreamingManager.removeDownload(MasterResourceStreamer.this);
            });
            Collection<SlaveController> slavesToRemove = new HashSet<>(activeSlaves.values());
            for (SlaveController slaveController : slavesToRemove) {
                removeSlave(slaveController.getSubchannel(), true);
            }
            downloadReports.stop();
            ThreadExecutor.shutdownClient(threadExecutorClientId);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MasterResourceStreamer that = (MasterResourceStreamer) o;

        return !(id != null ? !id.equals(that.id) : that.id != null);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

//    @Override
//    protected void finalize() throws Throwable {
//        super.finalize();
//        freeAssignedResources();
//    }

    @Override
    public float getPriority() {
        return getMasterPriority();
    }
}
