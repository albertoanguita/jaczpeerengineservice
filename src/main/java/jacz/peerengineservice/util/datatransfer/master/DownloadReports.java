package jacz.peerengineservice.util.datatransfer.master;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.datatransfer.DownloadProgressNotificationHandler;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceWriter;
import org.aanguita.jacuzzi.numeric.range.LongRange;

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

    DownloadReports(DownloadManager downloadManager, String resourceID, String storeName, DownloadProgressNotificationHandler downloadProgressNotificationHandler) {
        this.downloadManager = downloadManager;
        this.resourceID = resourceID;
        this.storeName = storeName;
        this.downloadProgressNotificationHandler = downloadProgressNotificationHandler;
        sequentialTaskExecutor = Executors.newSingleThreadExecutor();
    }

    public void initializeWriting() throws IOException {
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

    void addProvider(ProviderStatistics providerStatistics, PeerId peerId) {
        if (downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.providerAdded(resourceID, storeName, providerStatistics, downloadManager, peerId);
                }
            });
        }
    }

    void removeProvider(ProviderStatistics providerStatistics, PeerId peerId) {
        if (downloadProgressNotificationHandler != null && providerStatistics != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.providerRemoved(resourceID, storeName, providerStatistics, downloadManager, peerId);
                }
            });
        }
    }

    void reportSharedPart(ProviderStatistics providerStatistics, final ResourcePart resourcePart) {
        if (downloadProgressNotificationHandler != null && providerStatistics != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.providerReportedSharedPart(resourceID, storeName, providerStatistics, downloadManager, resourcePart);
                }
            });
        }
    }

    void reportAssignedSegment(ProviderStatistics providerStatistics, final LongRange segment) {
        if (downloadProgressNotificationHandler != null && providerStatistics != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.providerWasAssignedSegment(resourceID, storeName, providerStatistics, downloadManager, segment);
                }
            });
        }
    }

    void reportClearedAssignation(ProviderStatistics providerStatistics) {
        if (downloadProgressNotificationHandler != null && providerStatistics != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.providerWasClearedAssignation(resourceID, storeName, providerStatistics, downloadManager);
                }
            });
        }
    }

    void reportDownloadedSegment(final LongRange segment) {
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

    void reportCompleted(final ResourceWriter resourceWriter) {
        if (downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    downloadProgressNotificationHandler.completed(resourceID, storeName, resourceWriter, downloadManager);
                }
            });
        }
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
