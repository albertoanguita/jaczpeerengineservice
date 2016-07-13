package jacz.peerengineservice.util.tempfile_api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by Alberto on 13/10/2015.
 */
public class TempFileManagerEventsBridge implements TempFileManagerEvents {

    final static Logger logger = LoggerFactory.getLogger(TempFileManagerEvents.class);

    private final TempFileManagerEvents tempFileManagerEvents;

    private final ExecutorService sequentialTaskExecutor;

    public TempFileManagerEventsBridge(TempFileManagerEvents tempFileManagerEvents) {
        this.tempFileManagerEvents = tempFileManagerEvents;
        sequentialTaskExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public synchronized void indexFileGenerated(final String indexFilePath) {
        logger.info("INDEX FILE GENERATED. indexFilePath: " + indexFilePath);
        if (!sequentialTaskExecutor.isShutdown()) {
            sequentialTaskExecutor.submit(() -> tempFileManagerEvents.indexFileGenerated(indexFilePath));
        }
    }

    @Override
    public synchronized void indexFileRecovered(final String indexFilePath) {
        logger.info("INDEX FILE RECOVERED. indexFilePath: " + indexFilePath);
        if (!sequentialTaskExecutor.isShutdown()) {
            sequentialTaskExecutor.submit(() -> tempFileManagerEvents.indexFileRecovered(indexFilePath));
        }
    }

    @Override
    public synchronized void indexFileErrorRestoredWithBackup(final String indexFilePath) {
        logger.info("INDEX FILE ERROR RESTORED WITH BACKUP. indexFilePath: " + indexFilePath);
        if (!sequentialTaskExecutor.isShutdown()) {
            sequentialTaskExecutor.submit(() -> tempFileManagerEvents.indexFileErrorRestoredWithBackup(indexFilePath));
        }
    }

    @Override
    public synchronized void indexFileError(final String indexFilePath, Exception e) {
        logger.info("INDEX FILE ERROR. indexFilePath: " + indexFilePath);
        if (!sequentialTaskExecutor.isShutdown()) {
            sequentialTaskExecutor.submit(() -> tempFileManagerEvents.indexFileError(indexFilePath, e));
        }
    }

    public synchronized void stop() {
        sequentialTaskExecutor.shutdown();
    }
}
