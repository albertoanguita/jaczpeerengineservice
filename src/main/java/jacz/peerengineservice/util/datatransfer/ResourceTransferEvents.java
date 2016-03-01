package jacz.peerengineservice.util.datatransfer;

import jacz.peerengineservice.PeerId;

/**
 * Events related to resource transfers (download/upload requests, important download/upload events, file store changes,
 * etc) that are notified to clients
 */
public interface ResourceTransferEvents {

    void addLocalResourceStore(String name);

    void setLocalGeneralResourceStore();

    void addForeignResourceStore(String name);

    void removeLocalResourceStore(String name);

    void removeLocalGeneralResourceStore();

    void removeForeignResourceStore(String name);

    void globalDownloadInitiated(String resourceStoreName, String resourceID, double streamingNeed, String totalHash, String totalHashAlgorithm);

    void peerDownloadInitiated(PeerId serverPeerId, String resourceStoreName, String resourceID, double streamingNeed, String totalHash, String totalHashAlgorithm);

    void setMaxDesiredDownloadSpeed(Float totalMaxDesiredSpeed);

    void setMaxDesiredUploadSpeed(Float totalMaxDesiredSpeed);

    void setAccuracy(double accuracy);

    void approveResourceRequest(final ResourceRequest request, ResourceStoreResponse response);

    void denyUnavailableSubchannelResourceRequest(final ResourceRequest request, ResourceStoreResponse response);

    void deniedResourceRequest(final ResourceRequest request, ResourceStoreResponse response);

    void periodicDownloadsNotification(DownloadsManager downloadsManager);

    void periodicUploadsNotification(UploadsManager uploadsManager);
}
