package jacz.peerengineservice.util.datatransfer.resource_accession;

import jacz.util.files.FileUtil;
import jacz.util.files.RandomAccess;
import jacz.util.numeric.LongRange;
import jacz.util.numeric.RangeSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * This class implements the ResourceReader interface in order to allow clients to easily share files (not forcing them
 * to write their own ResourceReader for files). It is based on the RandomAccessFile library.
 */
public class BasicFileReader implements ResourceReader {

    /**
     * Accessed file
     */
    File file;

    public BasicFileReader(String path) throws FileNotFoundException {
        if (!FileUtil.isFile(path)) {
            throw new FileNotFoundException();
        }
        file = new File(path);
    }

    @Override
    public boolean supportsRandomAccess() {
        return true;
    }

    @Override
    public long length() {
        return file.length();
    }

    @Override
    public RangeSet<LongRange, Long> availableSegments() {
        return new RangeSet<LongRange, Long>(new LongRange(0l, length() - 1));
    }

    @Override
    public byte[] read(long offset, int length) throws IndexOutOfBoundsException, IOException {
        return RandomAccess.read(file, offset, length);
    }

    @Override
    public void stop() {
        // ignore
    }
}
