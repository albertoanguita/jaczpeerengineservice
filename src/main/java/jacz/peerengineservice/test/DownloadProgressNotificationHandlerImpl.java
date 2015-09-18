package jacz.peerengineservice.test;

import jacz.peerengineservice.util.datatransfer.master.ProviderStatistics;
import jacz.util.numeric.LongRange;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.peerengineservice.util.datatransfer.DownloadProgressNotificationHandler;
import jacz.peerengineservice.util.datatransfer.master.ResourcePart;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceWriter;

/**
 * Simple download progress notification handler
 */
public class DownloadProgressNotificationHandlerImpl implements DownloadProgressNotificationHandler {

    private String initMessage;

    public DownloadProgressNotificationHandlerImpl(PeerID peerID) {
        initMessage = peerID + " downloading resource: ";
    }

    @Override
    public void started(String resourceID, String storeName, DownloadManager downloadManager) {
        System.out.println(initMessage + "started download of resource " + resourceID);
    }

    @Override
    public void resourceSize(String resourceID, String storeName, DownloadManager downloadManager, long resourceSize) {
        System.out.println(initMessage + "reported the resource size of resource " + resourceID + ": " + resourceSize);
    }

    @Override
    public void providerAdded(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, String providerId) {
        System.out.println(initMessage + "provider added to download of resource " + resourceID + ". Provider: " + providerId);
    }

    @Override
    public void providerRemoved(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, String providerId) {
        System.out.println(initMessage + "provider removed from download of resource " + resourceID + ". Provider: " + providerId);
    }

    @Override
    public void providerReportedSharedPart(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, ResourcePart sharedPart) {
//        System.out.println(initMessage + "provider reported its shared part: " + providerStatistics.getResourceProviderID() + " / " + sharedPart);
    }

    @Override
    public void providerWasAssignedSegment(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, LongRange assignedSegment) {
//        System.out.println(initMessage + "provider was assigned a new segment: " + providerStatistics.getResourceProviderID() + " / " + assignedSegment);
    }

    @Override
    public void providerWasClearedAssignation(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager) {
        System.out.println(initMessage + "providers assignation was cleared: " + providerStatistics.getResourceProviderID());
    }

    @Override
    public void paused(String resourceID, String storeName, DownloadManager downloadManager) {
        System.out.println(initMessage + "download paused for resource " + resourceID);
    }

    @Override
    public void resumed(String resourceID, String storeName, DownloadManager downloadManager) {
        System.out.println(initMessage + "download resumed for resource " + resourceID);
    }

    @Override
    public void downloadedSegment(String resourceID, String storeName, LongRange segment, DownloadManager downloadManager) {
//        System.out.println(initMessage + "downloaded segment for resource " + resourceID + ". " + segment);
    }

    @Override
    public void successIntermediateHash(String resourceID, String storeName, LongRange range, DownloadManager downloadManager) {
//        System.out.println(initMessage + "OK intermediate hash for resource " + resourceID + ". " + range);
    }

    @Override
    public void failedIntermediateHash(String resourceID, String storeName, LongRange range, DownloadManager downloadManager) {
        System.out.println(initMessage + "FAIL intermediate hash for resource " + resourceID + ". " + range);
    }

    @Override
    public void invalidIntermediateHashAlgorithm(String resourceID, String storeName, LongRange range, String hashAlgorithm, DownloadManager downloadManager) {
        System.out.println(initMessage + "invalid intermediate hash algorithm for resource " + resourceID + ". " + range + ". " + hashAlgorithm);
    }

    @Override
    public void checkingTotalHash(String resourceID, String storeName, int percentage, DownloadManager downloadManager) {
        System.out.println(initMessage + "checking total hash for resource " + resourceID + ". " + percentage + "%");
    }

    @Override
    public void successTotalHash(String resourceID, String storeName, DownloadManager downloadManager) {
        System.out.println(initMessage + "OK total hash for resource " + resourceID);
    }

    @Override
    public void failedTotalHash(String resourceID, String storeName, DownloadManager downloadManager) {
        System.out.println(initMessage + "FAIL total hash for resource " + resourceID);
    }

    @Override
    public void invalidTotalHashAlgorithm(String resourceID, String storeName, String hashAlgorithm, DownloadManager downloadManager) {
        System.out.println(initMessage + "invalid total hash algorithm for resource " + resourceID + ". " + hashAlgorithm);
    }

    @Override
    public void completed(String resourceID, String storeName, ResourceWriter resourceWriter, DownloadManager downloadManager) {
        System.out.println(initMessage + "download completed for resource " + resourceID);
    }

    @Override
    public void cancelled(String resourceID, String storeName, CancellationReason reason, DownloadManager downloadManager) {
        System.out.println(initMessage + "download cancelled for resource " + resourceID + ". Reason: " + reason);
    }

    @Override
    public void stopped(String resourceID, String storeName, DownloadManager downloadManager) {
        System.out.println(initMessage + "download stopped for resource " + resourceID);

    }
}
