package jacz.peerengineservice.util.datatransfer.resource_accession;

import jacz.peerengineservice.util.tempfile_api.TempFileManager;
import org.aanguita.jacuzzi.numeric.range.LongRangeList;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

/**
 * A resource writer implementation for temporary files
 */
public class TempFileWriter implements ResourceWriter {

    private final TempFileManager tempFileManager;

    private final String tempFile;

//    private final Map<String, Serializable> customDictionary;

    private final HashMap<String, Serializable> userDictionary;

    private String finalPath;

    /**
     * New temporary download
     *
     * @param tempFileManager temp file manager to handle this temp download
     * @throws IOException
     */
    public TempFileWriter(TempFileManager tempFileManager) throws IOException {
        this(tempFileManager, new HashMap<>());
    }

    /**
     * New temporary download
     *
     * @param tempFileManager temp file manager to handle this temp download
     * @param userDictionary  user terms to store in the dictionary
     * @throws IOException
     */
    public TempFileWriter(TempFileManager tempFileManager, HashMap<String, Serializable> userDictionary) throws IOException {
        this.tempFileManager = tempFileManager;
        this.tempFile = tempFileManager.createNewTempFile(userDictionary);
        this.userDictionary = userDictionary;
    }

    /**
     * Constructor for recovering an old download
     *
     * @param tempFileManager temp file manager to handle this temp download
     * @param tempFile        existing temp file
     * @throws IOException
     */
    public TempFileWriter(TempFileManager tempFileManager, String tempFile) throws IOException {
        this.tempFileManager = tempFileManager;
        this.tempFile = tempFile;
        userDictionary = tempFileManager.getUserDictionary(tempFile);
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
    public HashMap<String, Serializable> getUserDictionary() {
        return userDictionary;
    }

    @Override
    public HashMap<String, Serializable> getSystemDictionary() throws IOException {
        return tempFileManager.getSystemDictionary(tempFile);
    }

    @Override
    public void setSystemField(String key, Serializable value) throws IOException {
        tempFileManager.setSystemField(tempFile, key, value);
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

//    public Map<String, Serializable> getCustomDictionary() {
//        return customDictionary;
//    }
}
