package jacz.peerengineservice.util.datatransfer.slave;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.datatransfer.GlobalUploadStatistics;
import jacz.peerengineservice.util.datatransfer.PeerBasedStatistics;
import jacz.peerengineservice.util.datatransfer.master.ResourcePart;
import jacz.util.date_time.SpeedMonitor;
import jacz.util.numeric.range.LongRange;

import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Statistics of a single upload session
 */
public class UploadSessionStatistics {

    private static final long MILLIS_FOR_SPEED_MEASURE = 5000;

    /**
     * The peer requesting this resource
     */
    private final PeerID requestingPeer;

    /**
     * The date this upload session started
     */
    private final Date creationDate;

    /**
     * Parts assigned for upload
     */
    private final ResourcePart assignedPart;

    /**
     * Parts already uploaded
     */
    private final ResourcePart uploadedPart;

    /**
     * The total amount of resource uploaded
     */
    private long uploadedSize;

    /**
     * Upload speed monitor
     */
    private final SpeedMonitor speed;

    private final GlobalUploadStatistics globalUploadStatistics;

    private final PeerBasedStatistics peerBasedStatistics;

    public UploadSessionStatistics(PeerID requestingPeer, GlobalUploadStatistics globalUploadStatistics, PeerBasedStatistics peerBasedStatistics) {
        this.requestingPeer = requestingPeer;
        creationDate = new GregorianCalendar().getTime();
        assignedPart = new ResourcePart();
        uploadedPart = new ResourcePart();
        speed = new SpeedMonitor(MILLIS_FOR_SPEED_MEASURE);
        this.globalUploadStatistics = globalUploadStatistics;
        if (globalUploadStatistics != null) {
            globalUploadStatistics.startTransferSession();
        }
        this.peerBasedStatistics = peerBasedStatistics;
        if (peerBasedStatistics != null) {
            peerBasedStatistics.startUploadSession(requestingPeer);
        }
    }

    synchronized void addAssignedSegment(LongRange segment) {
        assignedPart.add(segment);
    }

    synchronized void addUploadedSegment(LongRange segment) {
        speed.addProgress(segment.size());
        assignedPart.remove(segment);
        uploadedPart.add(segment);
        uploadedSize += segment.size();
        if (globalUploadStatistics != null) {
            globalUploadStatistics.addTransferSize(segment.size());
        }
        if (peerBasedStatistics != null) {
            peerBasedStatistics.addUploadedSize(requestingPeer, segment.size());
        }
    }

    public PeerID getRequestingPeer() {
        return requestingPeer;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public synchronized ResourcePart getAssignedPart() {
        return assignedPart;
    }

    public synchronized ResourcePart getUploadedPart() {
        return uploadedPart;
    }

    public synchronized long getUploadedSize() {
        return uploadedSize;
    }

    public synchronized Double getSpeed() {
        return speed.getAverageSpeed();
    }

    public synchronized void stop() {
        speed.stop();
        long sessionTime = System.currentTimeMillis() - creationDate.getTime();
        if (globalUploadStatistics != null) {
            globalUploadStatistics.endTransferSession(sessionTime);
        }
        if (peerBasedStatistics != null) {
            peerBasedStatistics.endUploadSession(requestingPeer, sessionTime);
        }
    }
}
