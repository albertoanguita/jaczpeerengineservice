package jacz.peerengineservice.util.datatransfer.master;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceProvider;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceWriter;
import jacz.util.date_time.SpeedMonitor;
import jacz.util.numeric.range.LongRange;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Statistics for a resource download. These statistics cover the different sessions for a download process
 */
public class ResourceDownloadStatistics {

    private static final String RESOURCE_WRITER_CREATION_DATE_FIELD = "RESOURCE_DOWNLOAD_STATISTICS@CREATION_DATE";
    private static final String RESOURCE_WRITER_DOWNLOADED_PART_FIELD = "RESOURCE_DOWNLOAD_STATISTICS@DOWNLOADED_PART";

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
     * The total amount of resource downloaded
     */
    private long downloadedSizeThisResource;

    /**
     * Download speed monitor
     */
    private final SpeedMonitor speed;

    private final HashMap<PeerId, ProviderStatistics> providers;

    ResourceDownloadStatistics(ResourceWriter resourceWriter) throws IOException {
        this.resourceWriter = resourceWriter;
        Map<String, Serializable> storedStatistics = resourceWriter.getSystemDictionary();
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
        downloadedSizeThisResource = downloadedPart.size();
        providers = new HashMap<>();
        speed = new SpeedMonitor(MILLIS_FOR_SPEED_MEASURE);
    }

    synchronized void stopSession() throws IOException {
        for (ProviderStatistics providerStatistics : providers.values()) {
            providerStatistics.stop();
        }
        resourceWriter.setSystemField(RESOURCE_WRITER_CREATION_DATE_FIELD, creationDate);
        resourceWriter.setSystemField(RESOURCE_WRITER_DOWNLOADED_PART_FIELD, downloadedPart);
    }

    synchronized void stop() {
        for (ProviderStatistics providerStatistics : providers.values()) {
            providerStatistics.stop();
        }
        speed.stop();
    }

    synchronized void downloadComplete(long resourceSize) {
        assignedPart.clear();
        downloadedPart.clear();
        downloadedPart.add(new LongRange(0L, resourceSize - 1L));
        downloadedSizeThisResource = resourceSize;
    }

    synchronized ProviderStatistics reportSharedPart(ResourceProvider resourceProvider, ResourcePart resourcePart) {
        ProviderStatistics providerStatistics = providers.get(resourceProvider.getPeerID());
        if (providerStatistics != null) {
            providerStatistics.reportSharedPart(resourcePart);
        }
        return providerStatistics;
    }

    synchronized ProviderStatistics reportAssignedPart(ResourceProvider resourceProvider, LongRange segment) {
        assignedPart.add(segment);
        ProviderStatistics providerStatistics = providers.get(resourceProvider.getPeerID());
        if (providerStatistics != null) {
            providerStatistics.reportAssignedSegment(segment);
        }
        return providerStatistics;
    }

    synchronized ProviderStatistics reportClearedAssignation(ResourceProvider resourceProvider) {
        ProviderStatistics providerStatistics = providers.get(resourceProvider.getPeerID());
        if (providerStatistics != null) {
            providerStatistics.reportClearedAssignation();
        }
        return providerStatistics;
    }

    synchronized ProviderStatistics reportDownloadedPart(ResourceProvider resourceProvider, LongRange segment) {
        speed.addProgress(segment.size());
        assignedPart.remove(segment);
        downloadedPart.add(segment);
        downloadedSizeThisResource += segment.size();
        ProviderStatistics providerStatistics = providers.get(resourceProvider.getPeerID());
        if (providerStatistics != null) {
            providerStatistics.reportDownloadedSegment(segment);
        }
        return providerStatistics;
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

    public synchronized long getDownloadedSizeThisResource() {
        return downloadedSizeThisResource;
    }

    public synchronized double getSpeed() {
        return speed.getAverageSpeed();
    }

    public synchronized Map<PeerId, ProviderStatistics> getProviders() {
        return new HashMap<>(providers);
    }

    synchronized ProviderStatistics addProvider(ResourceProvider resourceProvider) {
        providers.put(resourceProvider.getPeerID(), new ProviderStatistics(resourceProvider.getPeerID()));
//        ProviderStatistics providerStatistics = providers.get(resourceProvider.getID());
//        if (providerStatistics != null) {
//            providerStatistics.resume();
//        } else {
//            // provider not found, initialize and add in first position
//            providerStatistics = new ProviderStatistics(resourceProvider.getID());
//            providers.put(resourceProvider.getID(), providerStatistics);
//        }
//        if (resourceProvider.getType() == ResourceProvider.Type.PEER && peerBasedStatistics != null) {
//            peerBasedStatistics.startDownloadSession(((PeerResourceProvider) resourceProvider).getPeerId());
//        }
        return providers.get(resourceProvider.getPeerID());
    }

    synchronized ProviderStatistics removeProvider(ResourceProvider resourceProvider) {
//        ProviderStatistics providerStatistics = providers.get(resourceProvider.getID());
//        if (providerStatistics != null) {
//            long sessionMillis = providerStatistics.stopSession();
//        }
//        return providerStatistics;
        ProviderStatistics providerStatistics = providers.remove(resourceProvider.getPeerID());
        if (providerStatistics != null) {
            providerStatistics.stop();
        }
        return providerStatistics;
    }
}
