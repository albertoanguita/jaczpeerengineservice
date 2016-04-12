package jacz.peerengineservice.util.datatransfer.master;

import jacz.peerengineservice.util.datatransfer.DownloadProgressNotificationHandler;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceProvider;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceWriter;
import jacz.util.numeric.range.LongRange;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class handles events happened during a download and transmits them to the user
 */
class DownloadReports {

    private final DownloadManager downloadManager;

    private final String resourceID;

    private final String storeName;

    private final DownloadProgressNotificationHandler downloadProgressNotificationHandler;

    private final ExecutorService sequentialTaskExecutor;

    private ResourceDownloadStatistics resourceDownloadStatistics;


    DownloadReports(DownloadManager downloadManager, String resourceID, String storeName, DownloadProgressNotificationHandler downloadProgressNotificationHandler) {
        this.downloadManager = downloadManager;
        this.resourceID = resourceID;
        this.storeName = storeName;
        this.downloadProgressNotificationHandler = downloadProgressNotificationHandler;
        sequentialTaskExecutor = Executors.newSingleThreadExecutor();
    }

    public ResourceDownloadStatistics getResourceDownloadStatistics() {
        return resourceDownloadStatistics;
    }

    public void initializeWriting(ResourceWriter resourceWriter) throws IOException {
        resourceDownloadStatistics = new ResourceDownloadStatistics(resourceWriter);
        if (downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.started(resourceID, storeName, downloadManager);
                }
            });
        }
    }

    void reportResourceSize(final long size) {
        if (downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.resourceSize(resourceID, storeName, downloadManager, size);
                }
            });
        }
    }

    void addProvider(final ResourceProvider resourceProvider) {
        final ProviderStatistics providerStatistics = resourceDownloadStatistics.addProvider(resourceProvider);
        if (downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.providerAdded(resourceID, storeName, providerStatistics, downloadManager, resourceProvider.getPeerID());
                }
            });
        }
    }

    void removeProvider(final ResourceProvider resourceProvider) {
        final ProviderStatistics providerStatistics = resourceDownloadStatistics.removeProvider(resourceProvider);
        if (downloadProgressNotificationHandler != null && providerStatistics != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.providerRemoved(resourceID, storeName, providerStatistics, downloadManager, resourceProvider.getPeerID());
                }
            });
        }
    }

    void reportSharedPart(ResourceProvider resourceProvider, final ResourcePart resourcePart) {
        final ProviderStatistics providerStatistics = resourceDownloadStatistics.reportSharedPart(resourceProvider, resourcePart);
        if (downloadProgressNotificationHandler != null && providerStatistics != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.providerReportedSharedPart(resourceID, storeName, providerStatistics, downloadManager, resourcePart);
                }
            });
        }
    }

    void reportAssignedSegment(ResourceProvider resourceProvider, final LongRange segment) {
        final ProviderStatistics providerStatistics = resourceDownloadStatistics.reportAssignedPart(resourceProvider, segment);
        if (downloadProgressNotificationHandler != null && providerStatistics != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.providerWasAssignedSegment(resourceID, storeName, providerStatistics, downloadManager, segment);
                }
            });
        }
    }

    void reportClearedAssignation(ResourceProvider resourceProvider) {
        final ProviderStatistics providerStatistics = resourceDownloadStatistics.reportClearedAssignation(resourceProvider);
        if (downloadProgressNotificationHandler != null && providerStatistics != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.providerWasClearedAssignation(resourceID, storeName, providerStatistics, downloadManager);
                }
            });
        }
    }

    void reportDownloadedSegment(ResourceProvider resourceProvider, final LongRange segment) {
        resourceDownloadStatistics.reportDownloadedPart(resourceProvider, segment);
        if (downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.downloadedSegment(resourceID, storeName, segment, downloadManager);
                }
            });
        }
    }

    void reportCheckingTotalHash(final int percentage) {
        if (downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.checkingTotalHash(resourceID, storeName, percentage, downloadManager);
                }
            });
        }
    }

    void reportCorrectTotalHash() {
        if (downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.successTotalHash(resourceID, storeName, downloadManager);
                }
            });
        }
    }

    void reportFailedTotalHash() {
        if (downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.failedTotalHash(resourceID, storeName, downloadManager);
                }
            });
        }
    }

    void reportInvalidTotalHashAlgorithm(final String hashAlgorithm) {
        if (downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.invalidTotalHashAlgorithm(resourceID, storeName, hashAlgorithm, downloadManager);
                }
            });
        }
    }

    void reportCompleted(final ResourceWriter resourceWriter, final long resourceSize) {
        if (downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.completed(resourceID, storeName, resourceWriter, downloadManager);
                }
            });
        }
        resourceDownloadStatistics.downloadComplete(resourceSize);
        resourceDownloadStatistics.stop();
    }

    void reportCancelled(final DownloadProgressNotificationHandler.CancellationReason reason) {
        if (downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.cancelled(resourceID, storeName, reason, downloadManager);
                }
            });
        }
    }

    void reportPaused() {
        if (downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.paused(resourceID, storeName, downloadManager);
                }
            });
        }
    }

    void reportResumed() {
        if (downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.resumed(resourceID, storeName, downloadManager);
                }
            });
        }
    }

    void reportStopped() throws IOException {
        resourceDownloadStatistics.stopSession();
        if (downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.stopped(resourceID, storeName, downloadManager);
                }
            });
        }
    }

    void stop() {
        sequentialTaskExecutor.shutdown();
    }
}
