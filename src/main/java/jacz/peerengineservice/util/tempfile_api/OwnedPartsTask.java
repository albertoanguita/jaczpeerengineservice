package jacz.peerengineservice.util.tempfile_api;

import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.io.object_serialization.VersionedSerializationException;
import jacz.util.numeric.oldrange.RangeSet;
import jacz.util.numeric.range.LongRange;
import jacz.util.numeric.range.LongRangeList;
import jacz.util.numeric.range.RangeList;

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
