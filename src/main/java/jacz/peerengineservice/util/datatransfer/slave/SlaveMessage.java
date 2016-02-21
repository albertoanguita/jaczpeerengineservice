package jacz.peerengineservice.util.datatransfer.slave;

import jacz.peerengineservice.util.datatransfer.master.ResourcePart;
import jacz.util.io.serialization.MutableOffset;
import jacz.util.io.serialization.Serializer;
import jacz.util.numeric.range.LongRange;
import jacz.util.numeric.range.Range;

/**
 * Internal representation of a message received from a slave. It also contains static methods to create
 * such slaves messages (which are always byte arrays
 */
public class SlaveMessage {

    /**
     * Types of messages that the master resource streamer can receive from slaves
     */
    public enum MessageType {
        // data of the resource
        RESOURCE_CHUNK,
        // report of the resource size
        RESOURCE_SIZE_REPORT,
        // report of the resource segment availability
        SEGMENT_AVAILABILITY_REPORT,
        // report of the resource segment assignation
        SEGMENT_ASSIGNATION_REPORT,
        // a slave informs that he does not have the segments we have required him
        UNAVAILABLE_SEGMENT_WARNING,
        // a slave informs that he has died
        DIED
    }

    /**
     * Type of the message
     */
    public final MessageType messageType;

    /**
     * For data
     */
    public final ResourceChunk resourceChunk;

    /**
     * For resource size report
     */
    public final Long resourceSize;

    /**
     * For segment availability reports
     */
    public final ResourcePart resourcePart;

    public SlaveMessage(byte[] data) {
        MutableOffset offset = new MutableOffset();
        messageType = Serializer.deserializeEnum(MessageType.class, data, offset);
        if (messageType != null) {
            switch (messageType) {
                case RESOURCE_CHUNK:
                    resourceChunk = new ResourceChunk(data, offset);
                    resourceSize = null;
                    resourcePart = null;
                    break;

                case RESOURCE_SIZE_REPORT:
                    resourceChunk = null;
                    resourceSize = Serializer.deserializeLongValue(data, offset);
                    resourcePart = null;
                    break;

                case SEGMENT_AVAILABILITY_REPORT:
                case SEGMENT_ASSIGNATION_REPORT:
                    resourceChunk = null;
                    resourceSize = null;
                    int length = Serializer.deserializeIntValue(data, offset);
                    resourcePart = new ResourcePart();
                    for (int i = 0; i < length; i++) {
                        long min = Serializer.deserializeLong(data, offset);
                        long max = Serializer.deserializeLong(data, offset);
                        resourcePart.add(new LongRange(min, max));
                    }
                    break;

                case UNAVAILABLE_SEGMENT_WARNING:
                case DIED:
                default:
                    resourceChunk = null;
                    resourceSize = null;
                    resourcePart = null;
            }
        } else {
            resourceChunk = null;
            resourceSize = null;
            resourcePart = null;
        }
    }

    static byte[] generateResourceChunkMessage(ResourceChunk resourceChunk) {
        byte[] messageType = Serializer.serialize(MessageType.RESOURCE_CHUNK);
        return Serializer.addArrays(messageType, resourceChunk.serialize());
    }

    static byte[] generateResourceSizeMessage(long size) {
        byte[] messageType = Serializer.serialize(MessageType.RESOURCE_SIZE_REPORT);
        return Serializer.addArrays(messageType, Serializer.serialize(size));
    }

    static byte[] generateResourceAvailabilityMessage(ResourcePart resourcePart) {
        byte[] messageType = Serializer.serialize(MessageType.SEGMENT_AVAILABILITY_REPORT);
        return Serializer.addArrays(messageType, serializeResourcePart(resourcePart));
    }

    static byte[] generateAssignedSegmentsMessage(ResourcePart resourcePart) {
        byte[] messageType = Serializer.serialize(MessageType.SEGMENT_ASSIGNATION_REPORT);
        return Serializer.addArrays(messageType, serializeResourcePart(resourcePart));
    }

    private static byte[] serializeResourcePart(ResourcePart resourcePart) {
        // serialize the number of segments, and then each segment
        byte[] message = Serializer.serialize(resourcePart.getRangesAsList().size());
        for (Range<Long> aSegment : resourcePart.getRangesAsList()) {
            message = Serializer.addArrays(message, Serializer.serialize(aSegment.getMin()));
            message = Serializer.addArrays(message, Serializer.serialize(aSegment.getMax()));
        }
        return message;
    }

    static byte[] generateUnavailableSegmentsMessage() {
        return Serializer.serialize(MessageType.UNAVAILABLE_SEGMENT_WARNING);
    }

    static byte[] generateDiedMessage() {
        return Serializer.serialize(MessageType.DIED);
    }
}
