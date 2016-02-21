package jacz.peerengineservice.util.datatransfer;

import jacz.peerengineservice.PeerID;
import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.concurrency.task_executor.SequentialTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class acts as a bypass of the client's provided ResourceTransferEvents implementation, logging all activity
 * <p/>
 * In addition, it takes care of generating a thread for each method call, so invoker does not have to worry about it
 */
public class ResourceTransferEventsBridge implements ResourceTransferEvents {

    final static Logger logger = LoggerFactory.getLogger(ResourceTransferEvents.class);

    private final ResourceTransferEvents resourceTransferEvents;

    private final SequentialTaskExecutor sequentialTaskExecutor;

    public ResourceTransferEventsBridge(ResourceTransferEvents resourceTransferEvents) {
        this.resourceTransferEvents = resourceTransferEvents;
        sequentialTaskExecutor = new SequentialTaskExecutor();
    }

    @Override
    public void addLocalResourceStore(final String name) {
        logger.info("ADD LOCAL RESOURCE STORE. Name: " + name);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.addLocalResourceStore(name);
            }
        });
    }

    @Override
    public void setLocalGeneralResourceStore() {
        logger.info("SET LOCAL GENERAL RESOURCE STORE");
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.setLocalGeneralResourceStore();
            }
        });
    }

    @Override
    public void addForeignResourceStore(final String name) {
        logger.info("ADD FOREIGN RESOURCE STORE. Name: " + name);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.addForeignResourceStore(name);
            }
        });
    }

    @Override
    public void removeLocalResourceStore(final String name) {
        logger.info("REMOVE LOCAL RESOURCE STORE. Name: " + name);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.removeLocalResourceStore(name);
            }
        });
    }

    @Override
    public void removeLocalGeneralResourceStore() {
        logger.info("REMOVE LOCAL GENERAL RESOURCE STORE");
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.removeLocalGeneralResourceStore();
            }
        });
    }

    @Override
    public void removeForeignResourceStore(final String name) {
        logger.info("REMOVE FOREIGN RESOURCE STORE. Name: " + name);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.removeForeignResourceStore(name);
            }
        });
    }

    @Override
    public void globalDownloadInitiated(final String resourceStoreName, final String resourceID, final double streamingNeed, final String totalHash, final String totalHashAlgorithm) {
        logger.info("GLOBAL DOWNLOAD INITIATED: " + "resourceStoreName: " + resourceStoreName + ". resourceID: " + resourceID + ". streamingNeed: " + streamingNeed + ". totalHash: " + totalHash + ". totalHashAlgorithm: " + totalHashAlgorithm);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.globalDownloadInitiated(resourceStoreName, resourceID, streamingNeed, totalHash, totalHashAlgorithm);
            }
        });
    }

    @Override
    public void peerDownloadInitiated(final PeerID serverPeerID, final String resourceStoreName, final String resourceID, final double streamingNeed, final String totalHash, final String totalHashAlgorithm) {
        logger.info("PEER DOWNLOAD INITIATED: " + "serverPeerID: " + serverPeerID + ". resourceStoreName: " + resourceStoreName + ". resourceID: " + resourceID + ". streamingNeed: " + streamingNeed + ". totalHash: " + totalHash + ". totalHashAlgorithm: " + totalHashAlgorithm);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.peerDownloadInitiated(serverPeerID, resourceStoreName, resourceID, streamingNeed, totalHash, totalHashAlgorithm);
            }
        });
    }

    @Override
    public void setMaxDesiredDownloadSpeed(final Float totalMaxDesiredSpeed) {
        logger.info("SET MAX DESIRED DOWNLOAD SPEED. totalMaxDesiredSpeed: " + totalMaxDesiredSpeed);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.setMaxDesiredDownloadSpeed(totalMaxDesiredSpeed);
            }
        });
    }

    @Override
    public void setMaxDesiredUploadSpeed(final Float totalMaxDesiredSpeed) {
        logger.info("SET MAX DESIRED UPLOAD SPEED. totalMaxDesiredSpeed: " + totalMaxDesiredSpeed);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.setMaxDesiredUploadSpeed(totalMaxDesiredSpeed);
            }
        });
    }

    @Override
    public void setAccuracy(final double accuracy) {
        logger.info("SET ACCURACY. Accuracy: " + accuracy);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.setAccuracy(accuracy);
            }
        });
    }

    @Override
    public void approveResourceRequest(final ResourceRequest request, final ResourceStoreResponse response) {
        logger.info("APPROVE RESOURCE REQUEST. Request: " + request + ". Response: " + response);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.approveResourceRequest(request, response);
            }
        });
    }

    @Override
    public void denyUnavailableSubchannelResourceRequest(final ResourceRequest request, final ResourceStoreResponse response) {
        logger.info("DENY TO UNAVAILABLE SUBCHANNEL RESOURCE REQUEST. Request: " + request + ". Response: " + response);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.denyUnavailableSubchannelResourceRequest(request, response);
            }
        });
    }

    @Override
    public void deniedResourceRequest(final ResourceRequest request, final ResourceStoreResponse response) {
        logger.info("DENIED RESOURCE REQUEST. Request: " + request + ". Response: " + response);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.deniedResourceRequest(request, response);
            }
        });
    }

    @Override
    public void periodicDownloadsNotification(final DownloadsManager downloadsManager) {
        // no log of active downloads
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.periodicDownloadsNotification(downloadsManager);
            }
        });
    }

    @Override
    public void periodicUploadsNotification(final UploadsManager uploadsManager) {
        // no log of active uploads
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.periodicUploadsNotification(uploadsManager);
            }
        });
    }

    public void stop() {
        logger.info("STOP");
        sequentialTaskExecutor.stopAndWaitForFinalization();
    }
}
