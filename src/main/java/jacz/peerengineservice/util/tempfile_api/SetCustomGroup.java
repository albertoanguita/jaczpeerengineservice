package jacz.peerengineservice.util.tempfile_api;

import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.io.object_serialization.VersionedSerializationException;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 *
 */
class SetCustomGroup implements ParallelTask {

    /**
     * Temp file to read from
     */
    private String indexFilePath;

    /**
     * key group to access
     */
    private String group;

    private Map<String, Serializable> customGroup;

    private String key;

    private Serializable value;

    private IOException ioException;

    public SetCustomGroup(String indexFilePath, String group, Map<String, Serializable> customGroup) {
        this(indexFilePath, group, customGroup, null, null);
    }

    public SetCustomGroup(String indexFilePath, String group, String key, Serializable value) {
        this(indexFilePath, group, null, key, value);
    }

    private SetCustomGroup(String indexFilePath, String group, Map<String, Serializable> customGroup, String key, Serializable value) {
        this.indexFilePath = indexFilePath;
        this.group = group;
        this.customGroup = customGroup;
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
            if (customGroup != null) {
                index.setCustomGroup(group, customGroup);
            } else {
                index.setCustomGroupField(group, key, value);
            }
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
