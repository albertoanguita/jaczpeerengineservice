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
    public void indexFileGenerated(final String indexFilePath) {
        logger.info("INDEX FILE GENERATED. indexFilePath: " + indexFilePath);
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                tempFileManagerEvents.indexFileGenerated(indexFilePath);
            }
        });
    }

    @Override
    public void indexFileRecovered(final String indexFilePath) {
        logger.info("INDEX FILE RECOVERED. indexFilePath: " + indexFilePath);
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                tempFileManagerEvents.indexFileRecovered(indexFilePath);
            }
        });
    }

    @Override
    public void indexFileErrorRestoredWithBackup(final String indexFilePath) {
        logger.info("INDEX FILE ERROR RESTORED WITH BACKUP. indexFilePath: " + indexFilePath);
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                tempFileManagerEvents.indexFileErrorRestoredWithBackup(indexFilePath);
            }
        });
    }

    @Override
    public void indexFileError(final String indexFilePath) {
        logger.info("INDEX FILE ERROR. indexFilePath: " + indexFilePath);
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                tempFileManagerEvents.indexFileError(indexFilePath);
            }
        });
    }

    public void stop() {
        sequentialTaskExecutor.shutdown();
    }
}
