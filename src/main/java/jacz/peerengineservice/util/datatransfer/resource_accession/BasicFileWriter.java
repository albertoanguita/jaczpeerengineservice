package jacz.peerengineservice.util.datatransfer.resource_accession;

import jacz.util.files.FileUtil;
import jacz.util.files.RandomAccess;
import jacz.util.numeric.LongRange;
import jacz.util.numeric.RangeSet;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class BasicFileWriter implements ResourceWriter {

    private final String finalPath;

    private final File file;

    private boolean hasFailed;

    private Map<String, Map<String, Serializable>> userGenericData;

    public BasicFileWriter(String expectedFilePath) throws IOException {
        this(FileUtil.getFileDirectory(expectedFilePath), FileUtil.getFileName(expectedFilePath));
    }

    public BasicFileWriter(String downloadDir, String expectedFileName) throws IOException {
        hasFailed = false;
        String fileWithoutExtension = FileUtil.getFileNameWithoutExtension(expectedFileName);
        String extension = FileUtil.getFileExtension(expectedFileName);
        finalPath = FileUtil.createFile(downloadDir, fileWithoutExtension, extension, " (", ")", true).element1;
        file = new File(finalPath);
        if (!file.isFile()) {
            try {
                hasFailed = !file.createNewFile();
            } catch (IOException e) {
                hasFailed = true;
            }
        }
        userGenericData = new HashMap<>();
    }

    @Override
    public Long getSize() {
        // size is never known in the beginning
        return null;
    }

    @Override
    public RangeSet<LongRange, Long> getAvailableSegments() {
        // no share in the beginning
        return null;
    }

    @Override
    public void setCustomGroup(String group, Map<String, Serializable> userGenericData) {
        this.userGenericData.put(group, userGenericData);
    }

    @Override
    public Map<String, Serializable> getCustomGroup(String group) {
        return userGenericData.get(group);
    }

    @Override
    public Serializable getCustomGroupField(String group, String key) {
        if (!userGenericData.containsKey(group)) {
            setCustomGroup(group, new HashMap<String, Serializable>());
        }
        return userGenericData.get(group).get(key);
    }

    @Override
    public void init(long size) throws IOException {
        checkHasFailed();
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rws");
        randomAccessFile.setLength(size);
        randomAccessFile.close();
    }

    @Override
    public void setCustomGroupField(String group, String key, Serializable value) {
        if (!userGenericData.containsKey(group)) {
            setCustomGroup(group, new HashMap<String, Serializable>());
        }
        userGenericData.get(group).put(key, value);
    }

    @Override
    public void write(long offset, byte[] data) throws IOException, IndexOutOfBoundsException {
        checkHasFailed();
        RandomAccess.write(file, offset, data);
    }

    @Override
    public void complete() throws IOException {
    }

    @Override
    public void cancel() {
        try {
            FileUtil.deleteFile(finalPath);
        } catch (IOException e) {
            // ignore, do not throw this exception, it is not so important not being able to delete the meta file
        }
    }

    @Override
    public void stop() {
        // ignore, no backup is done
        cancel();
    }

    private void checkHasFailed() throws IOException {
        if (hasFailed) {
            throw new IOException("Writing of the file failed");
        }
    }

    public String getPath() {
        return finalPath;
    }
}
