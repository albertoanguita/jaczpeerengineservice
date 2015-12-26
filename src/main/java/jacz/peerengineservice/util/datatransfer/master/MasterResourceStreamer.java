package jacz.peerengineservice.util.datatransfer.master;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.datatransfer.*;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceLink;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceProvider;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceWriter;
import jacz.peerengineservice.util.datatransfer.slave.ResourceChunk;
import jacz.util.concurrency.daemon.Daemon;
import jacz.util.concurrency.daemon.DaemonAction;
import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.concurrency.task_executor.ParallelTaskExecutor;
import jacz.util.hash.HashFunction;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.identifier.UniqueIdentifierFactory;
import jacz.util.notification.ProgressNotification;
import jacz.util.numeric.range.LongRange;
import jacz.util.numeric.range.LongRangeList;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.*;

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

    private static final String RESOURCE_WRITER_STREAMING_NEED_FIELD = "MASTER_RESOURCE_STREAMER@STREAMING_NEED";

    private static final String RESOURCE_WRITER_PRIORITY_FIELD = "MASTER_RESOURCE_STREAMER@PRIORITY_NEED";

    private static final float DEFAULT_PRIORITY = 10f;

    /**
     * private ID for proper hashing of objects
     */
    private final UniqueIdentifier id;

    /**
     * The resource streaming manager that created this object (will assign incoming channels)
     */
    private final ResourceStreamingManager resourceStreamingManager;

    /**
     * Download manager for controlling this download
     */
    private DownloadManager downloadManager;

    /**
     * Download reports object for sending events to the client
     */
    private DownloadReports downloadReports;

    /**
     * This attribute indicates if the download is performed from a unique peer (non null value) or from as many peers as possible (null)
     */
    private final PeerID specificPeerDownload;

    /**
     * Name of the store holding the resource
     */
    private final String storeName;

    /**
     * ID of the resource being downloaded (usually a hash)
     */
    private final String resourceID;

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
     * Whether this resource download is active or paused (by the user)
     */
    private boolean active;

    /**
     * Whether this download is alive (is running or paused). A download stops being alive when it is stopped, cancelled or completed.
     * A dead download cannot be set alive again
     */
    private boolean alive;

    /**
     * The state of the download
     */
    private DownloadState state;


    public MasterResourceStreamer(
            ResourceStreamingManager resourceStreamingManager,
            PeerID specificPeerDownload,
            String storeName,
            String resourceID,
            ResourceWriter resourceWriter,
            DownloadProgressNotificationHandler downloadProgressNotificationHandler,
            double streamingNeed,
            String totalHash,
            String totalHashAlgorithm) {
        id = UniqueIdentifierFactory.getOneStaticIdentifier();
        this.resourceStreamingManager = resourceStreamingManager;
        this.specificPeerDownload = specificPeerDownload;
        this.storeName = storeName;
        this.resourceID = resourceID;
        this.resourceWriter = resourceWriter;
        writeDataBuffer = new WriteDataBuffer();
        writeDaemon = new Daemon(new WriteDaemon(resourceStreamingManager));
        activeSlaves = new HashMap<>();
        LongRangeList availableSegments = null;
        priority = DEFAULT_PRIORITY;
        boolean error = false;
        try {
            resourceSize = resourceWriter.getSize();
            availableSegments = resourceWriter.getAvailableSegments();
            Map<String, Serializable> downloadParameters = resourceWriter.getSystemDictionary();
            if (downloadParameters != null && downloadParameters.containsKey(RESOURCE_WRITER_STREAMING_NEED_FIELD) && downloadParameters.containsKey(RESOURCE_WRITER_PRIORITY_FIELD)) {
                // the resource writer had download parameters stored (from previous uses) -> ignore the given ones and use this
                streamingNeed = (Double) downloadParameters.get(RESOURCE_WRITER_STREAMING_NEED_FIELD);
                priority = (float) downloadParameters.get(RESOURCE_WRITER_PRIORITY_FIELD);
            } else {
                // the resource writer had no download parameters stored, so this is the first time this resource writer is used
                // use the given streaming need and, additionally, store it in the resource writer
                resourceWriter.setSystemField(RESOURCE_WRITER_STREAMING_NEED_FIELD, streamingNeed);
                resourceWriter.setSystemField(RESOURCE_WRITER_PRIORITY_FIELD, priority);
            }
        } catch (IOException e) {
            error = true;
        }
        downloadManager = new DownloadManager(this);
        downloadReports = new DownloadReports(downloadManager, resourceID, storeName, downloadProgressNotificationHandler);
        try {
            downloadReports.initializeWriting(resourceWriter);
        } catch (IOException e) {
            error = true;
        }
        resourcePartScheduler = new ResourcePartScheduler(this, resourceStreamingManager, resourceSize, availableSegments, streamingNeed);
        this.totalHash = totalHash;
        this.totalHashAlgorithm = totalHashAlgorithm;
        active = true;
        alive = true;
        state = DownloadState.RUNNING;
        if (error) {
            reportErrorWriting();
        }
    }

    public DownloadManager getDownloadManager() {
        return downloadManager;
    }

    public ResourceWriter getResourceWriter() {
        return resourceWriter;
    }

    public UniqueIdentifier getId() {
        return id;
    }

    @Override
    public String toString() {
        return "MasterResourceStreamer{" +
                "resId=" + resourceID +
                '}';
    }

    public String getStoreName() {
        return storeName;
    }

    public String getResourceID() {
        return resourceID;
    }

    public synchronized Long getResourceSize() {
        return resourceSize;
    }

    public PeerID getSpecificPeerDownload() {
        return specificPeerDownload;
    }

    public synchronized DownloadState getState() {
        return state;
    }

    public ResourceDownloadStatistics getStatistics() {
        return downloadReports.getResourceDownloadStatistics();
    }

    /**
     * Provides information about all resource providers offering this resource. This method will check if there is any
     * new provider, and use it if so. The collection given as parameter will not be modified. This method is thread-safe
     *
     * @param resourceProviders collection of resource providers offering the desired resource
     */
    public void reportAvailableResourceProviders(final Collection<? extends ResourceProvider> resourceProviders) {
        ParallelTask parallelTask = new ParallelTask() {
            @Override
            public void performTask() {
                reportAvailableResourceProvidersSynch(resourceProviders);
            }
        };
        ParallelTaskExecutor.executeTask(parallelTask);
    }

    /**
     * Not thread-safe implementation of the previous method.
     *
     * @param resourceProviders collection of resource providers offering the desired resource
     */
    private synchronized void reportAvailableResourceProvidersSynch(Collection<? extends ResourceProvider> resourceProviders) {
        if (alive) {
            // add the resource providers which are not active providers or active requests
            Set<PeerID> activeResourceProviders = getActiveResourceProviders();
            for (ResourceProvider resourceProvider : resourceProviders) {
                // check that the given resource provider is not null, as the resource streaming manager might include null providers due to those
                // not being available
                if (resourceProvider != null && !activeResourceProviders.contains(resourceProvider.getPeerID())) {
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
    private synchronized Set<PeerID> getActiveResourceProviders() {
        Set<PeerID> activeResourceProviders = new HashSet<>(activeSlaves.size());
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
            ResourceLink resourceLink = resourceProvider.requestResource(storeName, resourceID, subchannel);
            addSlave(resourceLink, resourceProvider, subchannel);
        }
    }

    private synchronized void addSlave(ResourceLink resourceLink, ResourceProvider resourceProvider, short subchannel) {
        final SlaveController slaveController = new SlaveController(this, resourceSize != null, resourceLink, resourceProvider, subchannel, resourceLink.recommendedMillisForRequest(), resourcePartScheduler, active);
        activeSlaves.put(subchannel, slaveController);
        ParallelTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceStreamingManager.getDownloadPriorityManager().addRegulatedResource(MasterResourceStreamer.this, slaveController);
            }
        });
    }

    /**
     * Removes an existing resource provider
     *
     * @param subchannel      subchannel that this resource provider owns
     * @param mustReportSlave boolean variable indicating if the slave must be notified about his removal
     */
    synchronized void removeSlave(final short subchannel, boolean mustReportSlave) {
        // remove provider from active providers list and free subchannel
        if (activeSlaves.containsKey(subchannel)) {
            if (mustReportSlave) {
                activeSlaves.get(subchannel).getResourceLink().die();
            }
            final SlaveController slaveController = activeSlaves.remove(subchannel);
            ParallelTaskExecutor.executeTask(new ParallelTask() {
                @Override
                public void performTask() {
                    slaveController.finishExecution();
                }
            });
            resourceStreamingManager.freeSubchannel(subchannel);
            ParallelTaskExecutor.executeTask(new ParallelTask() {
                @Override
                public void performTask() {
                    resourceStreamingManager.getDownloadPriorityManager().removeRegulatedResource(MasterResourceStreamer.this, slaveController);
                }
            });
        }
    }

    /**
     * Incoming object message. It is redirected to the corresponding slave. The same thread invokes this method every time
     *
     * @param subchannel subchannel by which the message arrived
     * @param message    actual message
     */
    public synchronized void processMessage(short subchannel, Object message) {
        if (alive) {
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
        if (alive) {
            // if this slave is still waiting for initialization data, kill it as we did not expect bytes. Otherwise,
            // give it to the slave so he processes it
            if (activeSlaves.containsKey(subchannel)) {
                activeSlaves.get(subchannel).processMessage(data);
            }
        }
    }

    void reportAddedProvider(ResourceProvider resourceProvider) {
        downloadReports.addProvider(resourceProvider);
    }

    void reportSetProviderShare(ResourceProvider resourceProvider, ResourcePart sharedPart) {
        downloadReports.reportSharedPart(resourceProvider, sharedPart);
    }

    void reportRemovedProvider(ResourceProvider resourceProvider) {
        downloadReports.removeProvider(resourceProvider);
    }

    void reportAssignedProviderSegment(ResourceProvider resourceProvider, LongRange assignedSegment) {
        downloadReports.reportAssignedSegment(resourceProvider, assignedSegment);
    }

    void reportClearedProviderAssignation(ResourceProvider resourceProvider) {
        downloadReports.reportClearedAssignation(resourceProvider);
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
                reportErrorWriting();
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
        } catch (IOException e) {
            reportErrorWriting();
        }
    }

    private void flushWriteData() {
        writeDaemon.stateChange();
        writeDaemon.blockUntilStateIsSolved();
    }

    synchronized void reportDownloadedSegment(ResourceProvider resourceProvider, LongRange downloadedSegment) {
        resourceStreamingManager.reportDownloadedSize(resourceProvider.getPeerID(), downloadedSegment.size());
        downloadReports.reportDownloadedSegment(resourceProvider, downloadedSegment);
    }

    /**
     * The download is complete, so resources must be freed and notifications must be submitted. Invoked by the scheduler, from a slave controller
     * thread
     */
    synchronized void reportDownloadComplete() {
        if (alive) {
            try {
                flushWriteData();
                resourceWriter.complete();
                checkTotalHash();
                downloadReports.reportCompleted(resourceWriter, getResourceSize());
                state = DownloadState.COMPLETED;
            } catch (IOException e) {
                reportErrorWriting();
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
    private synchronized void reportErrorWriting() {
        if (alive) {
            cancel(DownloadProgressNotificationHandler.CancellationReason.IO_FAILURE);
        }
    }

    /**
     * The user pauses the download
     */
    synchronized void pause() {
        if (alive && active) {
            active = false;
            downloadReports.reportPaused();
            state = DownloadState.PAUSED;
            for (SlaveController slaveController : activeSlaves.values()) {
                slaveController.pause();
            }
        }
    }

    /**
     * The user resumes the download
     */
    synchronized void resume() {
        if (alive && !active) {
            active = true;
            downloadReports.reportResumed();
            state = DownloadState.RUNNING;
            for (SlaveController slaveController : activeSlaves.values()) {
                slaveController.resume();
            }
        }
    }

    /**
     * The user cancels the download
     */
    synchronized void cancel(DownloadProgressNotificationHandler.CancellationReason cancellationReason) {
        if (alive) {
            freeAssignedResources();
            flushWriteData();
            resourceWriter.cancel();
            alive = false;
            downloadReports.reportCancelled(cancellationReason);
            state = DownloadState.CANCELLED;
        }
    }

    /**
     * The user stops the download
     */
    synchronized void stop() {
        if (alive) {
            freeAssignedResources();
            flushWriteData();
            resourceWriter.stop();
            alive = false;
            try {
                downloadReports.reportStopped();
                state = DownloadState.STOPPED;
            } catch (IOException e) {
                // error saving the download session -> cancel the download
                cancel(DownloadProgressNotificationHandler.CancellationReason.IO_FAILURE);
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
        ProviderStatistics providerStatistics = downloadReports.getResourceDownloadStatistics().getProviders().get(slaveController.getResourceProvider().getPeerID());
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
        if (alive) {
            this.priority = priority;
            try {
//                resourceWriter.setCustomGroupField(RESOURCE_WRITER_MASTER_GROUP, RESOURCE_WRITER_PRIORITY_FIELD, priority);
                resourceWriter.setSystemField(RESOURCE_WRITER_PRIORITY_FIELD, priority);
            } catch (IOException e) {
                // error writing the streaming need in the resource writer -> cancel download and report error
                reportErrorWriting();
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
        if (alive) {
            resourcePartScheduler.setStreamingNeed(streamingNeed);
            try {
//                resourceWriter.setCustomGroupField(RESOURCE_WRITER_MASTER_GROUP, RESOURCE_WRITER_STREAMING_NEED_FIELD, streamingNeed);
                resourceWriter.setSystemField(RESOURCE_WRITER_STREAMING_NEED_FIELD, streamingNeed);
            } catch (IOException e) {
                // error writing the streaming need in the resource writer -> cancel download and report error
                reportErrorWriting();
            }
        }
    }

    /**
     * Remove all slaves and free all resources (subchannels). The download dies
     */
    private synchronized void freeAssignedResources() {
        // free all subchannels and report the ResourceStreamingManager that this download must be removed. We parallelize this call to avoid
        // locks
        ParallelTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceStreamingManager.freeAllSubchannels(MasterResourceStreamer.this);
                resourceStreamingManager.removeDownload(MasterResourceStreamer.this);
            }
        });
        Collection<SlaveController> slavesToRemove = new HashSet<>(activeSlaves.values());
        for (SlaveController slaveController : slavesToRemove) {
            removeSlave(slaveController.getSubchannel(), true);
        }
        alive = false;
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

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        freeAssignedResources();
    }

    @Override
    public float getPriority() {
        return getMasterPriority();
    }
}
