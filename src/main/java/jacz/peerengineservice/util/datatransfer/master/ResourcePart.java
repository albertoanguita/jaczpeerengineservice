package jacz.peerengineservice.util.datatransfer.master;

import jacz.util.numeric.LongRange;
import jacz.util.numeric.NumericUtil;
import jacz.util.numeric.RangeSet;

import java.util.Collection;
import java.util.List;

/**
 * Class description
 * <p/>
 * User: Alberto<br>
 * Date: 19-mar-2011<br>
 * Last Modified: 19-mar-2011
 */
public class ResourcePart extends RangeSet<LongRange, Long> {

    private int segmentSearchIndex;

    private int accumulatedOffset;


    public ResourcePart() {
        super();
        resetOffsetCache();
    }

    public ResourcePart(LongRange initialRange) {
        super(initialRange);
        resetOffsetCache();
    }

    public ResourcePart(Collection<LongRange> ranges) {
        super(ranges);
        resetOffsetCache();
    }

    public ResourcePart(RangeSet<LongRange, Long> rangeSet) {
        super(rangeSet);
        resetOffsetCache();
    }

    @Override
    public void add(LongRange range) {
        super.add(range);
        resetOffsetCache();
    }

    @Override
    public void add(Collection<LongRange> ranges) {
        super.add(ranges);
        resetOffsetCache();
    }

    @Override
    public void add(LongRange... ranges) {
        super.add(ranges);
        resetOffsetCache();
    }

    @Override
    public void remove(LongRange range) {
        super.remove(range);
        resetOffsetCache();
    }

    @Override
    public void remove(List<LongRange> ranges) {
        super.remove(ranges);
        resetOffsetCache();
    }

    @Override
    public void remove(LongRange... ranges) {
        super.remove(ranges);
        resetOffsetCache();
    }

    private void resetOffsetCache() {
        segmentSearchIndex = 0;
        accumulatedOffset = 0;
    }

    /**
     * Retrieves the absolute position in this resource part according to an offset
     *
     * @param offset the offset to measure the absolute position
     * @return the absolute position that the offset means in this resource part
     */
    public Long getPosition(long offset) {
        if (segmentSearchIndex >= getRanges().size() || offset < accumulatedOffset) {
            resetOffsetCache();
        }
        offset -= accumulatedOffset;
        while (segmentSearchIndex < getRanges().size() && offset >= getRanges().get(segmentSearchIndex).size()) {
            offset -= getRanges().get(segmentSearchIndex).size();
            accumulatedOffset += getRanges().get(segmentSearchIndex).size();
            segmentSearchIndex++;
        }
        if (segmentSearchIndex < getRanges().size()) {
            // range found at index
            return getRanges().get(segmentSearchIndex).getMin() + offset;
        } else {
            return getRanges().get(getRanges().size() - 1).getMax();
        }
    }


    public ResourcePart intersection(ResourcePart anotherResourcePart) {
        return new ResourcePart(super.intersection(anotherResourcePart));
    }

    public LongRange getSegmentAroundPosition(long position, long preferredSize) {
        int index = searchRange(position);
        if (index >= 0) {
            LongRange range = getRanges().get(index);
            if (range.size() <= preferredSize) {
                return range;
            } else {
                // this range is larger than the preferredSize, we sure can fit it. We check if at any side we don't
                // have enough space for fitting a range right in the middle
                // the byte at position is taken by the right half
                List<Long> halves = NumericUtil.divide(preferredSize, 2);
                if (range.getMin() > position - halves.get(0) + 1) {
                    // no space at the left
                    return new LongRange(range.getMin(), range.getMin() + preferredSize - 1);
                } else if (range.getMax() < position + halves.get(1)) {
                    // no space at the right
                    return new LongRange(range.getMax() - preferredSize + 1, range.getMax());
                } else {
                    // space at both sides
                    return new LongRange(position - halves.get(0) + 1, position + halves.get(1));
                }
            }
        } else {
            return null;
        }
    }


}
