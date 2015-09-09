package jacz.peerengine.util.tempfile_api;

import jacz.util.concurrency.task_executor.ParallelTask;

import java.io.IOException;
import java.io.Serializable;

/**
 *
 */
class GetUserGenericDataFieldTask implements ParallelTask {

    /**
     * Temp file to read from
     */
    private String indexFilePath;

    /**
     * key group to access
     */
    private String group;

    /**
     * Key of the field to retrieve
     */
    private String key;

    private Serializable serializable;

    private IOException ioException;

    public GetUserGenericDataFieldTask(String indexFilePath, String group, String key) {
        this.indexFilePath = indexFilePath;
        this.group = group;
        this.key = key;
        serializable = null;
        ioException = null;
    }

    @Override
    public void performTask() {
        // obtain the TempIndex from the index file
        TempIndex index;
        try {
            index = TempFileManager.readIndexFile(indexFilePath);
            serializable = index.getUserGenericDataField(group, key);
        } catch (ClassNotFoundException e) {
            ioException = new IOException("Problems reading the temp index file");
        } catch (IOException e) {
            ioException = e;
        }
    }

    public Serializable getUserGenericDataField() throws IOException {
        if (ioException == null) {
            return serializable;
        } else {
            throw ioException;
        }
    }
}
