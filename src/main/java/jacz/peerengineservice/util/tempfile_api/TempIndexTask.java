package jacz.peerengineservice.util.tempfile_api;

import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.io.object_serialization.VersionedSerializationException;

import java.io.IOException;

/**
 * Generic parallel task over a temp index file
 */
public abstract class TempIndexTask implements ParallelTask {

    /**
     * Temp file to read from
     */
    protected final String indexFilePath;

    protected TempIndex tempIndex;

    protected IOException ioException;

    protected IndexOutOfBoundsException indexOutOfBoundsException;

    public TempIndexTask(String indexFilePath) {
        this.indexFilePath = indexFilePath;
        tempIndex = null;
        ioException = null;
        indexOutOfBoundsException = null;
    }


    @Override
    public void performTask() {
        // obtain the TempIndex from the index file
        try {
            tempIndex = TempFileManager.readIndexFile(indexFilePath);
        } catch (IOException e) {
            ioException = e;
        } catch (VersionedSerializationException e) {
            ioException = new IOException("Problems reading the temp index file");
        }
    }

    protected void checkIOException() throws IOException {
        if (ioException != null) {
            throw ioException;
        }
    }

    protected void checkIndexOutOfBoundsException() throws IndexOutOfBoundsException {
        if (indexOutOfBoundsException != null) {
            throw indexOutOfBoundsException;
        }
    }
}
