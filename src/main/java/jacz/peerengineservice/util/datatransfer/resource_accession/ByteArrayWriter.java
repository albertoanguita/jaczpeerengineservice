package jacz.peerengineservice.util.datatransfer.resource_accession;

import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.util.io.object_serialization.MutableOffset;
import jacz.util.io.object_serialization.Serializer;
import jacz.util.numeric.range.LongRangeList;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

/**
 * Resource writer implementation for byte-array transmissions in list synchronization
 */
public class ByteArrayWriter extends SingleSessionResourceWriter {

    private enum State {
        INIT,
        INDEX_READ,
        SIZE_READ
    }

    private final DataAccessor listAccessor;

    private final int level;

    private byte[] tempArray;

    private final MutableOffset mutableOffset;

    private State state;

    private String index;

    private int size;

    public ByteArrayWriter(DataAccessor listAccessor, int level) {
        this(listAccessor, level, new HashMap<String, Serializable>());
    }

    public ByteArrayWriter(DataAccessor listAccessor, int level, HashMap<String, Serializable> userDictionary) {
        super(userDictionary);
        this.listAccessor = listAccessor;
        this.level = level;
        tempArray = new byte[0];
        mutableOffset = new MutableOffset();
        state = State.INIT;
    }

    @Override
    public Long getSize() throws IOException {
        // not known
        return null;
    }

    @Override
    public LongRangeList getAvailableSegments() throws IOException {
        // no ranges owned
        return null;
    }

    @Override
    public void init(long size) throws IOException {
        // ignore
    }

    @Override
    public void write(long offset, byte[] data) throws IOException, IndexOutOfBoundsException {
        // writes are always sequential, no need to check this
        // the received data is added to tempArray. Then we try to read as many elements as possible from tempArray
        tempArray = Serializer.addArrays(tempArray, data);
        while (readElement(mutableOffset)) {
        }
    }

    /**
     * Reads one byte-array element from the incoming buffer. Each element comes in two parts: the first part is a serialized integer, which indicates
     * the size of the actual byte-array element coming. The second part is the actual byte-array
     * @param mutableOffset
     * @return
     * @throws IOException
     */
    private boolean readElement(MutableOffset mutableOffset) throws IOException {
        /*try {
            switch (state) {

                case INIT:
                    index = Serializer.deserializeString(tempArray, mutableOffset);
                    state = State.INDEX_READ;
                    return true;

                case INDEX_READ:
                    size = Serializer.deserializeInt(tempArray, mutableOffset);
                    state = State.SIZE_READ;
                    return true;

                case SIZE_READ:
                    // extract the portion of tempArray holding the byte-array element and copy it to 'data'.
                    // In this case, we must modify the mutableOffset manually

                    // check that there are enough bytes in tempArray
                    if (tempArray.length < mutableOffset.value() + size) {
                        return false;
                    }

                    byte[] data = Arrays.copyOfRange(tempArray, mutableOffset.value(), mutableOffset.value() + size);
                    mutableOffset.add(size);
                    // the extracted element is added to the list
                    try {
                        listAccessor.addElementAsByteArray(index, level, data);
                    } catch (DataAccessException e) {
                        throw new IOException("Data access error in the list accessor implementation");
                    }
                    state = State.INIT;
                    return true;

                default:
                    // cannot happen
                    return true;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // this happens when tempArray did not have enough size to perform the operation -> return false indicating that no more elements can be
            // read currently
            return false;
        }*/
        return false;
    }

    @Override
    public void complete() throws IOException {
        // ignore
    }

    @Override
    public void cancel() {
        // ignore
    }

    @Override
    public void stop() {
        // ignore
    }

    @Override
    public String getPath() {
        // ignore
        return null;
    }
}
