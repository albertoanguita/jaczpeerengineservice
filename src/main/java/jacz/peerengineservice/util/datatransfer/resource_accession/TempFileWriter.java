package jacz.peerengineservice.util.datatransfer.resource_accession;

import jacz.peerengineservice.util.tempfile_api.TempFileManager;
import jacz.util.numeric.range.LongRangeList;
import jacz.util.numeric.range.RangeList;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * A resource writer implementation for temporary files
 */
public class TempFileWriter implements ResourceWriter {

    private final TempFileManager tempFileManager;

    private final String tempFile;

    private final Map<String, Serializable> customDictionary;

    private String finalPath;

    public TempFileWriter(TempFileManager tempFileManager, String customGroup, Map<String, Serializable> customDictionary) throws IOException {
        this.tempFileManager = tempFileManager;
        this.tempFile = tempFileManager.createNewTempFile();
        this.customDictionary = customDictionary;
        if (customGroup != null && customDictionary != null) {
            tempFileManager.setCustomGroup(tempFile, customGroup, customDictionary);
        }
    }

    public TempFileWriter(TempFileManager tempFileManager, String tempFile, String customGroup) throws IOException {
        this.tempFileManager = tempFileManager;
        this.tempFile = tempFile;
        if (customGroup != null) {
            customDictionary = tempFileManager.getCustomGroup(tempFile, customGroup);
        } else {
            customDictionary = null;
        }
    }

    public String getTempFile() {
        return tempFile;
    }

    @Override
    public Long getSize() throws IOException {
        return tempFileManager.getTemporaryResourceSize(tempFile);
    }

    @Override
    public LongRangeList getAvailableSegments() throws IOException {
        return tempFileManager.getTemporaryOwnedParts(tempFile);
    }

    @Override
    public Map<String, Serializable> getCustomGroup(String group) throws IOException {
        return tempFileManager.getCustomGroup(tempFile, group);
    }

    @Override
    public Serializable getCustomGroupField(String group, String key) throws IOException {
        return tempFileManager.getCustomGroupField(tempFile, group, key);
    }

    @Override
    public void setCustomGroup(String group, Map<String, Serializable> userGenericData) throws IOException {
        tempFileManager.setCustomGroup(tempFile, group, userGenericData);
    }

    @Override
    public void setCustomGroupField(String group, String key, Serializable value) throws IOException {
        tempFileManager.setCustomGroupField(tempFile, group, key, value);
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

    public Map<String, Serializable> getCustomDictionary() {
        return customDictionary;
    }
}
