package jacz.peerengineservice.util.data_synchronization.old;

import jacz.util.io.buffer.BufferStream;
import jacz.util.io.buffer.ReadBuffer;
import jacz.util.io.object_serialization.Serializer;
import jacz.util.notification.ProgressNotificationWithError;
import jacz.util.numeric.LongRange;
import jacz.util.numeric.NumericUtil;
import jacz.util.numeric.RangeSet;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceReader;

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
class ByteArrayReader implements ResourceReader {

    private class ByteArrayBufferStream implements BufferStream {

        private final ListAccessor listAccessor;

        private final int level;

        private final List<String> indexList;

        /**
         * Index pointing to the next element to read
         */
        private int index;

        private ByteArrayBufferStream(ListAccessor listAccessor, int level, List<String> indexList) {
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
            try {
                byte[] indexBytes = Serializer.serialize(indexList.get(index));
                byte[] element = listAccessor.getElementByteArray(indexList.get(index), level);
                index++;
                return Serializer.addArrays(indexBytes, Serializer.serialize(element.length), element);
            } catch (Exception e) {
                throw new IOException("could not read element from list accessor");
            }
        }
    }

    // size of the  buffer (64KB) -> good enough for most transfers
    private static final int INIT_BUFFER_LENGTH = 64 * 1024;

    private final ListAccessor listAccessor;

    private ReadBuffer readBuffer;

    private long offset;

    private final Long length;

    private ProgressNotificationWithError<Integer, SynchronizeError> progress;

    public ByteArrayReader(ListAccessor listAccessor, int level, List<String> indexList, ProgressNotificationWithError<Integer, SynchronizeError> progress) {
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
    }

    private static Long calculateLength(ListAccessor listAccessor, int level, List<String> indexList) {
        // for each element, the size of the element will be stored as a 4-byte integer
        // we also store the index of each element
        long length = 4 * indexList.size();
        for (String index : indexList) {
            try {
                length += Serializer.serialize(index).length;
                length += listAccessor.getElementByteArrayLength(index, level);
            } catch (Exception e) {
                return null;
            }
        }
        return length;
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
    public RangeSet<LongRange, Long> availableSegments() throws IOException {
        // we own the complete resource
        return new RangeSet<>(new LongRange(0l, length - 1));
    }

    @Override
    public byte[] read(long offset, int length) throws IndexOutOfBoundsException, IOException {
        if (offset != this.offset) {
            // we always expect a read at the beginning of the queue
            throw new IOException("Non sequential read not allowed, offset: " + offset);
        }
        this.offset += length;
        if (progress != null) {
            progress.addNotification((int) NumericUtil.displaceInRange(this.offset, 0, this.length, 0, ListSynchronizerManager.PROGRESS_MAX));
        }
        return readBuffer.read(length);
    }

    @Override
    public void stop() {
        if (length != null && offset == length) {
            // full resource correctly read
            listAccessor.endSynchProcess(ListAccessor.Mode.SERVER, true);
            if (progress != null) {
                progress.completeTask();
            }
        } else {
            // there was some error
            listAccessor.endSynchProcess(ListAccessor.Mode.SERVER, false);
            if (progress != null) {
                progress.error(new SynchronizeError(SynchronizeError.Type.DATA_TRANSFER_FAILED, null));
            }
        }
    }
}
