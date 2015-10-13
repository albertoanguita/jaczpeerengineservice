package jacz.peerengineservice.util.tempfile_api;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

/**
 * Sets a system field in a temp index file
 */
public class SetSystemField extends TempIndexTask {

    private final String key;

    private final Serializable value;

    public SetSystemField(TempFileManager tempFileManager, String indexFilePath, String key, Serializable value) {
        super(tempFileManager, indexFilePath);
        this.key = key;
        this.value = value;
    }

    @Override
    public void performTask() {
        super.performTask();
        if (tempIndex != null) {
            try {
                tempIndex.setSystemField(key, value);
                TempFileManager.writeIndexFile(indexFilePath, tempIndex);
            } catch (IOException e) {
                ioException = e;
            }
        }
    }

    public void checkCorrectResult() throws IOException {
        checkIOException();
    }
}
