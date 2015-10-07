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
class OwnedPartsTask implements ParallelTask {

    /**
     * Temp file to read from
     */
    private String indexFilePath;

    private LongRangeList ownedParts;

    private IOException ioException;

    public OwnedPartsTask(String indexFilePath) {
        this.indexFilePath = indexFilePath;
        ownedParts = null;
        ioException = null;
    }

    @Override
    public void performTask() {
        // obtain the TempIndex from the index file
        TempIndex index;
        try {
            index = TempFileManager.readIndexFile(indexFilePath);
            ownedParts = index.getOwnedDataParts();
        } catch (IOException e) {
            ioException = e;
        } catch (VersionedSerializationException e) {
            ioException = new IOException("Problems reading the temp index file");
        }
    }

    public LongRangeList getOwnedParts() throws IOException {
        if (ownedParts != null) {
            return ownedParts;
        } else {
            throw ioException;
        }
    }
}
