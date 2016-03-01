package jacz.peerengineservice.util.tempfile_api;

import java.io.IOException;

/**
 *
 */
class SetSizeTask extends TempIndexTask {

    private long size;

    public SetSizeTask(TempFileManager tempFileManager, String indexFilePath, long size) {
        super(tempFileManager, indexFilePath);
        this.size = size;
    }

    @Override
    public void run() {
        super.run();
        if (tempIndex != null) {
            try {
                tempIndex.setTotalSize(size);
                TempFileManager.writeIndexFile(indexFilePath, tempIndex);
            } catch (IOException e) {
                ioException = e;
            }
        }
    }

    public void checkCorrectResult() throws IOException {
        super.checkIOException();
    }
}
