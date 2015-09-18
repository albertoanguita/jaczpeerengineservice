package jacz.peerengineservice.util.datatransfer.master;

import jacz.peerengineservice.util.datatransfer.GlobalDownloadStatistics;
import jacz.peerengineservice.util.datatransfer.PeerStatistics;
import jacz.peerengineservice.util.datatransfer.resource_accession.PeerResourceProvider;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceProvider;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceWriter;
import jacz.util.date_time.SpeedMonitor;
import jacz.util.numeric.LongRange;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * Statistics for a resource download. These statistics cover the different sessions for a download process
 */
public class Statistics {

    private static final String RESOURCE_WRITER_STATISTICS_GROUP = "@RESOURCE_WRITER_DOWNLOAD_STATISTICS_GROUP";
    private static final String RESOURCE_WRITER_CREATION_DATE_FIELD = "@CREATION_DATE";
    private static final String RESOURCE_WRITER_DOWNLOADED_PART_FIELD = "@DOWNLOADED_PART";
    private static final String RESOURCE_WRITER_CHECKED_PART_FIELD = "@CHECKED_PART";
    private static final String RESOURCE_WRITER_ACCUMULATED_MILLIS_ACTIVE_FIELD = "@ACCUMULATED_MILLIS_ACTIVE";
    private static final String RESOURCE_WRITER_DOWNLOADED_SIZE_THIS_RESOURCE_FIELD = "@DOWNLOADED_SIZE_THIS_RESOURCE";
    private static final String RESOURCE_WRITER_INCORRECT_SIZE_THIS_RESOURCE_FIELD = "@INCORRECT_SIZE_THIS_RESOURCE";
    private static final String RESOURCE_WRITER_PROVIDERS_STATISTICS_FIELD = "@PROVIDERS_STATISTICS";

    static final long MILLIS_FOR_SPEED_MEASURE = 5000;


    /**
     * Resource writer where we store the statistics (so they are maintained between sessions)
     */
    private final ResourceWriter resourceWriter;

    /**
     * The date this download was started (first session)
     */
    private final Date creationDate;

    /**
     * Parts assigned for download
     */
    private final ResourcePart assignedPart;

    /**
     * Parts already downloaded
     */
    private final ResourcePart downloadedPart;

    /**
     * Parts confirmed by intermediate hashes
     */
    private final ResourcePart checkedPart;

    /**
     * The date at which the current session started
     */
    private final Date dateStartedThisSession;

    /**
     * The amount of resource downloaded during the current session
     */
    private long downloadedSizeThisSession;

    /**
     * The amount of resource downloaded but discarded due to incorrect hash
     */
    private long incorrectSizeThisSession;

    /**
     * The time this download (all sessions) has been active
     */
    private long accumulatedMillisActive;

    /**
     * The total amount of resource downloaded
     */
    private long downloadedSizeThisResource;

    /**
     * The total amount of resource downloaded but discarded due to incorrect hash
     */
    private long incorrectSizeThisResource;

    /**
     * Download speed monitor
     */
    private final SpeedMonitor speed;

    private final HashMap<String, ProviderStatistics> providers;

    private final GlobalDownloadStatistics globalDownloadStatistics;

    private final PeerStatistics peerStatistics;

