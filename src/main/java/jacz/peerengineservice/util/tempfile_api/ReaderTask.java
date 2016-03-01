package jacz.peerengineservice.util.tempfile_api;

import java.io.IOException;

/**
 *
 */
class ReaderTask extends TempIndexTask {

    private long offset;

    private int length;

    /**
     * Data read
     */
    private byte[] data;

    public ReaderTask(TempFileManager tempFileManager, String indexFilePath, long offset, int length) {
        super(tempFileManager, indexFilePath);
        this.offset = offset;
        this.length = length;
        data = null;
    }

    @Override
    public void run() {
        super.run();
        if (tempIndex != null) {
            try {
                data = tempIndex.read(offset, length);
            } catch (IOException e) {
                ioException = e;
            } catch (IndexOutOfBoundsException e) {
                data = null;
                indexOutOfBoundsException = e;
            }
        }
    }

    public byte[] getData() throws IOException, IndexOutOfBoundsException {
        checkIOException();
        checkIndexOutOfBoundsException();
        return data;
    }
}
