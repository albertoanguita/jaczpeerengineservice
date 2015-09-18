package jacz.peerengineservice.util.tempfile_api;

import jacz.util.concurrency.task_executor.ParallelTask;

import java.io.IOException;
import java.io.Serializable;

/**
 *
 */
class SetUserGenericDataFieldTask implements ParallelTask {

    /**
     * Temp file to read from
     */
    private String indexFilePath;

    /**
     * key group to access
     */
    private String group;

    private String key;

    private Serializable value;

    private IOException ioException;

    public SetUserGenericDataFieldTask(String indexFilePath, String group, String key, Serializable value) {
        this.indexFilePath = indexFilePath;
        this.group = group;
        this.key = key;
        this.value = value;
        ioException = null;
    }

    @Override
    public void performTask() {
        // obtain the TempIndex from the index file
        TempIndex index;
        try {
            index = TempFileManager.readIndexFile(indexFilePath);
            index.setUserGenericDataField(group, key, value);
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
