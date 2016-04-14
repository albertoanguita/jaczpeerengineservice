package jacz.peerengineservice.client;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.datatransfer.DownloadProgressNotificationHandler;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.peerengineservice.util.datatransfer.master.ProviderStatistics;
import jacz.peerengineservice.util.datatransfer.master.ResourcePart;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceWriter;
import jacz.util.numeric.range.LongRange;

/**
 * Simple download progress notification handler
 */
public class DownloadProgressNotificationHandlerImpl implements DownloadProgressNotificationHandler {

    public DownloadProgressNotificationHandlerImpl() {
    }

    @Override
    public void started(String resourceID, String storeName, DownloadManager downloadManager) {
        System.out.println("started download of resource " + resourceID);
    }

    @Override
    public void resourceSize(String resourceID, String storeName, DownloadManager downloadManager, long resourceSize) {
        System.out.println("reported the resource size of resource " + resourceID + ": " + resourceSize);
    }

    @Override
    public void providerAdded(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, PeerId providerId) {
        System.out.println("provider added to download of resource " + resourceID + ". Provider: " + providerId);
    }

    @Override
    public void providerRemoved(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, PeerId providerId) {
        System.out.println("provider removed from download of resource " + resourceID + ". Provider: " + providerId);
    }

    @Override
    public void providerReportedSharedPart(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, ResourcePart sharedPart) {
//        System.out.println("provider reported its shared part: " + providerStatistics.getResourceProviderID() + " / " + sharedPart);
    }

    @Override
    public void providerWasAssignedSegment(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, LongRange assignedSegment) {
//        System.out.println("provider was assigned a new segment: " + providerStatistics.getResourceProviderID() + " / " + assignedSegment);
    }

    @Override
    public void providerWasClearedAssignation(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager) {
        System.out.println("providers assignation was cleared: " + providerStatistics.getResourceProviderID());
    }

    @Override
    public void paused(String resourceID, String storeName, DownloadManager downloadManager) {
        System.out.println("download paused for resource " + resourceID);
    }

    @Override
    public void resumed(String resourceID, String storeName, DownloadManager downloadManager) {
        System.out.println("download resumed for resource " + resourceID);
    }

    @Override
    public void downloadedSegment(String resourceID, String storeName, LongRange segment, DownloadManager downloadManager) {
//        System.out.println("downloaded segment for resource " + resourceID + ". " + segment);
    }

    @Override
    public void checkingTotalHash(String resourceID, String storeName, int percentage, DownloadManager downloadManager) {
        System.out.println("checking total hash for resource " + resourceID + ". " + percentage + "%");
    }

    @Override
    public void successTotalHash(String resourceID, String storeName, DownloadManager downloadManager) {
        System.out.println("OK total hash for resource " + resourceID);
        System.out.println("Resource is available at: " + downloadManager.getResourceWriter().getPath());
        System.out.println("Custom info for download: " + downloadManager.getResourceWriter().getUserDictionary());
    }

    @Override
    public void failedTotalHash(String resourceID, String storeName, DownloadManager downloadManager) {
        System.out.println("FAIL total hash for resource " + resourceID);
    }

    @Override
    public void invalidTotalHashAlgorithm(String resourceID, String storeName, String hashAlgorithm, DownloadManager downloadManager) {
        System.out.println("invalid total hash algorithm for resource " + resourceID + ". " + hashAlgorithm);
    }

    @Override
    public void completed(String resourceID, String storeName, ResourceWriter resourceWriter, DownloadManager downloadManager) {
        System.out.println("download completed for resource " + resourceID);
    }

    @Override
    public void cancelled(String resourceID, String storeName, CancellationReason reason, DownloadManager downloadManager) {
        System.out.println("download cancelled for resource " + resourceID + ". Reason: " + reason);
    }

    @Override
    public void stopped(String resourceID, String storeName, DownloadManager downloadManager) {
        System.out.println("download stopped for resource " + resourceID);

    }
}
