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
        if (!sequentialTaskExecutor.isShutdown() && downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(() -> downloadProgressNotificationHandler.started(resourceID, storeName, downloadManager));
        }
    }

    synchronized void reportResourceSize(final long size) {
        if (!sequentialTaskExecutor.isShutdown() && downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(() -> downloadProgressNotificationHandler.resourceSize(resourceID, storeName, downloadManager, size));
        }
    }

    synchronized void addProvider(ProviderStatistics providerStatistics, PeerId peerId) {
        if (!sequentialTaskExecutor.isShutdown() && downloadProgressNotificationHandler != null && providerStatistics != null) {
            sequentialTaskExecutor.submit(() -> downloadProgressNotificationHandler.providerAdded(resourceID, storeName, providerStatistics, downloadManager, peerId));
        }
    }

    synchronized void removeProvider(ProviderStatistics providerStatistics, PeerId peerId) {
        if (!sequentialTaskExecutor.isShutdown() && downloadProgressNotificationHandler != null && providerStatistics != null) {
            sequentialTaskExecutor.submit(() -> downloadProgressNotificationHandler.providerRemoved(resourceID, storeName, providerStatistics, downloadManager, peerId));
        }
    }

    synchronized void reportSharedPart(ProviderStatistics providerStatistics, final ResourcePart resourcePart) {
        if (!sequentialTaskExecutor.isShutdown() && downloadProgressNotificationHandler != null && providerStatistics != null) {
            sequentialTaskExecutor.submit(() -> downloadProgressNotificationHandler.providerReportedSharedPart(resourceID, storeName, providerStatistics, downloadManager, resourcePart));
        }
    }

    synchronized void reportAssignedSegment(ProviderStatistics providerStatistics, final LongRange segment) {
        if (!sequentialTaskExecutor.isShutdown() && downloadProgressNotificationHandler != null && providerStatistics != null) {
            sequentialTaskExecutor.submit(() -> downloadProgressNotificationHandler.providerWasAssignedSegment(resourceID, storeName, providerStatistics, downloadManager, segment));
        }
    }

    synchronized void reportClearedAssignation(ProviderStatistics providerStatistics) {
        if (!sequentialTaskExecutor.isShutdown() && downloadProgressNotificationHandler != null && providerStatistics != null) {
            sequentialTaskExecutor.submit(() -> downloadProgressNotificationHandler.providerWasClearedAssignation(resourceID, storeName, providerStatistics, downloadManager));
        }
    }

    synchronized void reportDownloadedSegment(final LongRange segment) {
        if (!sequentialTaskExecutor.isShutdown() && downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(() -> downloadProgressNotificationHandler.downloadedSegment(resourceID, storeName, segment, downloadManager));
        }
    }

    synchronized void reportCheckingTotalHash(final int percentage) {
        if (!sequentialTaskExecutor.isShutdown() && downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(() -> downloadProgressNotificationHandler.checkingTotalHash(resourceID, storeName, percentage, downloadManager));
        }
    }

    synchronized void reportCorrectTotalHash() {
        if (!sequentialTaskExecutor.isShutdown() && downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(() -> downloadProgressNotificationHandler.successTotalHash(resourceID, storeName, downloadManager));
        }
    }

    synchronized void reportFailedTotalHash() {
        if (!sequentialTaskExecutor.isShutdown() && downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(() -> downloadProgressNotificationHandler.failedTotalHash(resourceID, storeName, downloadManager));
        }
    }

    synchronized void reportInvalidTotalHashAlgorithm(final String hashAlgorithm) {
        if (!sequentialTaskExecutor.isShutdown() && downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(() -> downloadProgressNotificationHandler.invalidTotalHashAlgorithm(resourceID, storeName, hashAlgorithm, downloadManager));
        }
    }

    synchronized void reportCompleted(final ResourceWriter resourceWriter) {
        if (!sequentialTaskExecutor.isShutdown() && downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(() -> downloadProgressNotificationHandler.completed(resourceID, storeName, resourceWriter, downloadManager));
        }
    }

    synchronized void reportCancelled(final DownloadProgressNotificationHandler.CancellationReason reason, Exception e) {
        if (!sequentialTaskExecutor.isShutdown() && downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(() -> downloadProgressNotificationHandler.cancelled(resourceID, storeName, reason, e, downloadManager));
        }
    }

    synchronized void reportPaused() {
        if (!sequentialTaskExecutor.isShutdown() && downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(() -> downloadProgressNotificationHandler.paused(resourceID, storeName, downloadManager));
        }
    }

    synchronized void reportResumed() {
        if (!sequentialTaskExecutor.isShutdown() && downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(() -> downloadProgressNotificationHandler.resumed(resourceID, storeName, downloadManager));
        }
    }

    synchronized void reportStopped() throws IOException {
        if (!sequentialTaskExecutor.isShutdown() && downloadProgressNotificationHandler != null) {
            sequentialTaskExecutor.submit(() -> downloadProgressNotificationHandler.stopped(resourceID, storeName, downloadManager));
        }
    }

    synchronized void stop() {
        sequentialTaskExecutor.shutdown();
    }
}
