package jacz.peerengineservice.util.datatransfer.master;

import jacz.peerengineservice.PeerID;
import jacz.util.date_time.SpeedMonitor;
import jacz.util.numeric.range.LongRange;

/**
 * Statistics for a single provider in a specific download process. This element is accessible through the statistics object of the DownloadManager,
 * for the related download. It has no value after the download is complete
 * <p/>
 * It stores data about the different sessions in which this provider was active during the download
 */
public class ProviderStatistics {

    /**
     * The resource provider that generates this statistics
     */
//    private final ResourceProvider resourceProvider;
    private final PeerID resourceProviderID;

    /**
     * Currently shared part by this provider. Empty if not active
     */
    private ResourcePart sharedPart;

    /**
     * Currently assigned part to this provider. Empty if not active
     */
    private final ResourcePart assignedPart;

    /**
     * Download speed monitor
     */
    private transient SpeedMonitor speed;


    public ProviderStatistics(PeerID resourceProviderID) {
        // first time this provider is added to the download
        this.resourceProviderID = resourceProviderID;
        sharedPart = new ResourcePart();
        assignedPart = new ResourcePart();
        speed = new SpeedMonitor(ResourceDownloadStatistics.MILLIS_FOR_SPEED_MEASURE);
//        resume();
    }

//    private void init() {
//        if (sharedPart == null) {
//            sharedPart = new ResourcePart();
//        } else {
//            sharedPart.clear();
//        }
//        if (assignedPart == null) {
//            assignedPart = new ResourcePart();
//        } else {
//            assignedPart.clear();
//        }
//        speed = new SpeedMonitor(ResourceDownloadStatistics.MILLIS_FOR_SPEED_MEASURE);
//    }

//    public ResourceProvider getResourceProvider() {
//        return resourceProvider;
//    }

    synchronized void stop() {
        speed.stop();
    }

//    synchronized long stopSession() {
//        sharedPart.clear();
//        assignedPart.clear();
//        speed.stop();
////        return sessionMillis;
//        return 0L;
//    }

//    synchronized void resume() {
//        init();
//    }

    synchronized void reportSharedPart(ResourcePart sharedPart) {
        this.sharedPart = sharedPart;
    }

    synchronized void reportAssignedSegment(LongRange assignedSegment) {
        assignedPart.add(assignedSegment);
    }

    synchronized void reportClearedAssignation() {
        assignedPart.clear();
    }

    synchronized void reportDownloadedSegment(LongRange downloadedSegment) {
        speed.addProgress(downloadedSegment.size());
        assignedPart.remove(downloadedSegment);
    }

    public PeerID getResourceProviderID() {
        return resourceProviderID;
    }

    public synchronized ResourcePart getSharedPart() {
        return new ResourcePart(sharedPart);
    }

    public synchronized ResourcePart getAssignedPart() {
        return new ResourcePart(assignedPart);
    }

    public synchronized double getSpeed() {
        return speed.getAverageSpeed();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProviderStatistics)) return false;

        ProviderStatistics that = (ProviderStatistics) o;

        return resourceProviderID.equals(that.resourceProviderID);
    }

    @Override
    public int hashCode() {
        return resourceProviderID.hashCode();
    }
}
