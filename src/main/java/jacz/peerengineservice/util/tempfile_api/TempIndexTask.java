package jacz.peerengineservice.util.tempfile_api;

import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.io.serialization.VersionedSerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Generic parallel task over a temp index file
 */
public abstract class TempIndexTask implements ParallelTask {

    final static Logger logger = LoggerFactory.getLogger(TempIndexTask.class);

    private final TempFileManager tempFileManager;

    /**
     * Temp file to read from
     */
    protected final String indexFilePath;

    protected TempIndex tempIndex;

    protected IOException ioException;

    protected IndexOutOfBoundsException indexOutOfBoundsException;

    public TempIndexTask(TempFileManager tempFileManager, String indexFilePath) {
        this.tempFileManager = tempFileManager;
        this.indexFilePath = indexFilePath;
        tempIndex = null;
        ioException = null;
        indexOutOfBoundsException = null;
    }


    @Override
    public void performTask() {
        // obtain the TempIndex from the index file
        try {
            tempIndex = tempFileManager.readIndexFile(indexFilePath);
        } catch (IOException e) {
            ioException = e;
        } catch (VersionedSerializationException e) {
            ioException = new IOException("Problems reading the temp index file: " + e.getMessage());
        }
    }

    protected void checkIOException() throws IOException {
        if (ioException != null) {
            logger.error("Error reading a temp file" + ioException);
            throw ioException;
        }
    }

    protected void checkIndexOutOfBoundsException() throws IndexOutOfBoundsException {
        if (indexOutOfBoundsException != null) {
            logger.error("Error reading a temp file" + indexOutOfBoundsException);
            throw indexOutOfBoundsException;
        }
    }
}
