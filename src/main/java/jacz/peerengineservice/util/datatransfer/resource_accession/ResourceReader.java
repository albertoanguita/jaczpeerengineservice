package jacz.peerengineservice.util.datatransfer.resource_accession;

import org.aanguita.jacuzzi.numeric.range.LongRangeList;

import java.io.IOException;

/**
 * This interface contains methods to access the data of a shared resource. The client must provide with
 * implementations of this interface so the DataStreamingManager can access the required resource and transfer it to
 * other peers. Using this interface allows to share different types of resources, not only files
 */
public interface ResourceReader {

    /**
     * Whether this resource reader supports random access or not
     * todo use (@FUTURE@)
     *
     * @return true if random access is supported
     */
    boolean supportsRandomAccess();

    /**
     * Retrieves the length in bytes of the resource
     *
     * @return the length in bytes of the resource
     * @throws IOException error accessing the resource
     */public long length() throws IOException;

    /**
     * Returns the resource segments that this resource reader can access
     *
     * @return a range set with long ranges representing the resource segments that this resource reader can effectively read
     * @throws IOException error accessing the resource
     */
    LongRangeList availableSegments() throws IOException;

    /**
     * Reads an array of bytes from the resource
     *
     * @param offset the initial byte to read
     * @param length the length of the array to extract (number of bytes)
     * @return the read array
     * @throws IndexOutOfBoundsException if an incorrect offset is given, or the offset + length surpasses the resource length
     * @throws IOException               the read process could not conclude due to some IO error (usually if working with files)
     */
    byte[] read(long offset, int length) throws IndexOutOfBoundsException, IOException;

    /**
     * Reading from this reader has concluded
     */
    void stop();
}
