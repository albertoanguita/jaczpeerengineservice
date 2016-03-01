package jacz.peerengineservice.util.tempfile_api;

import jacz.util.numeric.range.LongRangeList;

import java.io.IOException;

/**
 *
 */
class OwnedPartsTask extends TempIndexTask {

    private LongRangeList ownedParts;

    public OwnedPartsTask(TempFileManager tempFileManager, String indexFilePath) {
        super(tempFileManager, indexFilePath);
        ownedParts = null;
    }

    @Override
    public void run() {
        super.run();
        if (tempIndex != null) {
            ownedParts = tempIndex.getOwnedDataParts();
        }
    }

    public LongRangeList getOwnedParts() throws IOException {
        checkIOException();
        return ownedParts;
    }
}
