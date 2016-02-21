package jacz.peerengineservice.util.datatransfer.slave;

import jacz.util.io.serialization.MutableOffset;
import jacz.util.io.serialization.Serializer;
import jacz.util.numeric.range.LongRange;

/**
 * This class is able to store a chunk of file (a continuous set of bytes of the file). For that effect, the initial
 * byte, and the bytes themselves are stored in objects of this class.
 */
public class ResourceChunk {

    private final long firstByte;

    private final byte[] data;

    private ResourceChunk(long firstByte, byte[] data) {
        this.firstByte = firstByte;
        this.data = data;
    }

    public ResourceChunk(byte[] serializedChunk, MutableOffset mutableOffset) {
        firstByte = Serializer.deserializeLongValue(serializedChunk, mutableOffset);
        data = Serializer.deserializeRest(serializedChunk, mutableOffset);
    }

    public static ResourceChunk generateDataChunk(long firstByte, byte[] data) {
        return new ResourceChunk(firstByte, data);
    }

    public long getFirstByte() {
        return firstByte;
    }

    public LongRange getSegment() {
        return new LongRange(firstByte, firstByte + data.length - 1);
    }

    public byte[] getData() {
        return data;
    }

    public byte[] serialize() {
        byte[] firstByteArray = Serializer.serialize(firstByte);
        return Serializer.addArrays(firstByteArray, data);
    }

    @Override
    public String toString() {
        String str = (new LongRange(firstByte, (firstByte + data.length - 1))).toString();
        return "Chunk: " + str;
    }
}
