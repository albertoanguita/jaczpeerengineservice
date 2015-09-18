package jacz.peerengineservice.util.tempfile_api;

import jacz.util.concurrency.task_executor.ParallelTask;

import java.io.IOException;

/**
 *
 */
class ReaderTask implements ParallelTask {

    /**
     * Temp file to read from
     */
    private String indexFilePath;

    private long offset;

    private int length;

    /**
     * Data read
     */
    private byte[] data;

    private IOException ioException;

    private IndexOutOfBoundsException indexOutOfBoundsException;

    public ReaderTask(String indexFilePath, long offset, int length) {
        this.indexFilePath = indexFilePath;
        this.offset = offset;
        this.length = length;
        data = null;
        ioException = null;
        indexOutOfBoundsException = null;
    }

    @Override
    public void performTask() {
        // obtain the TempIndex from the index file
        TempIndex index;
        try {
            index = TempFileManager.readIndexFile(indexFilePath);
            data = index.read(offset, length);
        } catch (ClassNotFoundException e) {
            data = null;
            ioException = new IOException("Problems reading the temp index file");
        } catch (IOException e) {
            data = null;
            ioException = e;
        } catch (IndexOutOfBoundsException e) {
            data = null;
            indexOutOfBoundsException = e;
        }
    }

    public byte[] getData() throws IOException, IndexOutOfBoundsException {
        if (data != null) {
            return data;
        } else if (ioException != null) {
            throw ioException;
        } else {
            throw indexOutOfBoundsException;
        }
    }
}
