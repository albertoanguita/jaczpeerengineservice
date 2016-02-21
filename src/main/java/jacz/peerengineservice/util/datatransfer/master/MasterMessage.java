package jacz.peerengineservice.util.datatransfer.master;

import jacz.util.io.serialization.MutableOffset;
import jacz.util.io.serialization.Serializer;
import jacz.util.numeric.range.LongRange;

/**
 * Messages created by a Master for a Slave
 */
public class MasterMessage {

    /**
     * Orders that this slave can receive from his master
     */
    public enum Order {
        // this slave must report the length of the shared resource
        REPORT_RESOURCE_LENGTH,
        // this slave must report him master about the segments he has available
        REPORT_AVAILABLE_SEGMENTS,
        // indicate the master which segments we have currently assigned
        REPORT_ASSIGNED_SEGMENTS,
        // this slave must erase all previously received segments
        ERASE_SEGMENTS,
        // this slave must add a new segment to the list of "segments to send"
        ADD_NEW_SEGMENT,
        // hardThrottle speed to slow down
        HARD_THROTTLE,
        SOFT_THROTTLE,
        // ping message to keep slave alive
        PING,
        // the master reports that he died, so we should die as well to free resources
        DIED
    }

    public final Order order;

    public final LongRange segment;

    public final Float speed;

    public final float throttle;

    public MasterMessage(byte[] data) {
        MutableOffset offset = new MutableOffset();
        order = Serializer.deserializeEnum(Order.class, data, offset);
        if (order != null && order == Order.ADD_NEW_SEGMENT) {
            long min = Serializer.deserializeLong(data, offset);
            long max = Serializer.deserializeLong(data, offset);
            segment = new LongRange(min, max);
            speed = null;
            throttle = 0;
        } else if (order != null && order == Order.HARD_THROTTLE) {
            segment = null;
            speed = null;
            throttle = Serializer.deserializeFloatValue(data, offset);
        } else {
            segment = null;
            speed = null;
            throttle = 0;
        }
    }

    public static byte[] generateReportResourceLengthMessage() {
        return Serializer.serialize(Order.REPORT_RESOURCE_LENGTH);
    }

    public static byte[] generateReportAvailableSegmentsMessage() {
        return Serializer.serialize(Order.REPORT_AVAILABLE_SEGMENTS);
    }

    public static byte[] generateReportAssignedSegmentsMessage() {
        return Serializer.serialize(Order.REPORT_ASSIGNED_SEGMENTS);
    }

    public static byte[] generateEraseSegmentsMessage() {
        return Serializer.serialize(Order.ERASE_SEGMENTS);
    }

    public static byte[] generateAddNewSegmentsMessage(LongRange segment) {
        byte[] order = Serializer.serialize(Order.ADD_NEW_SEGMENT);
        return Serializer.addArrays(order, Serializer.serialize(segment.getMin()), Serializer.serialize(segment.getMax()));
//        return FragmentedByteArray.addArraysFinal(order, Serializer.serialize(segment.getMin()), Serializer.serialize(segment.getMax()));
    }

    public static byte[] generateHardThrottleMessage(float variation) {
        byte[] order = Serializer.serialize(Order.HARD_THROTTLE);
        return Serializer.addArrays(order, Serializer.serialize(variation));
    }

    public static byte[] generateSoftThrottleMessage() {
        return  Serializer.serialize(Order.SOFT_THROTTLE);
    }

    public static byte[] generatePingMessage() {
        return Serializer.serialize(Order.PING);
    }

    public static byte[] generateDiedMessage() {
        return Serializer.serialize(Order.DIED);
    }
}
