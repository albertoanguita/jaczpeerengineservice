package jacz.peerengineservice.util.datatransfer;

import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.peerengineservice.util.datatransfer.master.ProviderStatistics;
import jacz.peerengineservice.util.datatransfer.master.ResourcePart;
import jacz.util.numeric.range.LongRange;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceWriter;

/**
 * This interface contains methods that are invoked during a resource download process. It is implemented by the
 * client, and it is a way for the client to receive news about the progress of a download. Each time the client
 * wants to download a resource, he provides an implementation of this interface to follow up the download
 */
public interface DownloadProgressNotificationHandler {

    /**
     * Possible causes of the cancellation of a download
     */
    public enum CancellationReason {
        // the cancellation was issued by the user
        USER,
        // the cancellation was due to an IO failure in the resource writer
        IO_FAILURE
    }

    /**
     * The download just started. This is invoked during the invocation of the download method itself
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param downloadManager download manager associated to this download
     */
    public void started(String resourceID, String storeName, DownloadManager downloadManager);

    /**
     * The download obtained the total resource size
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param downloadManager download manager associated to this download
     * @param resourceSize    the reported resource size in bytes
     */
    public void resourceSize(String resourceID, String storeName, DownloadManager downloadManager, long resourceSize);

    /**
     * This is invoked every time a provider was added to the download
     *
     * @param resourceID         id of the corresponding downloaded resource
     * @param storeName          name of the resource store from which the resource is being downloaded
     * @param providerStatistics data about the added provider
     * @param downloadManager    download manager associated to this download
     */
    public void providerAdded(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, String providerId);

    /**
     * This is invoked every time a provider was removed from the download
     *
     * @param resourceID         id of the corresponding downloaded resource
     * @param storeName          name of the resource store from which the resource is being downloaded
     * @param providerStatistics data about the removed provider
     * @param downloadManager    download manager associated to this download
     */
    public void providerRemoved(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, String providerId);

    /**
     * One of the active providers reported the part of the resource that he is currently sharing
     *
     * @param resourceID         id of the corresponding downloaded resource
     * @param storeName          name of the resource store from which the resource is being downloaded
     * @param providerStatistics data about the provider
     * @param downloadManager    download manager associated to this download
     * @param sharedPart         part of the resource shared by the provider
     */
    public void providerReportedSharedPart(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, ResourcePart sharedPart);

    /**
     * One of the active providers received a new assignment (a segment of the resource) for transferring to us
     *
     * @param resourceID         id of the corresponding downloaded resource
     * @param storeName          name of the resource store from which the resource is being downloaded
     * @param providerStatistics data about the provider
     * @param downloadManager    download manager associated to this download
     * @param assignedSegment    the new assigned segment (the total assignation can be accessed through the included provider statistics)
     */
    public void providerWasAssignedSegment(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, LongRange assignedSegment);

    /**
     * The assignation of an active provider was cleared (this can happen in 3 situations: i) the download was paused so all provider's assignation
     * was cleared, ii) the provider slowed down too much, so its assignation is cleared to avoid him taking too long to transfer the assignation, and
     * iii) the provider's speed was below a minimum allowed speed). The cause is not provided here
     *
     * @param resourceID         id of the corresponding downloaded resource
     * @param storeName          name of the resource store from which the resource is being downloaded
     * @param providerStatistics data about the provider
     * @param downloadManager    download manager associated to this download
     */
    public void providerWasClearedAssignation(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager);

    /**
     * The download was paused (the user issued it)
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param downloadManager download manager associated to this download
     */
    public void paused(String resourceID, String storeName, DownloadManager downloadManager);

    /**
     * The download was resumed (the user issued it)
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param downloadManager download manager associated to this download
     */
    public void resumed(String resourceID, String storeName, DownloadManager downloadManager);

    /**
     * Successfully checked an intermediate hash
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param segment         downloaded segment
     * @param downloadManager download manager associated to this download
     */
    public void downloadedSegment(String resourceID, String storeName, LongRange segment, DownloadManager downloadManager);

    /**
     * Successfully checked an intermediate hash
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param range           range of the checked intermediate hash
     * @param downloadManager download manager associated to this download
     */
    public void successIntermediateHash(String resourceID, String storeName, LongRange range, DownloadManager downloadManager);

    /**
     * Failed when checking an intermediate hash. The part will be downloaded again
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param range           range of the checked intermediate hash
     * @param downloadManager download manager associated to this download
     */
    public void failedIntermediateHash(String resourceID, String storeName, LongRange range, DownloadManager downloadManager);

    /**
     * The system cannot check an intermediate hash due to a not valid hash algorithm
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param range           range of the checked intermediate hash
     * @param hashAlgorithm   invalid hash algorithm
     * @param downloadManager download manager associated to this download
     */
    public void invalidIntermediateHashAlgorithm(String resourceID, String storeName, LongRange range, String hashAlgorithm, DownloadManager downloadManager);

    /**
     * The total hash is currently being checked for the downloaded resource. Percentage values are not repeated
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param percentage      value between 0 and 100 with the percentage completed. Not repeated in different invocations. The first is always 0,
     *                        and the last is always 100
     * @param downloadManager download manager associated to this download
     */
    public void checkingTotalHash(String resourceID, String storeName, int percentage, DownloadManager downloadManager);

    /**
     * The total hash was successfully checked on the downloaded resource. The completed method will be invoked next
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param downloadManager download manager associated to this download
     */
    public void successTotalHash(String resourceID, String storeName, DownloadManager downloadManager);

    /**
     * The total hash failed when checking. The download will not start again, since the resource writer was already completed. The user must
     * invoke the download again if he wishes so. The completed method will be invoked normally right after this method
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param downloadManager download manager associated to this download
     */
    public void failedTotalHash(String resourceID, String storeName, DownloadManager downloadManager);

    /**
     * The system cannot check the total hash due to a not valid hash algorithm
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param hashAlgorithm   invalid hash algorithm
     * @param downloadManager download manager associated to this download
     */
    public void invalidTotalHashAlgorithm(String resourceID, String storeName, String hashAlgorithm, DownloadManager downloadManager);

    /**
     * The download was completed. The corresponding resource writer already completed its own "complete" operation,
     * so the resource is ready for full use
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param resourceWriter  the resource writer in charge of writing the downloaded resource
     * @param downloadManager download manager associated to this download
     */
    public void completed(String resourceID, String storeName, ResourceWriter resourceWriter, DownloadManager downloadManager);

    /**
     * This is invoked when the download process was cancelled (no possibility of resuming). This can be caused by
     * the user of by an error
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param reason          reason of this cancellation
     * @param downloadManager download manager associated to this download
     */
    public void cancelled(String resourceID, String storeName, CancellationReason reason, DownloadManager downloadManager);

    /**
     * This is invoked when the download process was stopped by the user (with the intention of backing up the
     * download for later use). No resuming is allowed. A new download process must be initiated.
     *
     * @param resourceID      id of the corresponding downloaded resource
     * @param storeName       name of the resource store from which the resource is being downloaded
     * @param downloadManager download manager associated to this download
     */
    public void stopped(String resourceID, String storeName, DownloadManager downloadManager);
}
