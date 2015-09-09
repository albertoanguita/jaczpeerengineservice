package jacz.peerengine.util.tempfile_api;

import jacz.util.concurrency.task_executor.ParallelTask;

import java.io.IOException;

/**
 *
 */
class WriterTask implements ParallelTask {

    /**
     * Temp file to write to
     */
    private String indexFilePath;

    private long offset;

    /**
     * Data to write to the file
     */
    private byte[] data;

    private IOException ioException;

    private IndexOutOfBoundsException indexOutOfBoundsException;

    public WriterTask(String indexFilePath, long offset, byte[] data) {
        this.indexFilePath = indexFilePath;
        this.offset = offset;
        this.data = data;
        ioException = null;
        indexOutOfBoundsException = null;
    }

    @Override
    public void performTask() {
        // obtain the TempIndex from the index file
        TempIndex index;
        try {
            index = TempFileManager.readIndexFile(indexFilePath);
            index.write(offset, data);
            TempFileManager.writeIndexFile(indexFilePath, index);
        } catch (ClassNotFoundException e) {
            ioException = new IOException("Problems reading the temp index file");
        } catch (IOException e) {
            ioException = e;
        } catch (IndexOutOfBoundsException e) {
            indexOutOfBoundsException = e;
        }
    }

    public void checkCorrectResult() throws IOException, IndexOutOfBoundsException {
        if (ioException != null) {
            throw ioException;
        } else if (indexOutOfBoundsException != null) {
            throw indexOutOfBoundsException;
        }
    }
}
