package jacz.peerengineservice.util.datatransfer;

import jacz.peerengineservice.client.PeerClientPrivateInterface;
import jacz.peerengineservice.util.datatransfer.slave.UploadManager;
import jacz.util.identifier.UniqueIdentifier;

import java.util.List;

/**
 * This class handles all active uploads, and organizes them by store
 */
public class UploadsManager extends TransfersManager<UploadManager> {

    /**
     * This class can periodically notify the client, using this ProgressNotification
     */
    private PeerClientPrivateInterface peerClientPrivateInterface;

    public UploadsManager(PeerClientPrivateInterface peerClientPrivateInterface) {
        super("UploadsManager");
        this.peerClientPrivateInterface = peerClientPrivateInterface;
    }

    /**
     * Adds a new download
     *
     * @param store                 resource store corresponding to this upload
     * @param uploadManager manager for the slave that is added
     */
    synchronized void addUpload(String store, UploadManager uploadManager) {
        addTransfer(store, uploadManager.getId(), uploadManager);
    }

    /**
     * Removes a download
     *
     * @param store store then this upload is located
     * @param id    id of the download to remove
     */
    synchronized UploadManager removeUpload(String store, UniqueIdentifier id) {
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
    synchronized List<UploadManager> getAllUploads() {
        return getAllTransfers();
    }

    @Override
    protected void notifyClient() {
        peerClientPrivateInterface.periodicUploadsNotification(this);
    }
}
