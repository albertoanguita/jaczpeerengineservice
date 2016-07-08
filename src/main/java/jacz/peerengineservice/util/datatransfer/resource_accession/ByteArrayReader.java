package jacz.peerengineservice.util.datatransfer.resource_accession;

import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import org.aanguita.jacuzzi.io.buffer.BufferStream;
import org.aanguita.jacuzzi.io.buffer.ReadBuffer;
import org.aanguita.jacuzzi.notification.ProgressNotificationWithError;
import org.aanguita.jacuzzi.numeric.range.LongRange;
import org.aanguita.jacuzzi.numeric.range.LongRangeList;

import java.io.IOException;
import java.util.List;

/**
 * A resource reader implementation for byte-array based lists
 * <p/>
 * The data belongs to a selection of elements of a list which are accessed as byte arrays. The simplest way would be
 * storing in memory all needed byte arrays into a single big byte array, but that could require too much memory.
 * Instead, the actual data will be retrieved dynamically. As the read requests arrive, we will retrieve elements from
 * the list accessor. Overdue data will be put in a temporary byte array which will be used in the next read request.
 * <p/>
 * A full initial read is though necessary in order to implement the length() method.
 */
public class ByteArrayReader implements ResourceReader {

    private class ByteArrayBufferStream implements BufferStream {

        private final DataAccessor listAccessor;

        private final int level;

        private final List<String> indexList;

        /**
         * Index pointing to the next element to read
         */
        private int index;

        private ByteArrayBufferStream(DataAccessor listAccessor, int level, List<String> indexList) {
            this.listAccessor = listAccessor;
            this.level = level;
            this.indexList = indexList;
            index = 0;
        }

        @Override
        public boolean hasMoreBytes() {
            return index < indexList.size();
        }

        @Override
        public byte[] readNextBytes() throws IOException {
            // the byte[] of one element is composed by {index, length, element}
            /*try {
                byte[] indexBytes = Serializer.serialize(indexList.get(index));
                byte[] element = listAccessor.getElementByteArray(indexList.get(index), level);
                index++;
                return Serializer.addArrays(indexBytes, Serializer.serialize(element.length), element);
            } catch (Exception e) {
                throw new IOException("could not read element from list accessor");
            }*/
            return null;
        }
    }

    // size of the  buffer (64KB) -> good enough for most transfers
    private static final int INIT_BUFFER_LENGTH = 64 * 1024;

    private final DataAccessor listAccessor;

    private ReadBuffer readBuffer;

    private long offset;

    private final Long length;

    private ProgressNotificationWithError<Integer, SynchError> progress;

    public ByteArrayReader(DataAccessor listAccessor, int level, List<String> indexList, ProgressNotificationWithError<Integer, SynchError> progress) {
        this.listAccessor = listAccessor;
        try {
            readBuffer = new ReadBuffer(INIT_BUFFER_LENGTH, new ByteArrayBufferStream(listAccessor, level, indexList));
        } catch (IOException e) {
            // ignore this exception -> length will not be calculated and that will notify the error
            readBuffer = null;
        }
        offset = 0;
        length = calculateLength(listAccessor, level, indexList);
        this.progress = progress;
        if (progress != null) {
            progress.beginTask();
        }
    }

    private static Long calculateLength(DataAccessor listAccessor, int level, List<String> indexList) {
        // for each element, the size of the element will be stored as a 4-byte integer
        // we also store the index of each element
        /*long length = 4 * indexList.size();
        for (String index : indexList) {
            try {
                length += Serializer.serialize(index).length;
                length += listAccessor.getElementByteArrayLength(index, level);
            } catch (Exception e) {
                return null;
            }
        }
        return length;*/
        return 0L;
    }

    @Override
    public boolean supportsRandomAccess() {
        return false;
    }

    @Override
    public long length() throws IOException {
        if (length != null) {
            return length;
        } else {
            throw new IOException("Length could not be correctly calculated");
        }
    }

    @Override
    public LongRangeList availableSegments() throws IOException {
        // we own the complete resource
        return new LongRangeList(new LongRange(0l, length - 1));
    }

    @Override
    public byte[] read(long offset, int length) throws IndexOutOfBoundsException, IOException {
        if (offset != this.offset) {
            // we always expect a read at the beginning of the queue
            throw new IOException("Non sequential read not allowed, offset: " + offset);
        }
        this.offset += length;
        if (progress != null) {
//            progress.addNotification((int) NumericUtil.displaceInRange(this.offset, 0, this.length, 0, ListSynchronizerManager.PROGRESS_MAX));
        }
        return readBuffer.read(length);
    }

    @Override
    public void stop() {
        if (length != null && offset == length) {
            // full resource correctly read
            listAccessor.endSynchProcess(DataAccessor.Mode.SERVER, true);
            if (progress != null) {
                progress.completeTask();
            }
        } else {
            // there was some error
            listAccessor.endSynchProcess(DataAccessor.Mode.SERVER, false);
            if (progress != null) {
//                progress.error(new SynchError(SynchError.Type.DATA_TRANSFER_FAILED, null));
            }
        }
    }
}
