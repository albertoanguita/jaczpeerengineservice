package jacz.peerengineservice.util.tempfile_api;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

/**
 * Retrieves the stored user dictionary from a temp index file
 */
public class GetUserDictionary extends TempIndexTask {

    private HashMap<String, Serializable> userDictionary;

    public GetUserDictionary(TempFileManager tempFileManager, String indexFilePath) {
        super(tempFileManager, indexFilePath);
        userDictionary = null;
    }

    @Override
    public void run() {
        super.run();
        if (tempIndex != null) {
            userDictionary = tempIndex.getUserDictionary();
        }
    }

    public HashMap<String, Serializable> getUserDictionary() throws IOException {
        checkIOException();
        return userDictionary;
    }
}
