package jacz.peerengineservice.util.tempfile_api;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

/**
 * Retrieves the stored system dictionary from a temp index file
 */
public class GetSystemDictionary extends TempIndexTask {

    private HashMap<String, Serializable> systemDictionary;

    public GetSystemDictionary(String indexFilePath) {
        super(indexFilePath);
        systemDictionary = null;
    }

    @Override
    public void performTask() {
        super.performTask();
        if (tempIndex != null) {
            systemDictionary = tempIndex.getSystemDictionary();
        }
    }

    public HashMap<String, Serializable> getSystemDictionary() throws IOException {
        checkIOException();
        return systemDictionary;
    }
}
