package jacz.peerengineservice.util.datatransfer.resource_accession;

import jacz.util.io.object_serialization.Serializer;
import jacz.util.numeric.LongRange;
import jacz.util.numeric.RangeSet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

/**
 * A normal file reader with some additional bytes that go before the actual data from the resource.
 * <p/>
 * This is useful if we want to create a resource reader from a file and add some metadata to the file
 */
public class BasicFileReaderWithPrologData implements ResourceReader {

    private byte[] prolog;

    private ResourceReader resourceReader;

    public BasicFileReaderWithPrologData(byte[] prolog, ResourceReader resourceReader) throws FileNotFoundException {
        if (prolog == null) {
            this.prolog = new byte[0];
        } else {
            this.prolog = prolog;
        }
        this.resourceReader = resourceReader;
    }

    @Override
    public boolean supportsRandomAccess() {
        return true;
    }

    @Override
    public long length() throws IOException {
        return prolog.length + resourceReader.length();
    }

    @Override
    public RangeSet<LongRange, Long> availableSegments() throws IOException {
        return resourceReader.availableSegments();
    }

    @Override
    public byte[] read(long offset, int length) throws IndexOutOfBoundsException, IOException {
        if (offset + length <= prolog.length) {
            // only touches the prolog
            return Arrays.copyOfRange(prolog, (int) offset, (int) (offset + length));
        } else if (offset >= prolog.length) {
            // only touches file
            long fileOffset = offset - prolog.length;
            return resourceReader.read(fileOffset, length);
        } else {
            // touches both
            byte[] prologData = Arrays.copyOfRange(prolog, (int) offset, prolog.length);
            byte[] fileData = resourceReader.read(0, (int) (length - (prolog.length - offset)));
            return Serializer.addArrays(prologData, fileData);
        }
    }

    @Override
    public void stop() {
        resourceReader.stop();
    }
}
