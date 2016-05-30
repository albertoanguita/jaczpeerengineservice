package jacz.peerengineservice.util.tempfile_api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
            Files.delete(Paths.get(indexFilePath));
            Files.delete(Paths.get(TempFileManager.generateBackupPath(indexFilePath)));
        } catch (IOException e) {
            // ignore this exception, cannot happen or we do not care
        }
    }

    public String getFinalPath() throws IOException {
        super.checkIOException();
        return finalPath;
    }
}
