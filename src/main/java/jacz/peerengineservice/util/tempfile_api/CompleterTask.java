package jacz.peerengineservice.util.tempfile_api;

import jacz.util.files.FileUtil;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 *
 */
class CompleterTask extends TempIndexTask {

    private String finalPath;

    public CompleterTask(String indexFilePath) {
        super(indexFilePath);
        finalPath = null;
    }

    @Override
    public void performTask() {
        super.performTask();
        if (tempIndex != null) {
            finalPath = tempIndex.getTempDataFilePath();
        }
        try {
            FileUtil.deleteFile(indexFilePath);
        } catch (FileNotFoundException e) {
            // ignore this exception, cannot happen
        }
    }

    public String getFinalPath() throws IOException {
        super.checkIOException();
        return finalPath;
    }
}
