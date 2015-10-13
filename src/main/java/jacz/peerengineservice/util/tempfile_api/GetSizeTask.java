package jacz.peerengineservice.util.tempfile_api;

import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.io.object_serialization.VersionedSerializationException;

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
    public void performTask() {
        super.performTask();
        if (tempIndex != null) {
            size = tempIndex.getTotalResourceSize();
        }
    }

    public Long getSize() throws IOException {
        super.checkIOException();
        return size;
    }
}
