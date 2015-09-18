package jacz.peerengineservice.util.tempfile_api;

import jacz.util.concurrency.task_executor.ParallelTask;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 *
 */
class GetUserGenericDataTask implements ParallelTask {

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

    public GetUserGenericDataTask(String indexFilePath, String group) {
        this.indexFilePath = indexFilePath;
        this.group = group;
        userGenericData = null;
        ioException = null;
    }

    @Override
    public void performTask() {
        // obtain the TempIndex from the index file
        TempIndex index;
        try {
            index = TempFileManager.readIndexFile(indexFilePath);
            userGenericData = index.getUserGenericData(group);
        } catch (ClassNotFoundException e) {
            ioException = new IOException("Problems reading the temp index file");
        } catch (IOException e) {
            ioException = e;
        }
    }

    public Map<String, Serializable> getUserGenericData() throws IOException {
        if (ioException == null) {
            return userGenericData;
        } else {
            throw ioException;
        }
    }
}
