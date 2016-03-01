package jacz.peerengineservice.util.tempfile_api;

import java.io.IOException;

/**
 *
 */
class GetSizeTask extends TempIndexTask {

    private Long size;

    public GetSizeTask(TempFileManager tempFileManager, String indexFilePath) {
        super(tempFileManager, indexFilePath);
        size = null;
    }

    @Override
    public void run() {
        super.run();
        if (tempIndex != null) {
            size = tempIndex.getTotalResourceSize();
        }
    }

    public Long getSize() throws IOException {
        super.checkIOException();
        return size;
    }
}