    Statistics(ResourceWriter resourceWriter, GlobalDownloadStatistics globalDownloadStatistics, PeerStatistics peerStatistics) throws IOException {
        this.resourceWriter = resourceWriter;
        Map<String, Serializable> storedStatistics = resourceWriter.getUserGenericData(RESOURCE_WRITER_STATISTICS_GROUP);
        if (storedStatistics != null && storedStatistics.containsKey(RESOURCE_WRITER_CREATION_DATE_FIELD)) {
            creationDate = (Date) storedStatistics.get(RESOURCE_WRITER_CREATION_DATE_FIELD);
        } else {
            creationDate = new GregorianCalendar().getTime();
        }
        assignedPart = new ResourcePart();
        if (storedStatistics != null && storedStatistics.containsKey(RESOURCE_WRITER_DOWNLOADED_PART_FIELD)) {
            downloadedPart = (ResourcePart) storedStatistics.get(RESOURCE_WRITER_DOWNLOADED_PART_FIELD);
        } else {
            downloadedPart = new ResourcePart();
        }
        if (storedStatistics != null && storedStatistics.containsKey(RESOURCE_WRITER_CHECKED_PART_FIELD)) {
            checkedPart = (ResourcePart) storedStatistics.get(RESOURCE_WRITER_CHECKED_PART_FIELD);
        } else {
            checkedPart = new ResourcePart();
        }
        dateStartedThisSession = new GregorianCalendar().getTime();
        downloadedSizeThisSession = 0L;
        incorrectSizeThisSession = 0L;
        if (storedStatistics != null && storedStatistics.containsKey(RESOURCE_WRITER_ACCUMULATED_MILLIS_ACTIVE_FIELD)) {
            accumulatedMillisActive = (Long) storedStatistics.get(RESOURCE_WRITER_ACCUMULATED_MILLIS_ACTIVE_FIELD);
        } else {
            accumulatedMillisActive = 0L;
        }
        if (storedStatistics != null && storedStatistics.containsKey(RESOURCE_WRITER_DOWNLOADED_SIZE_THIS_RESOURCE_FIELD)) {
            downloadedSizeThisResource = (Long) storedStatistics.get(RESOURCE_WRITER_DOWNLOADED_SIZE_THIS_RESOURCE_FIELD);
        } else {
            downloadedSizeThisResource = 0L;
        }
        if (storedStatistics != null && storedStatistics.containsKey(RESOURCE_WRITER_INCORRECT_SIZE_THIS_RESOURCE_FIELD)) {
            incorrectSizeThisResource = (Long) storedStatistics.get(RESOURCE_WRITER_INCORRECT_SIZE_THIS_RESOURCE_FIELD);
        } else {
            incorrectSizeThisResource = 0L;
        }
        if (storedStatistics != null && storedStatistics.containsKey(RESOURCE_WRITER_PROVIDERS_STATISTICS_FIELD)) {
            providers = (HashMap<String, ProviderStatistics>) storedStatistics.get(RESOURCE_WRITER_PROVIDERS_STATISTICS_FIELD);
        } else {
            providers = new HashMap<>();
        }
        speed = new SpeedMonitor(MILLIS_FOR_SPEED_MEASURE);
        this.globalDownloadStatistics = globalDownloadStatistics;
        if (globalDownloadStatistics != null) {
            globalDownloadStatistics.startTransferSession();
        }
        this.peerStatistics = peerStatistics;
    }

    synchronized void stopSession() throws IOException {
        for (ProviderStatistics providerStatistics : providers.values()) {
            providerStatistics.stopSession();
        }
        accumulatedMillisActive += System.currentTimeMillis() - dateStartedThisSession.getTime();
        resourceWriter.setUserGenericDataField(RESOURCE_WRITER_STATISTICS_GROUP, RESOURCE_WRITER_CREATION_DATE_FIELD, creationDate);
        resourceWriter.setUserGenericDataField(RESOURCE_WRITER_STATISTICS_GROUP, RESOURCE_WRITER_DOWNLOADED_PART_FIELD, downloadedPart);
        resourceWriter.setUserGenericDataField(RESOURCE_WRITER_STATISTICS_GROUP, RESOURCE_WRITER_ACCUMULATED_MILLIS_ACTIVE_FIELD, accumulatedMillisActive);
        resourceWriter.setUserGenericDataField(RESOURCE_WRITER_STATISTICS_GROUP, RESOURCE_WRITER_DOWNLOADED_SIZE_THIS_RESOURCE_FIELD, downloadedSizeThisResource);
        resourceWriter.setUserGenericDataField(RESOURCE_WRITER_STATISTICS_GROUP, RESOURCE_WRITER_INCORRECT_SIZE_THIS_RESOURCE_FIELD, incorrectSizeThisResource);
        resourceWriter.setUserGenericDataField(RESOURCE_WRITER_STATISTICS_GROUP, RESOURCE_WRITER_PROVIDERS_STATISTICS_FIELD, providers);
    }

    synchronized void stop() {
        for (ProviderStatistics providerStatistics : providers.values()) {
            providerStatistics.stop();
        }
        speed.stop();
        if (globalDownloadStatistics != null) {
            globalDownloadStatistics.endTransferSession(accumulatedMillisActive);
        }
    }

    synchronized void downloadComplete() {
        if (globalDownloadStatistics != null) {
            globalDownloadStatistics.downloadComplete();
        }
    }

    synchronized ProviderStatistics reportSharedPart(ResourceProvider resourceProvider, ResourcePart resourcePart) {
        ProviderStatistics providerStatistics = providers.get(resourceProvider.getID());
        if (providerStatistics != null) {
            providerStatistics.reportSharedPart(resourcePart);
        }
        return providerStatistics;
    }

