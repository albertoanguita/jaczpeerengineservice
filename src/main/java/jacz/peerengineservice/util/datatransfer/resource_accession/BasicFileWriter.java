package jacz.peerengineservice.util.datatransfer.resource_accession;

import org.aanguita.jacuzzi.files.FileGenerator;
import org.aanguita.jacuzzi.files.RandomAccess;
import org.aanguita.jacuzzi.numeric.range.LongRangeList;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 *
 */
public class BasicFileWriter extends SingleSessionResourceWriter {

    private final String finalPath;

    private final File file;

    private boolean hasFailed;

    public BasicFileWriter(String expectedFilePath) throws IOException {
        this(new File(expectedFilePath).getParent(), Paths.get(expectedFilePath).getFileName().toString());
    }

    public BasicFileWriter(String expectedFilePath, HashMap<String, Serializable> userDictionary) throws IOException {
        this(new File(expectedFilePath).getParent(), Paths.get(expectedFilePath).getFileName().toString(), userDictionary);
    }

    public BasicFileWriter(String downloadDir, String expectedFileName) throws IOException {
        this(downloadDir, expectedFileName, new HashMap<>());
    }

    public BasicFileWriter(String downloadDir, String expectedFileName, HashMap<String, Serializable> userDictionary) throws IOException {
        super(userDictionary);
        hasFailed = false;
        String fileWithoutExtension = FilenameUtils.getBaseName(expectedFileName);
        String extension = FilenameUtils.getExtension(expectedFileName);
        finalPath = FileGenerator.createFile(downloadDir, fileWithoutExtension, extension, " (", ")", true);
        file = new File(finalPath);
        if (!file.isFile()) {
            try {
                hasFailed = !file.createNewFile();
            } catch (IOException e) {
                hasFailed = true;
            }
        }
    }

    @Override
    public Long getSize() {
        // size is never known in the beginning
        return null;
    }

    @Override
    public LongRangeList getAvailableSegments() {
        // no share in the beginning
        return null;
    }

    @Override
    public void init(long size) throws IOException {
        checkHasFailed();
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rws");
        randomAccessFile.setLength(size);
        randomAccessFile.close();
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
            Files.delete(Paths.get(finalPath));
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
