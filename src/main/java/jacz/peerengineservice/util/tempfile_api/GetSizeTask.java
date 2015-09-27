package jacz.peerengineservice.util.tempfile_api;

import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.io.object_serialization.VersionedSerializationException;

import java.io.IOException;

/**
 *
 */
class GetSizeTask implements ParallelTask {

    /**
     * Temp file to read from
     */
    private String indexFilePath;

    private Long size;

    private IOException ioException;

    public GetSizeTask(String indexFilePath) {
        this.indexFilePath = indexFilePath;
        size = null;
        ioException = null;
    }

    @Override
    public void performTask() {
        // obtain the TempIndex from the index file
        TempIndex index;
        try {
            index = TempFileManager.readIndexFile(indexFilePath);
            size = index.getTotalResourceSize();
        } catch (IOException e) {
            ioException = e;
        } catch (VersionedSerializationException e) {
            ioException = new IOException("Problems reading the temp index file");
        }
    }

    public Long getSize() throws IOException {
        return size;
    }
}
