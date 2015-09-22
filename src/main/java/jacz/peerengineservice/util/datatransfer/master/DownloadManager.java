package jacz.peerengineservice.util.datatransfer.master;

import jacz.peerengineservice.util.datatransfer.DownloadProgressNotificationHandler;
import jacz.util.identifier.UniqueIdentifier;

/**
 * This class gives the client the ability to control a single download. It also contains the statistics about the
 * download progress.
 */
public class DownloadManager {

    private final MasterResourceStreamer masterResourceStreamer;

    public DownloadManager(MasterResourceStreamer masterResourceStreamer) {
        this.masterResourceStreamer = masterResourceStreamer;
    }

    /**
     * Pauses the download. If already paused, then this has no effect
     */
    public void pause() {
        masterResourceStreamer.pause();
    }

    /**
     * Resumes a paused download. If already resumed, then this has no effect
     */
    public void resume() {
        masterResourceStreamer.resume();
    }

    /**
     * Stops the download. All resources for this download are freed. If the FileWriter allows it, the download will be saved into disk
     * for later resuming. If already stopped or cancelled, this has no effect
     */
    public void stop() {
        masterResourceStreamer.stop();
    }

    /**
     * Cancels the download. All achieved progress is deleted. If already stopped or cancelled, this has no effect
     */
    public void cancel() {
        masterResourceStreamer.cancel(DownloadProgressNotificationHandler.CancellationReason.USER);
    }

    public double getStreamingNeed() {
        return masterResourceStreamer.getStreamingNeed();
    }

    public void setStreamingNeed(double streamingNeed) {
        masterResourceStreamer.setStreamingNeed(streamingNeed);
    }

    public float getPriority() {
        return masterResourceStreamer.getMasterPriority();
    }

    public void setPriority(int priority) {
        masterResourceStreamer.setPriority(priority);
    }

    public UniqueIdentifier getId() {
        return masterResourceStreamer.getId();
    }

    public DownloadState getState() {
        return masterResourceStreamer.getState();
    }

    public String getResourceID() {
        return masterResourceStreamer.getResourceID();
    }

    public String getStoreName() {
        return masterResourceStreamer.getStoreName();
    }

    public synchronized Long getLength() {
        return masterResourceStreamer.getResourceSize();
    }

    public Statistics getStatistics() {
        return masterResourceStreamer.getStatistics();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DownloadManager that = (DownloadManager) o;

        return masterResourceStreamer.getId().equals(that.masterResourceStreamer.getId());
    }

    @Override
    public int hashCode() {
        return masterResourceStreamer.getId().hashCode();
    }
}
