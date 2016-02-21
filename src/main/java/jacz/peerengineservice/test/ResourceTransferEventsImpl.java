package jacz.peerengineservice.test;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.datatransfer.*;

/**
 * Created by Alberto on 13/10/2015.
 */
public class ResourceTransferEventsImpl implements ResourceTransferEvents {
    @Override
    public void addLocalResourceStore(String name) {

    }

    @Override
    public void setLocalGeneralResourceStore() {

    }

    @Override
    public void addForeignResourceStore(String name) {

    }

    @Override
    public void removeLocalResourceStore(String name) {

    }

    @Override
    public void removeLocalGeneralResourceStore() {

    }

    @Override
    public void removeForeignResourceStore(String name) {

    }

    @Override
    public void globalDownloadInitiated(String resourceStoreName, String resourceID, double streamingNeed, String totalHash, String totalHashAlgorithm) {

    }

    @Override
    public void peerDownloadInitiated(PeerID serverPeerID, String resourceStoreName, String resourceID, double streamingNeed, String totalHash, String totalHashAlgorithm) {

    }

    @Override
    public void setMaxDesiredDownloadSpeed(Float totalMaxDesiredSpeed) {

    }

    @Override
    public void setMaxDesiredUploadSpeed(Float totalMaxDesiredSpeed) {

    }

    @Override
    public void setAccuracy(double accuracy) {

    }

    @Override
    public void approveResourceRequest(ResourceRequest request, ResourceStoreResponse response) {

    }

    @Override
    public void denyUnavailableSubchannelResourceRequest(ResourceRequest request, ResourceStoreResponse response) {

    }

    @Override
    public void deniedResourceRequest(ResourceRequest request, ResourceStoreResponse response) {

    }

    @Override
    public void periodicDownloadsNotification(DownloadsManager downloadsManager) {

    }

    @Override
    public void periodicUploadsNotification(UploadsManager uploadsManager) {

    }
}
