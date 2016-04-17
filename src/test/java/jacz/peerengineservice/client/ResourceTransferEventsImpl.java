package jacz.peerengineservice.client;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.datatransfer.*;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.peerengineservice.util.datatransfer.slave.UploadManager;

/**
 * Created by Alberto on 12/04/2016.
 */
public class ResourceTransferEventsImpl implements ResourceTransferEvents {

    @Override
    public void addLocalResourceStore(String name) {
        System.out.println("Add local resource store: " + name);
    }

    @Override
    public void setLocalGeneralResourceStore() {
        System.out.println("Ser local general resource store");
    }

    @Override
    public void addForeignResourceStore(String name) {
        System.out.println("Add foreign resource store: " + name);
    }

    @Override
    public void removeLocalResourceStore(String name) {
        System.out.println("Remove local resource store: " + name);
    }

    @Override
    public void removeLocalGeneralResourceStore() {
        System.out.println("Remove local general resource store");
    }

    @Override
    public void removeForeignResourceStore(String name) {
        System.out.println("Remove foreign resource store: " + name);
    }

    @Override
    public void globalDownloadInitiated(String resourceStoreName, String resourceID, double streamingNeed, String totalHash, String totalHashAlgorithm) {
        System.out.println("Global download initiated: StoreName" + resourceStoreName + ", resourceID" + resourceID + ", streamingNeed: " + streamingNeed + ", totalHash: " + totalHash + ", totalHashAlgorithm: " + totalHashAlgorithm);
    }

    @Override
    public void peerDownloadInitiated(PeerId serverPeerId, String resourceStoreName, String resourceID, double streamingNeed, String totalHash, String totalHashAlgorithm) {
        System.out.println("Peer download initiated: serverPeerId: " + serverPeerId + ", StoreName" + resourceStoreName + ", resourceID" + resourceID + ", streamingNeed: " + streamingNeed + ", totalHash: " + totalHash + ", totalHashAlgorithm: " + totalHashAlgorithm);
    }

    @Override
    public void setMaxDesiredDownloadSpeed(Float totalMaxDesiredSpeed) {
        System.out.println("Set max desired download speed: " + totalMaxDesiredSpeed);
    }

    @Override
    public void setMaxDesiredUploadSpeed(Float totalMaxDesiredSpeed) {
        System.out.println("Set max desired upload speed: " + totalMaxDesiredSpeed);
    }

    @Override
    public void setAccuracy(double accuracy) {
        System.out.println("Set accuracy: " + accuracy);
    }

    @Override
    public void approveResourceRequest(ResourceRequest request, ResourceStoreResponse response) {
        System.out.println("Approve resource request: " + request + ", " + response);
    }

    @Override
    public void denyUnavailableSubchannelResourceRequest(ResourceRequest request, ResourceStoreResponse response) {
        System.out.println("Deny unavailable subchannel resource request: " + request + ", " + response);
    }

    @Override
    public void deniedResourceRequest(ResourceRequest request, ResourceStoreResponse response) {
        System.out.println("Denied resource request: " + request + ", " + response);
    }

    @Override
    public void periodicDownloadsNotification(DownloadsManager downloadsManager) {
        for (DownloadManager downloadManager : downloadsManager.getAllDownloads()) {
            Double speed = downloadManager.getStatistics().getSpeed();
            speed /= 1024d;
            long size = downloadManager.getStatistics().getDownloadedSizeThisResource();
            Long length = downloadManager.getLength();
            Double part = null;
            if (length != null) {
                part = (double) size / (double) length * 100d;
            }
            System.out.println("Speed for " + downloadManager.getResourceID() + ": " + speed + "KB, downloaded part: " + part);
        }
    }

    @Override
    public void periodicUploadsNotification(UploadsManager uploadsManager) {
        for (UploadManager uploadManager : uploadsManager.getAllUploads()) {
            Double speed = uploadManager.getStatistics().getSpeed();
            speed /= 1024d;
            long size = uploadManager.getStatistics().getUploadedSizeThisResource();
            System.out.println("Speed for " + uploadManager.getResourceID() + ": " + speed + "KB, downloaded size: " + size);
        }
    }
}
