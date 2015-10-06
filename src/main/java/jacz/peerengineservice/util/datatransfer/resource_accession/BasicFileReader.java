package jacz.peerengineservice.util.datatransfer.resource_accession;

import jacz.util.concurrency.ThreadUtil;
import jacz.util.files.FileUtil;
import jacz.util.files.RandomAccess;
import jacz.util.io.object_serialization.Serializer;
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
    private final File file;

    private final DataCache dataCache;

    public BasicFileReader(String path) throws FileNotFoundException {
        if (!FileUtil.isFile(path)) {
            throw new FileNotFoundException();
        }
        file = new File(path);
        dataCache = new DataCache();
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
        return new RangeSet<>(new LongRange(0l, length() - 1));
    }

    @Override
    public byte[] read(long offset, int length) throws IndexOutOfBoundsException, IOException {
        // todo does not work, download stops at 99,8%
        // try to get the data from the memory cache
        byte[] data = new byte[0];
        while (data.length < length) {
            data = Serializer.addArrays(data, readAux(offset + data.length, length - data.length));
        }
        ThreadUtil.safeSleep(100);
        return data;
    }

    private byte[] readAux(long offset, int length) throws IndexOutOfBoundsException, IOException {
        if (dataCache.isDataStoredFrom(offset)) {
            // read and return the data fetched from the cache
            return dataCache.readData(new LongRange(offset, offset + length - 1));
        } else {
            // no valid data in the cache
            // read up to 10 times the requested length and store in cache
            LongRange rangeToCache = new LongRange(offset, Math.min(offset + 10 * length, length() - 1));
            byte[] data = RandomAccess.read(file, rangeToCache.getMin(), rangeToCache.size().intValue());
            dataCache.bufferData(rangeToCache, data);
            return readAux(offset, length);
        }
    }

    @Override
    public void stop() {
        // ignore
    }
}
