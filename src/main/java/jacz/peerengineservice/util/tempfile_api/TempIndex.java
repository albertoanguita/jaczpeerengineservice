package jacz.peerengineservice.util.tempfile_api;

import jacz.util.files.RandomAccess;
import jacz.util.numeric.LongRange;
import jacz.util.numeric.RangeSet;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provides an object representation of an index file. Index files store which segments have already been
 * written into a temporary file (the data itself is stored in a data file). Index files also contain metadata about
 * the download itself (size of the data file, total expected size, user generic information, etc).
 * <p/>
 * Index file are the entry point to manage the data of their corresponding data files. Read or write tasks are
 * handled by the index files, which themselves access the data files.
 */
class TempIndex implements Serializable {

    private final String tempDataFilePath;

    private Long totalResourceSize;

    private final Map<String, Map<String, Serializable>> userGenericData;

    /**
     * Segments of data which the data file already owns. The data outside these segments is undetermined.
     */
    private final RangeSet<LongRange, Long> data;

    TempIndex(TempFileManager tempFileManager, String tempDataFile) throws IOException {
        tempDataFilePath = tempFileManager.getBaseDir() + tempDataFile;
        setupDataFile(0);
        totalResourceSize = null;
        userGenericData = new HashMap<String, Map<String, Serializable>>();
        //checkGenericUserDataNonNull();
        data = new RangeSet<LongRange, Long>();
    }

    /*private void checkGenericUserDataNonNull() {
        if (userGenericData == null) {
            userGenericData = new HashMap<String, Serializable>();
        }
    }*/

    Map<String, Serializable> getUserGenericData(String group) {
        return userGenericData.get(group);
    }

    Serializable getUserGenericDataField(String group, String key) {
        if (userGenericData.containsKey(group) && userGenericData.get(group).containsKey(key)) {
            return userGenericData.get(group).get(key);
        } else {
            return null;
        }
    }

    void removeUserGenericDataGroup(String group) {
        userGenericData.remove(group);
    }

    void setUserGenericData(String group, Map<String, Serializable> userGenericData) {
        this.userGenericData.put(group, userGenericData);
    }

    void setUserGenericDataField(String group, String key, Serializable value) {
        if (!userGenericData.containsKey(group)) {
            userGenericData.put(group, new HashMap<String, Serializable>());
        }
        if (userGenericData.get(group) == null) {
            userGenericData.put(group, new HashMap<String, Serializable>());
        }
        userGenericData.get(group).put(key, value);
    }

    String getTempDataFilePath() {
        return tempDataFilePath;
    }

    Long getTotalResourceSize() {
        return totalResourceSize;
    }

    void setTotalSize(long size) throws IOException {
        totalResourceSize = size;
        setupDataFile(size);
    }

    RangeSet<LongRange, Long> getOwnedDataParts() {
        return new RangeSet<LongRange, Long>(data);
    }

    private void setupDataFile(long fileSize) throws IOException {
        RandomAccessFile f = new RandomAccessFile(tempDataFilePath, "rws");
        f.setLength(fileSize);
        f.close();
//        RandomAccessFile rFile = new RandomAccessFile(tempFileManager.getBaseDir() + tempDataFile, "rws");
//        System.out.println("TEMP FILES SIZE SET UP!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!: " + rFile.length());
//        rFile.close();
    }

    byte[] read(long offset, int length) throws IOException, IndexOutOfBoundsException {
        LongRange range = generateRangeFromOffsetAndLength(offset, length);
        checkCorrectRange(range);
        if (data.contains(range)) {
            // the requested range is valid -> read the data and return it
            return RandomAccess.read(tempDataFilePath, offset, length);
        } else {
            throw new IndexOutOfBoundsException("The requested range " + range + " is not valid for this temp file");
        }
    }

    void write(long offset, byte[] bytesToWrite) throws IOException {
        LongRange range = generateRangeFromOffsetAndLength(offset, bytesToWrite.length);
        checkCorrectRange(range);
        RangeSet<LongRange, Long> inputRangeSet = new RangeSet<LongRange, Long>(range);
        inputRangeSet.remove(data);
        RandomAccess.write(tempDataFilePath, offset, bytesToWrite);
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
}
