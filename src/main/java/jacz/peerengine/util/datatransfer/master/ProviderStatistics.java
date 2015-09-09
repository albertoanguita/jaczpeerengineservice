package jacz.peerengine.util.datatransfer.master;

import jacz.peerengine.util.datatransfer.resource_accession.ResourceProvider;
import jacz.util.date_time.SpeedMonitor;
import jacz.util.numeric.LongRange;

import java.util.Date;
import java.util.GregorianCalendar;

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
    private final ResourceProvider resourceProvider;

    /**
     * The time this provider was first added to this download
     */
    private final Date creationDate;

    /**
     * The time that the current session (if active) started
     */
    private Date dateStartedThisSession;

    /**
     * Total time that this provider has been active
     */
    private long accumulatedMillisActive;

    /**
     * If this provider is active in the current download. If the provider is removed from the download, this object is maintained but this
     * attribute is set to false. In that state, retrieving the time of session start or the time of the active session returns -1
     */
    private boolean active;

    /**
     * Currently shared part by this provider. Empty if not active
     */
    private ResourcePart sharedPart;

    /**
     * Currently assigned part to this provider. Empty if not active
     */
    private ResourcePart assignedPart;

    /**
     * Downloaded part from this provider so far. Wrong parts are removed
     */
    private ResourcePart downloadedPart;

    /**
     * Download speed monitor
     */
    private SpeedMonitor speed;


    public ProviderStatistics(ResourceProvider resourceProvider) {
        // first time this provider is added to the download
        this.resourceProvider = resourceProvider;
        creationDate = new GregorianCalendar().getTime();
        sharedPart = new ResourcePart();
        assignedPart = new ResourcePart();
        downloadedPart = new ResourcePart();
        resume();
    }

    private void init() {
        dateStartedThisSession = new GregorianCalendar().getTime();
        sharedPart.clear();
        assignedPart.clear();
        speed = new SpeedMonitor(Statistics.MILLIS_FOR_SPEED_MEASURE);
    }

    public ResourceProvider getResourceProvider() {
        return resourceProvider;
    }

    public synchronized Date getCreationDate() {
        return creationDate;
    }

    public synchronized Date getDateStartedThisSession() {
        return (isActive()) ? dateStartedThisSession : null;
    }

    public synchronized long getAccumulatedMillisActive() {
        return accumulatedMillisActive + getCurrentSessionMillis();
    }

    public synchronized long getCurrentSessionMillis() {
        return (isActive()) ? System.currentTimeMillis() - getDateStartedThisSession().getTime() : 0;
    }

    public synchronized boolean isActive() {
        return active;
    }

    synchronized void stop() {
        speed.stop();
    }

    synchronized long stopSession() {
        sharedPart.clear();
        assignedPart.clear();
        long sessionMillis = getCurrentSessionMillis();
        accumulatedMillisActive += sessionMillis;
        active = false;
        speed.stop();
        return sessionMillis;
    }

    synchronized void resume() {
        active = true;
        init();
    }

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
        downloadedPart.add(downloadedSegment);
    }

    synchronized void reportFailedSegment(LongRange failedSegment) {
        downloadedPart.remove(failedSegment);
    }

    public String getResourceProviderID() {
        return resourceProvider.getID();
    }

    public synchronized ResourcePart getSharedPart() {
        return new ResourcePart(sharedPart);
    }

    public synchronized ResourcePart getAssignedPart() {
        return assignedPart;
    }

    public synchronized ResourcePart getDownloadedPart() {
        return downloadedPart;
    }

    public synchronized Double getSpeed() {
        return speed.getAverageSpeed();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProviderStatistics)) return false;

        ProviderStatistics that = (ProviderStatistics) o;

        if (!resourceProvider.equals(that.resourceProvider)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return resourceProvider.hashCode();
    }
}