    synchronized ProviderStatistics reportAssignedPart(ResourceProvider resourceProvider, LongRange segment) {
        assignedPart.add(segment);
        ProviderStatistics providerStatistics = providers.get(resourceProvider.getID());
        if (providerStatistics != null) {
            providerStatistics.reportAssignedSegment(segment);
        }
        return providerStatistics;
    }

    synchronized ProviderStatistics reportClearedAssignation(ResourceProvider resourceProvider) {
        ProviderStatistics providerStatistics = providers.get(resourceProvider.getID());
        if (providerStatistics != null) {
            providerStatistics.reportClearedAssignation();
        }
        return providerStatistics;
    }

    synchronized ProviderStatistics reportDownloadedPart(ResourceProvider resourceProvider, LongRange segment) {
        speed.addProgress(segment.size());
        if (globalDownloadStatistics != null) {
            globalDownloadStatistics.addTransferSize(segment.size());
        }
        assignedPart.remove(segment);
        downloadedPart.add(segment);
        downloadedSizeThisSession += segment.size();
        downloadedSizeThisResource += segment.size();
        ProviderStatistics providerStatistics = providers.get(resourceProvider.getID());
        if (providerStatistics != null) {
            providerStatistics.reportDownloadedSegment(segment);
            if (resourceProvider.getType() == ResourceProvider.Type.PEER && peerStatistics != null) {
                peerStatistics.addDownloadedSize(((PeerResourceProvider) resourceProvider).getPeerID(), segment.size());
            }
        }
        return providerStatistics;
    }

    synchronized void reportCorrectIntermediateHash(LongRange segment) {
        checkedPart.add(segment);
    }

    synchronized void reportFailedIntermediateHash(LongRange segment) {
        downloadedPart.remove(segment);
        downloadedSizeThisSession -= segment.size();
        downloadedSizeThisResource -= segment.size();
        incorrectSizeThisSession += segment.size();
        incorrectSizeThisResource += segment.size();
        for (ProviderStatistics providerStatistics : providers.values()) {
            providerStatistics.reportFailedSegment(segment);
        }
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public synchronized ResourcePart getAssignedPart() {
        return new ResourcePart(assignedPart);
    }

    public synchronized ResourcePart getDownloadedPart() {
        return new ResourcePart(downloadedPart);
    }

    public synchronized long millisThisSession() {
        return System.currentTimeMillis() - dateStartedThisSession.getTime();
    }

    public synchronized long getDownloadedSizeThisSession() {
        return downloadedSizeThisSession;
    }

    public synchronized long getIncorrectSizeThisSession() {
        return incorrectSizeThisSession;
    }

    public synchronized long totalMillis() {
        return accumulatedMillisActive + millisThisSession();
    }

    public synchronized long getDownloadedSizeThisResource() {
        return downloadedSizeThisResource;
    }

    public synchronized long getIncorrectSizeThisResource() {
        return incorrectSizeThisResource;
    }

    public synchronized Double getSpeed() {
        return speed.getAverageSpeed();
    }

    public synchronized Map<String, ProviderStatistics> getProviders() {
        return new HashMap<String, ProviderStatistics>(providers);
    }

    synchronized ProviderStatistics addProvider(ResourceProvider resourceProvider) {
        ProviderStatistics providerStatistics = providers.get(resourceProvider.getID());
        if (providerStatistics != null) {
            providerStatistics.resume();
        } else {
            // provider not found, initialize and add in first position
            providerStatistics = new ProviderStatistics(resourceProvider);
            providers.put(resourceProvider.getID(), providerStatistics);
        }
        if (resourceProvider.getType() == ResourceProvider.Type.PEER && peerStatistics != null) {
            peerStatistics.startDownloadSession(((PeerResourceProvider) resourceProvider).getPeerID());
        }
        return providerStatistics;
    }

    synchronized ProviderStatistics removeProvider(ResourceProvider resourceProvider) {
        ProviderStatistics providerStatistics = providers.get(resourceProvider.getID());
        if (providerStatistics != null) {
            long sessionMillis = providerStatistics.stopSession();
            if (resourceProvider.getType() == ResourceProvider.Type.PEER && peerStatistics != null) {
                peerStatistics.endDownloadSession(((PeerResourceProvider) resourceProvider).getPeerID(), sessionMillis);
            }
        }
        return providerStatistics;
    }
}
