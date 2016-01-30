package jacz.peerengineservice.util.datatransfer;

import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.util.identifier.UniqueIdentifier;

import java.util.List;


/**
 * This class handles the current downloads. Downloads are classified in visible and invisible. The former are periodically notified to the client.
 * <p/>
 * We can request it to notify us every fixed time, and we can iterate through the active downloads.
 */
public class DownloadsManager extends TransfersManager<DownloadManager> {

    /**
     * This class can periodically notify the client, using this ProgressNotification
     */
    private final ResourceTransferEvents resourceTransferEvents;

    /**
     * Class constructor
     *
     * @param resourceTransferEvents peer client action implementation for periodically notifying downloads
     */
    public DownloadsManager(ResourceTransferEvents resourceTransferEvents) {
        super("DownloadsManager");
        this.resourceTransferEvents = resourceTransferEvents;
    }

    /**
     * Adds a new download
     *
     * @param downloadManager manager for the new download. We use its id for indexing the download
     */
    synchronized void addDownload(String store, DownloadManager downloadManager) {
        addTransfer(store, downloadManager.getId(), downloadManager);
    }

    /**
     * Removes a download
     *
     * @param id id of the download to remove
     */
    synchronized void removeDownload(String store, UniqueIdentifier id) {
        removeTransfer(store, id);
    }

    /**
     * Retrieves a shallow copy of the active downloads (only visible ones)
     *
     * @return a shallow copy of the active downloads
     */
    public synchronized List<DownloadManager> getDownloads(String store) {
        return getTransfers(store);
    }

    /**
     * Retrieves a shallow copy of the active downloads (visible plus invisible)
     *
     * @return a shallow copy of the active downloads
     */
    public synchronized List<DownloadManager> getAllDownloads() {
        return getAllTransfers();
    }

    @Override
    protected void notifyClient() {
        resourceTransferEvents.periodicDownloadsNotification(this);
    }
}
