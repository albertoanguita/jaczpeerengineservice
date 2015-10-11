package jacz.peerengineservice.util.tempfile_api;

import jacz.util.numeric.range.LongRangeList;

import java.io.IOException;

/**
 *
 */
class OwnedPartsTask extends TempIndexTask {

    private LongRangeList ownedParts;

    public OwnedPartsTask(String indexFilePath) {
        super(indexFilePath);
        ownedParts = null;
    }

    @Override
    public void performTask() {
        super.performTask();
        if (tempIndex != null) {
            ownedParts = tempIndex.getOwnedDataParts();
        }
    }

    public LongRangeList getOwnedParts() throws IOException {
        checkIOException();
        return ownedParts;
    }
}
