package jacz.peerengineservice.util.datatransfer.resource_accession;

import jacz.util.numeric.range.LongRange;
import jacz.util.numeric.range.Range;

import java.util.Arrays;

/**
 * This class implements a simple, flexible data cache that can be used by resource readers to optimize operations
 */
class DataCache {

    private LongRange dataSegment;

    private byte[] data;

    private int offset;

    public DataCache() {
        clearDataSegment();
    }

    private void clearDataSegment() {
        dataSegment = new LongRange(0L, -1L);
    }

    void bufferData(LongRange dataSegment, byte[] data) {
        this.dataSegment = dataSegment;
        this.data = data;
        offset = 0;
    }

    boolean isDataStoredFrom(long from) {
        return !dataSegment.isEmpty() && dataSegment.getMin() == from;
    }

    byte[] readData(LongRange requestedDataSegment) {
        if (dataSegment.isEmpty()) {
            return null;
        }
        Range.RangeComparison comparison = dataSegment.compareTo(requestedDataSegment);
        if (comparison == Range.RangeComparison.EQUALS || (comparison == Range.RangeComparison.INSIDE && dataSegment.getMin().equals(requestedDataSegment.getMin()))) {
            clearDataSegment();
            return Arrays.copyOfRange(data, offset, data.length);
        } else if (comparison == Range.RangeComparison.CONTAINS && dataSegment.getMin().equals(requestedDataSegment.getMin())) {
            dataSegment = new LongRange(dataSegment.getMin() + requestedDataSegment.size(), dataSegment.getMax());
            byte[] dataToReturn = Arrays.copyOfRange(data, offset, (int) (offset + requestedDataSegment.size()));
            offset += requestedDataSegment.size();
            return dataToReturn;
        } else {
            // invalidate existing data and return null indicating that we do not have the requested data
            clearDataSegment();
            return null;
        }
    }
}
