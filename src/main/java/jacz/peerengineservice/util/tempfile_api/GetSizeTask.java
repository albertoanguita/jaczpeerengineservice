package jacz.peerengineservice.util.tempfile_api;

import jacz.util.concurrency.task_executor.ParallelTask;

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
        } catch (ClassNotFoundException e) {
            ioException = new IOException("Problems reading the temp index file");
        } catch (IOException e) {
            ioException = e;
        }
    }

    public Long getSize() throws IOException {
        return size;
    }
}
