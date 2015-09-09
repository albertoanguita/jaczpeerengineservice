package jacz.peerengine.util.datatransfer.master;

import jacz.util.io.object_serialization.MutableOffset;
import jacz.util.io.object_serialization.Serializer;
import jacz.util.numeric.LongRange;

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
        // set the maximum (and recommended) speed of transmission
        SET_SPEED,
        // ping message to keep slave alive
        PING,
        // the master reports that he died, so we should die as well to free resources
        DIED
    }

    public final Order order;

    public final LongRange segment;

    public final Float speed;

    public MasterMessage(byte[] data) {
        MutableOffset offset = new MutableOffset();
        order = Serializer.deserializeEnum(Order.class, data, offset);
        if (order != null && order == Order.ADD_NEW_SEGMENT) {
            long min = Serializer.deserializeLong(data, offset);
            long max = Serializer.deserializeLong(data, offset);
            segment = new LongRange(min, max);
            speed = null;
        } else if (order != null && order == Order.SET_SPEED) {
            segment = null;
            speed = Serializer.deserializeFloat(data, offset);
        } else {
            segment = null;
            speed = null;
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
    }

    public static byte[] generateSetSpeedMessage(Float speed) {
        byte[] order = Serializer.serialize(Order.SET_SPEED);
        return Serializer.addArrays(order, Serializer.serialize(speed));
    }

    public static byte[] generatePingMessage() {
        return Serializer.serialize(Order.PING);
    }

    public static byte[] generateDiedMessage() {
        return Serializer.serialize(Order.DIED);
    }
}
