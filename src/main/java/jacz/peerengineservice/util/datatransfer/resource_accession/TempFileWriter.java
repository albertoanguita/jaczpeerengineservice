package jacz.peerengineservice.util.datatransfer.resource_accession;

import jacz.util.numeric.LongRange;
import jacz.util.numeric.RangeSet;
import jacz.peerengineservice.util.tempfile_api.TempFileManager;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * A resource writer implementation for temporary files
 */
public class TempFileWriter implements ResourceWriter {

    private final TempFileManager tempFileManager;

    private final String tempFile;

    private String finalPath;

    public TempFileWriter(TempFileManager tempFileManager) throws IOException {
        this(tempFileManager, tempFileManager.createNewTempFile());
    }

    public TempFileWriter(TempFileManager tempFileManager, String tempFile) {
        this.tempFileManager = tempFileManager;
        this.tempFile = tempFile;
        finalPath = null;
    }

    public String getTempFile() {
        return tempFile;
    }

    @Override
    public Long getSize() throws IOException {
        return tempFileManager.getTemporaryResourceSize(tempFile);
    }

    @Override
    public RangeSet<LongRange, Long> getAvailableSegments() throws IOException {
        return tempFileManager.getTemporaryOwnedParts(tempFile);
    }

    @Override
    public void setUserGenericData(String group, Map<String, Serializable> userGenericData) throws IOException {
        tempFileManager.setUserGenericData(tempFile, group, userGenericData);
    }

    @Override
    public Map<String, Serializable> getUserGenericData(String group) throws IOException {
        return tempFileManager.getUserGenericData(tempFile, group);
    }

    @Override
    public void setUserGenericDataField(String group, String key, Serializable value) throws IOException {
        tempFileManager.setUserGenericDataField(tempFile, group, key, value);
    }

    @Override
    public Serializable getUserGenericDataField(String group, String key) throws IOException {
        return tempFileManager.getUserGenericDataField(tempFile, group, key);
    }

    @Override
    public void init(long size) throws IOException {
        tempFileManager.setTemporaryResourceSize(tempFile, size);
    }

    @Override
    public void write(long offset, byte[] data) throws IOException, IndexOutOfBoundsException {
        tempFileManager.write(tempFile, offset, data);
    }

    @Override
    public void complete() throws IOException {
        finalPath = tempFileManager.completeTempFile(tempFile);
    }

    @Override
    public void cancel() {
        try {
            tempFileManager.removeTempFile(tempFile);
        } catch (IOException e) {
            // the temporary file was not correctly removed, but nothing we can do
        }
    }

    @Override
    public void stop() {
        // nothing to do here. The resource is already stored in the disk
    }

    @Override
    public String getPath() {
        return finalPath;
    }

    public String getTempDataFilePath() {
        return tempFileManager.tempFileToDataPath(tempFile);
    }

    public static String getTempIndex(String tempDataFilePath) {
        return TempFileManager.dataPathToTempFile(tempDataFilePath);
    }
}
