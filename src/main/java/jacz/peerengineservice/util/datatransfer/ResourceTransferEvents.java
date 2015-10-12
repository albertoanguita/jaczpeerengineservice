package jacz.peerengineservice.util.datatransfer;

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

    void globalDownloadInitiated();

    void globalDownloadDenied();

    void peerDownloadInitiated();

    void peerDownloadDenied();

    void setMaxDesiredDownloadSpeed(Float totalMaxDesiredSpeed);

    void setMaxDesiredUploadSpeed(Float totalMaxDesiredSpeed);

    void setAccuracy(double accuracy);

    void approveResourceRequest(final ResourceRequest request, ResourceStoreResponse response);

    void denyUnavailableSubchannelResourceRequest(final ResourceRequest request, ResourceStoreResponse response);

    void deniedResourceRequest(final ResourceRequest request, ResourceStoreResponse response);

    void stop();
}
