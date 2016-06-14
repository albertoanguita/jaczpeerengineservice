package jacz.peerengineservice.util.tempfile_api;

import jacz.util.files.RandomAccess;
import jacz.util.io.serialization.*;
import jacz.util.numeric.range.LongRange;
import jacz.util.numeric.range.LongRangeList;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides an object representation of an index file. Index files store which segments have already been
 * written into a temporary file (the data itself is stored in a data file). Index files also contain metadata about
 * the download itself (size of the data file, total expected size, user generic information, etc).
 * <p/>
 * Index file are the entry point to manage the data of their corresponding data files. Read or write tasks are
 * handled by the index files, which themselves access the data files.
 */
class TempIndex implements VersionedObject {

    private final static String VERSION_0_1 = "0.1";

    private final static String CURRENT_VERSION = VERSION_0_1;

    private Path tempDataFilePath;

    private Long totalResourceSize;

    private HashMap<String, Serializable> userDictionary;

    private HashMap<String, Serializable> systemDictionary;

    /**
     * Segments of data which the data file already owns. The data outside these segments is undetermined.
     */
    private LongRangeList data;

    TempIndex(Path tempDataFilePath, HashMap<String, Serializable> userDictionary) throws IOException {
        this.tempDataFilePath = tempDataFilePath;
        this.totalResourceSize = null;
        this.userDictionary = userDictionary;
        this.systemDictionary = new HashMap<>();
        this.data = new LongRangeList();
        setupDataFile(0);
    }

    public TempIndex(String path, String backupPath, TempFileManager tempFileManager) throws VersionedSerializationException, IOException {
        List<String> repairedFiles = VersionedObjectSerializer.deserialize(this, path, true, backupPath);
        repairedFiles.forEach(tempFileManager::indexFileErrorRestoredWithBackup);
    }

    HashMap<String, Serializable> getUserDictionary() {
        return userDictionary;
    }

    HashMap<String, Serializable> getSystemDictionary() {
        return systemDictionary;
    }

    void setSystemField(String key, Serializable value) {
        systemDictionary.put(key, value);
    }

    String getTempDataFilePath() {
        return tempDataFilePath.toString();
    }

    Long getTotalResourceSize() {
        return totalResourceSize;
    }

    void setTotalSize(long size) throws IOException {
        totalResourceSize = size;
        setupDataFile(size);
    }

    LongRangeList getOwnedDataParts() {
        return new LongRangeList(data);
    }

    private void setupDataFile(long fileSize) throws IOException {
        RandomAccessFile f = new RandomAccessFile(tempDataFilePath.toFile(), "rws");
        f.setLength(fileSize);
        f.close();
    }

    byte[] read(long offset, int length) throws IOException, IndexOutOfBoundsException {
        LongRange range = generateRangeFromOffsetAndLength(offset, length);
        checkCorrectRange(range);
        if (data.contains(range)) {
            // the requested range is valid -> read the data and return it
            return RandomAccess.read(tempDataFilePath.toFile(), offset, length);
        } else {
            throw new IndexOutOfBoundsException("The requested range " + range + " is not valid for this temp file");
        }
    }

    void write(long offset, byte[] bytesToWrite) throws IOException {
        LongRange range = generateRangeFromOffsetAndLength(offset, bytesToWrite.length);
        checkCorrectRange(range);
        LongRangeList inputRangeSet = new LongRangeList(range);
        inputRangeSet.remove(data);
        RandomAccess.write(tempDataFilePath.toFile(), offset, bytesToWrite);
        data.add(range);
    }

    private void checkCorrectRange(LongRange range) throws IOException, IndexOutOfBoundsException {
        if (totalResourceSize == null) {
            throw new IOException("Resource size has not been set yet");
        }
        if (range.getMin() < 0 || range.getMax() >= totalResourceSize) {
            throw new IndexOutOfBoundsException("offset and length values out of range of the resource size");
        }
    }

    private static LongRange generateRangeFromOffsetAndLength(long offset, int length) {
        return new LongRange(offset, offset + length - 1);
    }

    @Override
    public VersionStack getCurrentVersion() {
        return new VersionStack(CURRENT_VERSION);
    }

    @Override
    public Map<String, Serializable> serialize() {
        Map<String, Serializable> map = new HashMap<>();
        map.put("tempDataFilePath", tempDataFilePath.toString());
        map.put("totalResourceSize", totalResourceSize);
        map.put("userDictionary", userDictionary);
        map.put("systemDictionary", systemDictionary);
        map.put("data", data);
        return map;
    }

    @Override
    public void deserialize(String version, Map<String, Object> attributes, VersionStack parentVersions) throws UnrecognizedVersionException {
        if (version.equals(CURRENT_VERSION)) {
            tempDataFilePath = Paths.get((String) attributes.get("tempDataFilePath"));
            totalResourceSize = (Long) attributes.get("totalResourceSize");
            userDictionary = (HashMap<String, Serializable>) attributes.get("userDictionary");
            systemDictionary = (HashMap<String, Serializable>) attributes.get("systemDictionary");
            data = (LongRangeList) attributes.get("data");
        } else {
            throw new UnrecognizedVersionException();
        }
    }
}
