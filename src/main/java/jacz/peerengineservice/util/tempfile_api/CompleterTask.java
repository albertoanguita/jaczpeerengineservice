package jacz.peerengineservice.util.tempfile_api;

import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.files.FileUtil;
import jacz.util.io.object_serialization.VersionedSerializationException;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 *
 */
class CompleterTask implements ParallelTask {

    /**
     * Temp file to read from
     */
    private final String indexFilePath;

    private String finalPath;

    private IOException ioException;

    public CompleterTask(String indexFilePath) {
        this.indexFilePath = indexFilePath;
        finalPath = null;
        ioException = null;
    }

    @Override
    public void performTask() {
        // obtain the TempIndex from the index file
        TempIndex index;
        try {
            index = TempFileManager.readIndexFile(indexFilePath);
            finalPath = index.getTempDataFilePath();
        } catch (IOException e) {
            ioException = e;
        } catch (VersionedSerializationException e) {
            ioException = new IOException("Problems reading the temp index file");
        }
        try {
            FileUtil.deleteFile(indexFilePath);
        } catch (FileNotFoundException e) {
            // ignore this exception, cannot happen
        }
    }

    public String getFinalPath() throws IOException {
        if (finalPath != null) {
            return finalPath;
        } else {
            throw ioException;
        }
    }
}
