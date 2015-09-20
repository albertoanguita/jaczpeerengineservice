package jacz.peerengineservice.util.tempfile_api;

import jacz.util.concurrency.task_executor.ParallelTask;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 *
 */
class GetCustomGroup implements ParallelTask {

    /**
     * Temp file to read from
     */
    private String indexFilePath;

    /**
     * key group to access
     */
    private String group;

    private Map<String, Serializable> customGroup;

    /**
     * Key of the field to retrieve
     */
    private String key;

    private Serializable value;

    private IOException ioException;

    public GetCustomGroup(String indexFilePath, String group) {
        this(indexFilePath, group, null);
    }

    public GetCustomGroup(String indexFilePath, String group, String key) {
        this.indexFilePath = indexFilePath;
        this.group = group;
        this.key = key;
        customGroup = null;
        value = null;
        ioException = null;
    }

    @Override
    public void performTask() {
        // obtain the TempIndex from the index file
        TempIndex index;
        try {
            index = TempFileManager.readIndexFile(indexFilePath);
            if (key == null) {
                customGroup = index.getCustomGroup(group);
            } else {
                value = index.getCustomGroupField(group, key);
            }
        } catch (ClassNotFoundException e) {
            ioException = new IOException("Problems reading the temp index file");
        } catch (IOException e) {
            ioException = e;
        }
    }

    public Map<String, Serializable> getCustomGroup() throws IOException {
        if (ioException == null) {
            return customGroup;
        } else {
            throw ioException;
        }
    }

    public Serializable getValue() throws IOException {
        if (ioException == null) {
            return value;
        } else {
            throw ioException;
        }
    }
}
