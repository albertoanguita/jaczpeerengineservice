package jacz.peerengineservice.util.tempfile_api;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 *
 */
class CompleterTask extends TempIndexTask {

    private String finalPath;

    public CompleterTask(TempFileManager tempFileManager, String indexFilePath) {
        super(tempFileManager, indexFilePath);
        finalPath = null;
    }

    @Override
    public void run() {
        super.run();
        if (tempIndex != null) {
            finalPath = tempIndex.getTempDataFilePath();
        }
        try {
            FileUtils.forceDelete(new File(indexFilePath));
            FileUtils.forceDelete(new File(TempFileManager.generateBackupPath(indexFilePath)));
        } catch (IOException e) {
            // ignore this exception, cannot happen or we do not care
        }
    }

    public String getFinalPath() throws IOException {
        super.checkIOException();
        return finalPath;
    }
}
