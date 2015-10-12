package jacz.peerengineservice.util.datatransfer;

import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.concurrency.task_executor.SequentialTaskExecutor;
import org.apache.log4j.Logger;

/**
 * This class acts as a bypass of the client's provided ResourceTransferEvents implementation, logging all activity
 * <p/>
 * In addition, it takes care of generating a thread for each method call, so invoker does not have to worry about it
 */
public class ResourceTransferEventsBridge implements ResourceTransferEvents {

    final static Logger logger = Logger.getLogger(ResourceTransferEvents.class);

    private final ResourceTransferEvents resourceTransferEvents;

    private final SequentialTaskExecutor sequentialTaskExecutor;

    public ResourceTransferEventsBridge(ResourceTransferEvents resourceTransferEvents) {
        this.resourceTransferEvents = resourceTransferEvents;
        sequentialTaskExecutor = new SequentialTaskExecutor();
    }

    @Override
    public void addLocalResourceStore(final String name) {
        logger.debug("ADD LOCAL RESOURCE STORE. Name: " + name);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.addLocalResourceStore(name);
            }
        });
    }

    @Override
    public void setLocalGeneralResourceStore() {
        logger.debug("SET LOCAL GENERAL RESOURCE STORE");
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.setLocalGeneralResourceStore();
            }
        });
    }

    @Override
    public void addForeignResourceStore(final String name) {
        logger.debug("ADD FOREIGN RESOURCE STORE. Name: " + name);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.addForeignResourceStore(name);
            }
        });
    }

    @Override
    public void removeLocalResourceStore(final String name) {
        logger.debug("REMOVE LOCAL RESOURCE STORE. Name: " + name);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.removeLocalResourceStore(name);
            }
        });
    }

    @Override
    public void removeLocalGeneralResourceStore() {
        logger.debug("REMOVE LOCAL GENERAL RESOURCE STORE");
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.removeLocalGeneralResourceStore();
            }
        });
    }

    @Override
    public void removeForeignResourceStore(final String name) {
        logger.debug("REMOVE FOREIGN RESOURCE STORE. Name: " + name);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.removeForeignResourceStore(name);
            }
        });
    }

    @Override
    public void globalDownloadInitiated() {

        // todo

    }

    @Override
    public void globalDownloadDenied() {

    }

    @Override
    public void peerDownloadInitiated() {

    }

    @Override
    public void peerDownloadDenied() {

    }

    @Override
    public void setMaxDesiredDownloadSpeed(final Float totalMaxDesiredSpeed) {
        logger.debug("SET MAX DESIRED DOWNLOAD SPEED. totalMaxDesiredSpeed: " + totalMaxDesiredSpeed);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.setMaxDesiredDownloadSpeed(totalMaxDesiredSpeed);
            }
        });
    }

    @Override
    public void setMaxDesiredUploadSpeed(final Float totalMaxDesiredSpeed) {
        logger.debug("SET MAX DESIRED UPLOAD SPEED. totalMaxDesiredSpeed: " + totalMaxDesiredSpeed);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.setMaxDesiredUploadSpeed(totalMaxDesiredSpeed);
            }
        });
    }

    @Override
    public void setAccuracy(final double accuracy) {
        logger.debug("SET ACCURACY. Accuracy: " + accuracy);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.setAccuracy(accuracy);
            }
        });
    }

    @Override
    public void approveResourceRequest(final ResourceRequest request, final ResourceStoreResponse response) {
        logger.debug("APPROVE RESOURCE REQUEST. Request: " + request + ". Response: " + response);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.approveResourceRequest(request, response);
            }
        });
    }

    @Override
    public void denyUnavailableSubchannelResourceRequest(final ResourceRequest request, final ResourceStoreResponse response) {
        logger.debug("DENY TO UNAVAILABLE SUBCHANNEL RESOURCE REQUEST. Request: " + request + ". Response: " + response);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.denyUnavailableSubchannelResourceRequest(request, response);
            }
        });
    }

    @Override
    public void deniedResourceRequest(final ResourceRequest request, final ResourceStoreResponse response) {
        logger.debug("DENIED RESOURCE REQUEST. Request: " + request + ". Response: " + response);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.deniedResourceRequest(request, response);
            }
        });
    }

    @Override
    public void stop() {
        logger.info("STOP");
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                resourceTransferEvents.stop();
            }
        });
        sequentialTaskExecutor.stopAndWaitForFinalization();
    }
}
