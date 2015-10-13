package jacz.peerengineservice.util.tempfile_api;

import jacz.peerengineservice.util.datatransfer.ResourceTransferEvents;
import jacz.util.concurrency.task_executor.SequentialTaskExecutor;
import org.apache.log4j.Logger;

/**
 * Created by Alberto on 13/10/2015.
 */
public class TempFileManagerEventsBridge implements TempFileManagerEvents {

    final static Logger logger = Logger.getLogger(ResourceTransferEvents.class);

    private final TempFileManagerEvents tempFileManagerEvents;

    private final SequentialTaskExecutor sequentialTaskExecutor;

    public TempFileManagerEventsBridge(TempFileManagerEvents tempFileManagerEvents) {
        this.tempFileManagerEvents = tempFileManagerEvents;
        sequentialTaskExecutor = new SequentialTaskExecutor();
    }

    @Override
    public void indexFileErrorRestoredWithBackup(String indexFilePath) {
        // todo
    }

    @Override
    public void indexFileError(String indexFilePath) {

    }

    public void stop() {
        // todo invoke
        sequentialTaskExecutor.stopAndWaitForFinalization();
    }
}
