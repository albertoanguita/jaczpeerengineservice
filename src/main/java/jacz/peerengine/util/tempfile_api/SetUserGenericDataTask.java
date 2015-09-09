package jacz.peerengine.util.tempfile_api;

import jacz.util.concurrency.task_executor.ParallelTask;

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
            index.setUserGenericData(group, userGenericData);
            TempFileManager.writeIndexFile(indexFilePath, index);
        } catch (ClassNotFoundException e) {
            ioException = new IOException("Problems reading the temp index file");
        } catch (IOException e) {
            ioException = e;
        }
    }

    public void checkCorrectResult() throws IOException {
        if (ioException != null) {
            throw ioException;
        }
    }
}
