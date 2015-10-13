package jacz.peerengineservice.util.tempfile_api;

import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.io.object_serialization.VersionedSerializationException;

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
    public void performTask() {
        super.performTask();
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
