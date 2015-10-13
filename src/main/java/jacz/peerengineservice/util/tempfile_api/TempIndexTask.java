package jacz.peerengineservice.util.tempfile_api;

import jacz.peerengineservice.client.PeerClient;
import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.io.object_serialization.VersionedSerializationException;
import jacz.util.log.ErrorLog;

import java.io.IOException;

/**
 * Generic parallel task over a temp index file
 */
public abstract class TempIndexTask implements ParallelTask {

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
            ErrorLog.reportError(PeerClient.ERROR_LOG, "Error reading a temp file", ioException);
            throw ioException;
        }
    }

    protected void checkIndexOutOfBoundsException() throws IndexOutOfBoundsException {
        if (indexOutOfBoundsException != null) {
            ErrorLog.reportError(PeerClient.ERROR_LOG, "Error reading a temp file", indexOutOfBoundsException);
            throw indexOutOfBoundsException;
        }
    }
}
