package jacz.peerengineservice.util.tempfile_api;

import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.io.object_serialization.VersionedSerializationException;
import jacz.util.numeric.LongRange;
import jacz.util.numeric.RangeSet;

import java.io.IOException;

/**
 *
 */
class OwnedPartsTask implements ParallelTask {

    /**
     * Temp file to read from
     */
    private String indexFilePath;

    private RangeSet<LongRange, Long> ownedParts;

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

    public RangeSet<LongRange, Long> getOwnedParts() throws IOException {
        if (ownedParts != null) {
            return ownedParts;
        } else {
            throw ioException;
        }
    }
}
