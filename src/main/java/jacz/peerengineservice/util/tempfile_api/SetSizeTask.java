package jacz.peerengineservice.util.tempfile_api;

import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.io.object_serialization.VersionedSerializationException;

import java.io.IOException;

/**
 *
 */
class SetSizeTask implements ParallelTask {

    /**
     * Temp file to read from
     */
    private String indexFilePath;

    private long size;

    private IOException ioException;

    public SetSizeTask(String indexFilePath, long size) {
        this.indexFilePath = indexFilePath;
        this.size = size;
        ioException = null;
    }

    @Override
    public void performTask() {
        // obtain the TempIndex from the index file
        TempIndex index;
        try {
            index = TempFileManager.readIndexFile(indexFilePath);
            index.setTotalSize(size);
            TempFileManager.writeIndexFile(indexFilePath, index);
        } catch (IOException e) {
            ioException = e;
        } catch (VersionedSerializationException e) {
            ioException = new IOException("Problems reading the temp index file");
        }
    }

    public void checkCorrectResult() throws IOException {
        if (ioException != null) {
            throw ioException;
        }
    }
}
