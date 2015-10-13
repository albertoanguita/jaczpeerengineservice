package jacz.peerengineservice.util.tempfile_api;

import java.io.IOException;

/**
 *
 */
class WriterTask extends TempIndexTask {

    private long offset;

    /**
     * Data to write to the file
     */
    private byte[] data;

    public WriterTask(TempFileManager tempFileManager, String indexFilePath, long offset, byte[] data) {
        super(tempFileManager, indexFilePath);
        this.offset = offset;
        this.data = data;
    }

    @Override
    public void performTask() {
        super.performTask();
        if (tempIndex != null) {
            try {
                tempIndex.write(offset, data);
                TempFileManager.writeIndexFile(indexFilePath, tempIndex);
            } catch (IOException e) {
                ioException = e;
            } catch (IndexOutOfBoundsException e) {
                indexOutOfBoundsException = e;
            }
        }
    }

    public void checkCorrectResult() throws IOException, IndexOutOfBoundsException {
        checkIOException();
        checkIndexOutOfBoundsException();
    }
}
