package jacz.peerengineservice.util.datatransfer.resource_accession;

import jacz.peerengineservice.util.tempfile_api.TempFileManager;
import jacz.util.numeric.range.LongRangeList;
import jacz.util.numeric.range.RangeList;

import java.io.IOException;

/**
 * A resource reader implementation for temporary files
 */
public class TempFileReader implements ResourceReader {

    private final TempFileManager tempFileManager;

    private final String tempFile;

    public TempFileReader(TempFileManager tempFileManager, String tempFile) {
        this.tempFileManager = tempFileManager;
        this.tempFile = tempFile;
    }


    @Override
    public boolean supportsRandomAccess() {
        return true;
    }

    @Override
    public long length() throws IOException {
        return tempFileManager.getTemporaryResourceSize(tempFile);
    }

    @Override
    public LongRangeList availableSegments() throws IOException {
        return tempFileManager.getTemporaryOwnedParts(tempFile);
    }

    @Override
    public byte[] read(long offset, int length) throws IndexOutOfBoundsException, IOException {
        return tempFileManager.read(tempFile, offset, length);
    }

    @Override
    public void stop() {
        // ignore
    }
}
