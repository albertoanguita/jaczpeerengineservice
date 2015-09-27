package jacz.peerengineservice.util.tempfile_api;

import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.io.object_serialization.VersionedSerializationException;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 *
 */
class SetUserGenericDataTask implements ParallelTask {

    /**
     * Temp file to read from
     */
    private String indexFilePath;

    /**
     * key group to access
     */
    private String group;

    private Map<String, Serializable> userGenericData;

    private IOException ioException;

    public SetUserGenericDataTask(String indexFilePath, String group, Map<String, Serializable> userGenericData) {
        this.indexFilePath = indexFilePath;
        this.group = group;
        this.userGenericData = userGenericData;
        ioException = null;
    }

    @Override
    public void performTask() {
        // obtain the TempIndex from the index file
        TempIndex index;
        try {
            index = TempFileManager.readIndexFile(indexFilePath);
            index.setCustomGroup(group, userGenericData);
            TempFileManager.writeIndexFile(indexFilePath, index);
        } catch (IOException e) {
            ioException = e;
        } catch (VersionedSerializationException e) {
            ioException = new IOException("Problems reading the temp index file");
        }
    }

    public void checkCorrectResult() throws IOException {
        if (ioException != null) {
            throw ioException;
        }
    }
}
