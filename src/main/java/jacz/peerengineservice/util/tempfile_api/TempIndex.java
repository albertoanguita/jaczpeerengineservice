package jacz.peerengineservice.util.tempfile_api;

import jacz.util.files.RandomAccess;
import jacz.util.io.object_serialization.UnrecognizedVersionException;
import jacz.util.io.object_serialization.VersionedObject;
import jacz.util.io.object_serialization.VersionedObjectSerializer;
import jacz.util.io.object_serialization.VersionedSerializationException;
import jacz.util.numeric.range.LongRange;
import jacz.util.numeric.range.LongRangeList;

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
class TempIndex implements VersionedObject {

    private final static String VERSION_0_1 = "0.1";

    private final static String CURRENT_VERSION = VERSION_0_1;

    private String tempDataFilePath;

    private Long totalResourceSize;

    private HashMap<String, Map<String, Serializable>> customData;

    /**
     * Segments of data which the data file already owns. The data outside these segments is undetermined.
     */
    private LongRangeList data;

    TempIndex(String tempDataFilePath) throws IOException {
        this(tempDataFilePath, null, new HashMap<String, Map<String, Serializable>>(), new LongRangeList());
        setupDataFile(0);
    }

    private TempIndex(String tempDataFilePath, Long totalResourceSize, HashMap<String, Map<String, Serializable>> customData, LongRangeList data) {
        this.tempDataFilePath = tempDataFilePath;
        this.totalResourceSize = totalResourceSize;
        this.customData = customData;
        this.data = data;
    }

    public TempIndex(byte[] data) throws VersionedSerializationException {
        VersionedObjectSerializer.deserialize(this, data);
    }

//    public void saveToXML(String path) throws IOException, XMLStreamException {
//        XMLWriter xmlWriter = new XMLWriter("index-file");
//        xmlWriter.addField("VERSION", VERSION_0_1);
//        xmlWriter.addField("tempDataFilePath", tempDataFilePath);
//        xmlWriter.addField("totalResourceSize", totalResourceSize);
//        xmlWriter.beginStruct("customData");
//        for (Map.Entry<String, Map<String, Serializable>> customGroup : customData.entrySet()) {
//            xmlWriter.beginStruct(customGroup.getKey());
//            for (Map.Entry<String, Serializable> entry : customGroup.getValue().entrySet()) {
//                xmlWriter.addField(entry.getKey(), entry.getValue());
//            }
//            xmlWriter.endStruct();
//        }
//        xmlWriter.endStruct();
//        xmlWriter.beginStruct("data");
//        for (LongRange longRange : data.getRanges()) {
//            xmlWriter.beginStruct();
//            xmlWriter.addValue(longRange.getMin());
//            xmlWriter.addValue(longRange.getMax());
//            xmlWriter.endStruct();
//        }
//        xmlWriter.endStruct();
//        xmlWriter.write(path);
//    }

//    public static TempIndex readFromXML(String path) throws FileNotFoundException, XMLStreamException, TempIndexVersionException {
//        XMLReader xmlReader = new XMLReader(path);
//        String VERSION = xmlReader.getFieldValue("VERSION");
//        if (!VERSION.equals(TempIndex.VERSION_0_1)) {
//            // wrong version detected
//            throw new TempIndexVersionException(VERSION);
//        }
//        String tempDataFilePath = xmlReader.getFieldValue("tempDataFilePath");
//        Long totalResourceSize = StrCast.asLong(xmlReader.getFieldValue("totalResourceSize"));
//        Map<String, Map<String, Serializable>> customData = new HashMap<>();
//        xmlReader.getStruct("customData");
//        while (xmlReader.hasMoreChildren()) {
//            String groupName = xmlReader.getNextStructAndName();
//            Map<String, Serializable> customGroup = new HashMap<>();
//            while (xmlReader.hasMoreChildren()) {
//                Duple<String, String> fieldAndValue = xmlReader.getNextFieldAndValue();
//                customGroup.put(fieldAndValue.element1, fieldAndValue.element2);
//            }
//            customData.put(groupName, customGroup);
//        }
//        RangeSet<LongRange, Long> data = new RangeSet<>();
//        xmlReader.getStruct("data");
//        while (xmlReader.hasMoreChildren()) {
//            xmlReader.getNextStruct();
//            Long min = StrCast.asLong(xmlReader.getNextValue());
//            Long max = StrCast.asLong(xmlReader.getNextValue());
//            data.add(new LongRange(min, max));
//            xmlReader.gotoParent();
//        }
//        return new TempIndex(tempDataFilePath, totalResourceSize, customData, data);
//    }

    Map<String, Serializable> getCustomGroup(String groupName) {
        return customData.get(groupName);
    }

    Serializable getCustomGroupField(String groupName, String key) {
        if (customData.containsKey(groupName)) {
            return customData.get(groupName).get(key);
        } else {
            return null;
        }
    }

    void setCustomGroup(String groupName, Map<String, Serializable> group) {
        this.customData.put(groupName, group);
    }

    void setCustomGroupField(String groupName, String key, Serializable value) {
        if (!customData.containsKey(groupName)) {
            customData.put(groupName, new HashMap<String, Serializable>());
        }
        customData.get(groupName).put(key, value);
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

    LongRangeList getOwnedDataParts() {
        return new LongRangeList(data);
    }

    private void setupDataFile(long fileSize) throws IOException {
        RandomAccessFile f = new RandomAccessFile(tempDataFilePath, "rws");
        f.setLength(fileSize);
        f.close();
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
        LongRangeList inputRangeSet = new LongRangeList(range);
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

    @Override
    public String getCurrentVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public Map<String, Serializable> serialize() {
        Map<String, Serializable> map = new HashMap<>();
        map.put("tempDataFilePath", tempDataFilePath);
        map.put("totalResourceSize", totalResourceSize);
        map.put("customData", customData);
        map.put("data", data);
        return map;
    }

    @Override
    public void deserialize(Map<String, Object> attributes) {
        tempDataFilePath = (String) attributes.get("tempDataFilePath");
        totalResourceSize = (Long) attributes.get("totalResourceSize");
        customData = (HashMap<String, Map<String, Serializable>>) attributes.get("customData");
        data = (LongRangeList) attributes.get("data");
    }

    @Override
    public void deserializeOldVersion(String version, Map<String, Object> attributes) throws UnrecognizedVersionException {
        throw new UnrecognizedVersionException();
    }
}
