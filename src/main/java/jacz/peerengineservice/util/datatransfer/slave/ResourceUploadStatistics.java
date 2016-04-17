package jacz.peerengineservice.util.datatransfer.slave;

import jacz.peerengineservice.util.datatransfer.master.ResourcePart;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceProvider;
import jacz.util.date_time.SpeedMonitor;
import jacz.util.numeric.range.LongRange;

import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Statistics about one single resource upload (one session only)
 */
public class ResourceUploadStatistics {

    private static final long MILLIS_FOR_SPEED_MEASURE = 5000;

    /**
     * The date this upload session was started (first session)
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
    private long uploadedSizeThisResource;

    /**
     * Download speed monitor
     */
    private final SpeedMonitor speed;

    public ResourceUploadStatistics() {
        creationDate = new GregorianCalendar().getTime();
        assignedPart = new ResourcePart();
        uploadedPart = new ResourcePart();
        speed = new SpeedMonitor(MILLIS_FOR_SPEED_MEASURE);
    }

    synchronized void stop() {
        speed.stop();
    }
    synchronized void reportAssignedPart(LongRange segment) {
        assignedPart.add(segment);
    }

    synchronized void reportClearedAssignation() {
        assignedPart.clear();
    }

    synchronized void reportUploadedPart(LongRange segment) {
        speed.addProgress(segment.size());
        assignedPart.remove(segment);
        uploadedPart.add(segment);
        uploadedSizeThisResource += segment.size();
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public synchronized ResourcePart getAssignedPart() {
        return new ResourcePart(assignedPart);
    }

    public synchronized ResourcePart getUploadedPart() {
        return new ResourcePart(uploadedPart);
    }

    public synchronized long getUploadedSizeThisResource() {
        return uploadedSizeThisResource;
    }

    public synchronized double getSpeed() {
        return speed.getAverageSpeed();
    }
}
