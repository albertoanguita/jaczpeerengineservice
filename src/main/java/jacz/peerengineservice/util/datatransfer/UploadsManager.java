package jacz.peerengineservice.util.datatransfer;

import jacz.peerengineservice.util.datatransfer.slave.UploadManager;

import java.util.List;

/**
 * This class handles all active uploads, and organizes them by store
 */
public class UploadsManager extends TransfersManager<UploadManager> {

    /**
     * This class can periodically notify the client, using this ProgressNotification
     */
    private final ResourceTransferEvents resourceTransferEvents;

    public UploadsManager(ResourceTransferEvents resourceTransferEvents) {
        super("UploadsManager");
        this.resourceTransferEvents = resourceTransferEvents;
    }

    /**
     * Adds a new download
     *
     * @param store                 resource store corresponding to this upload
     * @param uploadManager manager for the slave that is added
     */
    synchronized void addUpload(String store, String id, UploadManager uploadManager) {
        addTransfer(store, uploadManager.getId(), uploadManager);
    }

    /**
     * Removes a download
     *
     * @param store store then this upload is located
     * @param id    id of the download to remove
     */
    synchronized UploadManager removeUpload(String store, String id) {
        return removeTransfer(store, id);
    }

    /**
     * Retrieves a shallow copy of the active downloads for a specific store
     *
     * @return a shallow copy of the active uploads of a store
     */
    public synchronized List<UploadManager> getUploads(String store) {
        return getTransfers(store);
    }

    /**
     * Retrieves a shallow copy of the active downloads (visible plus invisible)
     *
     * @return a shallow copy of the active downloads
     */
    public synchronized List<UploadManager> getAllUploads() {
        return getAllTransfers();
    }

    @Override
    protected void notifyClient() {
        resourceTransferEvents.periodicUploadsNotification(this);
    }
}
