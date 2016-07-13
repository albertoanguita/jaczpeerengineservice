package jacz.peerengineservice.util.datatransfer;

import jacz.peerengineservice.PeerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class acts as a bypass of the client's provided ResourceTransferEvents implementation, logging all activity
 * <p/>
 * In addition, it takes care of generating a thread for each method call, so invoker does not have to worry about it
 */
public class ResourceTransferEventsBridge implements ResourceTransferEvents {

    final static Logger logger = LoggerFactory.getLogger(ResourceTransferEvents.class);

    private final ResourceTransferEvents resourceTransferEvents;

    private final ExecutorService sequentialTaskExecutor;

    public ResourceTransferEventsBridge(ResourceTransferEvents resourceTransferEvents) {
        this.resourceTransferEvents = resourceTransferEvents;
        sequentialTaskExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public synchronized void addLocalResourceStore(final String name) {
        logger.info("ADD LOCAL RESOURCE STORE. Name: " + name);
        if (!sequentialTaskExecutor.isShutdown()) {
            sequentialTaskExecutor.submit(() -> resourceTransferEvents.addLocalResourceStore(name));
        }
    }

    @Override
    public synchronized void setLocalGeneralResourceStore() {
        logger.info("SET LOCAL GENERAL RESOURCE STORE");
        if (!sequentialTaskExecutor.isShutdown()) {
            sequentialTaskExecutor.submit(resourceTransferEvents::setLocalGeneralResourceStore);
        }
    }

    @Override
    public synchronized void addForeignResourceStore(final String name) {
        logger.info("ADD FOREIGN RESOURCE STORE. Name: " + name);
        if (!sequentialTaskExecutor.isShutdown()) {
            sequentialTaskExecutor.submit(() -> resourceTransferEvents.addForeignResourceStore(name));
        }
    }

    @Override
    public synchronized void removeLocalResourceStore(final String name) {
        logger.info("REMOVE LOCAL RESOURCE STORE. Name: " + name);
        if (!sequentialTaskExecutor.isShutdown()) {
            sequentialTaskExecutor.submit(() -> resourceTransferEvents.removeLocalResourceStore(name));
        }
    }

    @Override
    public synchronized void removeLocalGeneralResourceStore() {
        logger.info("REMOVE LOCAL GENERAL RESOURCE STORE");
        if (!sequentialTaskExecutor.isShutdown()) {
            sequentialTaskExecutor.submit(resourceTransferEvents::removeLocalGeneralResourceStore);
        }
    }

    @Override
    public synchronized void removeForeignResourceStore(final String name) {
        logger.info("REMOVE FOREIGN RESOURCE STORE. Name: " + name);
        if (!sequentialTaskExecutor.isShutdown()) {
            sequentialTaskExecutor.submit(() -> resourceTransferEvents.removeForeignResourceStore(name));
        }
    }

    @Override
    public synchronized void updateResourceProviders(String resourceId, Set<PeerId> providers) {
        logger.info("UPDATE RESOURCE PROVIDERS. Resource id: " + resourceId);
        if (!sequentialTaskExecutor.isShutdown()) {
            sequentialTaskExecutor.submit(() -> resourceTransferEvents.updateResourceProviders(resourceId, providers));
        }
    }

    @Override
    public synchronized void globalDownloadInitiated(final String resourceStoreName, final String resourceID, final double streamingNeed, final String totalHash, final String totalHashAlgorithm) {
        logger.info("GLOBAL DOWNLOAD INITIATED: " + "resourceStoreName: " + resourceStoreName + ". resourceID: " + resourceID + ". streamingNeed: " + streamingNeed + ". totalHash: " + totalHash + ". totalHashAlgorithm: " + totalHashAlgorithm);
        if (!sequentialTaskExecutor.isShutdown()) {
            sequentialTaskExecutor.submit(() -> resourceTransferEvents.globalDownloadInitiated(resourceStoreName, resourceID, streamingNeed, totalHash, totalHashAlgorithm));
        }
    }

    @Override
    public synchronized void peerDownloadInitiated(final PeerId serverPeerId, final String resourceStoreName, final String resourceID, final double streamingNeed, final String totalHash, final String totalHashAlgorithm) {
        logger.info("PEER DOWNLOAD INITIATED: " + "serverPeerId: " + serverPeerId + ". resourceStoreName: " + resourceStoreName + ". resourceID: " + resourceID + ". streamingNeed: " + streamingNeed + ". totalHash: " + totalHash + ". totalHashAlgorithm: " + totalHashAlgorithm);
        if (!sequentialTaskExecutor.isShutdown()) {
            sequentialTaskExecutor.submit(() -> resourceTransferEvents.peerDownloadInitiated(serverPeerId, resourceStoreName, resourceID, streamingNeed, totalHash, totalHashAlgorithm));
        }
    }

    @Override
    public synchronized void setMaxDesiredDownloadSpeed(final Float totalMaxDesiredSpeed) {
        logger.info("SET MAX DESIRED DOWNLOAD SPEED. totalMaxDesiredSpeed: " + totalMaxDesiredSpeed);
        if (!sequentialTaskExecutor.isShutdown()) {
            sequentialTaskExecutor.submit(() -> resourceTransferEvents.setMaxDesiredDownloadSpeed(totalMaxDesiredSpeed));
        }
    }

    @Override
    public synchronized void setMaxDesiredUploadSpeed(final Float totalMaxDesiredSpeed) {
        logger.info("SET MAX DESIRED UPLOAD SPEED. totalMaxDesiredSpeed: " + totalMaxDesiredSpeed);
        if (!sequentialTaskExecutor.isShutdown()) {
            sequentialTaskExecutor.submit(() -> resourceTransferEvents.setMaxDesiredUploadSpeed(totalMaxDesiredSpeed));
        }
    }

    @Override
    public synchronized void setAccuracy(final double accuracy) {
        logger.info("SET ACCURACY. Accuracy: " + accuracy);
        if (!sequentialTaskExecutor.isShutdown()) {
            sequentialTaskExecutor.submit(() -> resourceTransferEvents.setAccuracy(accuracy));
        }
    }

    @Override
    public synchronized void approveResourceRequest(final ResourceRequest request, final ResourceStoreResponse response) {
        logger.info("APPROVE RESOURCE REQUEST. Request: " + request + ". Response: " + response);
        if (!sequentialTaskExecutor.isShutdown()) {
            sequentialTaskExecutor.submit(() -> resourceTransferEvents.approveResourceRequest(request, response));
        }
    }

    @Override
    public synchronized void denyUnavailableSubchannelResourceRequest(final ResourceRequest request, final ResourceStoreResponse response) {
        logger.info("DENY TO UNAVAILABLE SUBCHANNEL RESOURCE REQUEST. Request: " + request + ". Response: " + response);
        if (!sequentialTaskExecutor.isShutdown()) {
            sequentialTaskExecutor.submit(() -> resourceTransferEvents.denyUnavailableSubchannelResourceRequest(request, response));
        }
    }

    @Override
    public synchronized void deniedResourceRequest(final ResourceRequest request, final ResourceStoreResponse response) {
        logger.info("DENIED RESOURCE REQUEST. Request: " + request + ". Response: " + response);
        if (!sequentialTaskExecutor.isShutdown()) {
            sequentialTaskExecutor.submit(() -> resourceTransferEvents.deniedResourceRequest(request, response));
        }
    }

    @Override
    public synchronized void periodicDownloadsNotification(final DownloadsManager downloadsManager) {
        // no log of active downloads
        if (!sequentialTaskExecutor.isShutdown()) {
            sequentialTaskExecutor.submit(() -> resourceTransferEvents.periodicDownloadsNotification(downloadsManager));
        }
    }

    @Override
    public synchronized void periodicUploadsNotification(final UploadsManager uploadsManager) {
        // no log of active uploads
        if (!sequentialTaskExecutor.isShutdown()) {
            sequentialTaskExecutor.submit(() -> resourceTransferEvents.periodicUploadsNotification(uploadsManager));
        }
    }

    public synchronized void stop() {
        logger.info("STOP");
        sequentialTaskExecutor.shutdown();
    }
}
