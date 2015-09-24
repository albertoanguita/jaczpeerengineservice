package jacz.peerengineservice.util.datatransfer.master;

import jacz.peerengineservice.util.datatransfer.slave.ResourceChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * A queue of data stored in memory and that must be written to a resource writer. Each queue element is a
 * data segment, created from one or several connected resource chunks.
 * <p/>
 * Queue is ordered by age. The older elements are first. This way, older elements will be first written to disk,
 * avoiding starvation.
 * <p/>
 * New resource chunks can be appended to existing elements, if one adjacent element is found. This way, we reduce
 * the number of writes.
 * <p/>
 * The class is thread-safe
 */
class WriteDataBuffer {

    public static final class DataElement {

        final long timestamp;

        long firstByte;

        byte[] data;

        public DataElement(ResourceChunk resourceChunk) {
            timestamp = System.currentTimeMillis();
            this.firstByte = resourceChunk.getFirstByte();
            this.data = resourceChunk.getData();
        }

        public boolean isAdjacent(ResourceChunk resourceChunk) {
            return firstByte + data.length == resourceChunk.getFirstByte();
        }

        public void append(ResourceChunk resourceChunk) {
            byte[] append = new byte[data.length + resourceChunk.getData().length];
            System.arraycopy(data, 0, append, 0, data.length);
            System.arraycopy(resourceChunk.getData(), 0, append, data.length, resourceChunk.getData().length);
            data = append;
        }
    }

    private final List<DataElement> dataBuffer;


    public WriteDataBuffer() {
        dataBuffer = new ArrayList<>();
    }

    public boolean isEmpty() {
        return dataBuffer.isEmpty();
    }

    public synchronized void addResourceChunk(ResourceChunk resourceChunk) {
        for (DataElement dataElement : dataBuffer) {
            if (dataElement.isAdjacent(resourceChunk)) {
                dataElement.append(resourceChunk);
                return;
            }
        }
        dataBuffer.add(new DataElement(resourceChunk));
    }

    public synchronized DataElement getDataElement() {
        return dataBuffer.isEmpty() ? null : dataBuffer.remove(0);
    }
}
