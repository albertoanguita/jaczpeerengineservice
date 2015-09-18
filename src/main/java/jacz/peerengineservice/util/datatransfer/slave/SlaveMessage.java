package jacz.peerengineservice.util.datatransfer.slave;

import jacz.peerengineservice.util.datatransfer.master.ResourcePart;
import jacz.util.io.object_serialization.MutableOffset;
import jacz.util.io.object_serialization.Serializer;
import jacz.util.numeric.LongRange;

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
        // report of the hash of the subsequent data
        SEGMENT_HASH_DATA,
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

    public final byte[] correctHash;

    public final String hashAlgorithm;

    public final LongRange hashSegment;

    public SlaveMessage(byte[] data) {
        MutableOffset offset = new MutableOffset();
        messageType = Serializer.deserializeEnum(MessageType.class, data, offset);
        if (messageType != null) {
            switch (messageType) {
                case RESOURCE_CHUNK:
                    resourceChunk = new ResourceChunk(data, offset);
                    resourceSize = null;
                    resourcePart = null;
                    correctHash = null;
                    hashAlgorithm = null;
                    hashSegment = null;
                    break;

                case RESOURCE_SIZE_REPORT:
                    resourceChunk = null;
                    resourceSize = Serializer.deserializeLong(data, offset);
                    resourcePart = null;
                    correctHash = null;
                    hashAlgorithm = null;
                    hashSegment = null;
                    break;

                case SEGMENT_AVAILABILITY_REPORT:
                case SEGMENT_ASSIGNATION_REPORT:
                    resourceChunk = null;
                    resourceSize = null;
                    int length = Serializer.deserializeInt(data, offset);
                    resourcePart = new ResourcePart();
                    for (int i = 0; i < length; i++) {
                        long min = Serializer.deserializeLong(data, offset);
                        long max = Serializer.deserializeLong(data, offset);
                        resourcePart.add(new LongRange(min, max));
                    }
                    correctHash = null;
                    hashAlgorithm = null;
                    hashSegment = null;
                    break;

                case SEGMENT_HASH_DATA:
                    resourceChunk = null;
                    resourceSize = null;
                    resourcePart = null;
                    correctHash = Serializer.deserializeBytes(data, offset);
                    hashAlgorithm = Serializer.deserializeString(data, offset);
                    Long min = Serializer.deserializeLong(data, offset);
                    Long max = Serializer.deserializeLong(data, offset);
                    hashSegment = new LongRange(min, max);
                    break;

                case UNAVAILABLE_SEGMENT_WARNING:
                case DIED:
                default:
                    resourceChunk = null;
                    resourceSize = null;
                    resourcePart = null;
                    correctHash = null;
                    hashAlgorithm = null;
                    hashSegment = null;
            }
        } else {
            resourceChunk = null;
            resourceSize = null;
            resourcePart = null;
            correctHash = null;
            hashAlgorithm = null;
            hashSegment = null;
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
        byte[] message = Serializer.serialize(resourcePart.getRanges().size());
        for (LongRange aSegment : resourcePart.getRanges()) {
            message = Serializer.addArrays(message, Serializer.serialize(aSegment.getMin()));
            message = Serializer.addArrays(message, Serializer.serialize(aSegment.getMax()));
        }
        return message;
    }

    static byte[] generateSegmentHashData(byte[] correctHash, String hashAlgorithm, LongRange hashSegment) {
        byte[] messageType = Serializer.serialize(MessageType.SEGMENT_HASH_DATA);
        return Serializer.addArrays(messageType, Serializer.serialize(correctHash), Serializer.serialize(hashAlgorithm), Serializer.serialize(hashSegment.getMin()), Serializer.serialize(hashSegment.getMax()));
    }

    static byte[] generateUnavailableSegmentsMessage() {
        return Serializer.serialize(MessageType.UNAVAILABLE_SEGMENT_WARNING);
    }

    static byte[] generateDiedMessage() {
        return Serializer.serialize(MessageType.DIED);
    }
}
