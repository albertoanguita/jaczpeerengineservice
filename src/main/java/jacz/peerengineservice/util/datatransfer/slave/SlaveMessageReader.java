package jacz.peerengineservice.util.datatransfer.slave;

import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceReader;
import jacz.util.date_time.SpeedLimiter;
import jacz.util.hash.HashFunction;
import jacz.util.numeric.LongRange;
import jacz.util.queues.event_processing.MessageReader;
import jacz.util.queues.event_processing.StopReadingMessages;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * This class implements a message reader that retrieves chunks of resources for being sent to other peer. These
 * chucks are extracted according to a range queue that indicates which blocks of resource should be sent. This
 * queue is updated outside this class, by the slave resource streamer. Should this queue be empty, this method
 * will block until it is filled.
 * <p/>
 * Code is included to allow controlling the uploading speed of data. The class offers a method for setting the
 * desired speed, and it makes its best for keeping up with the assigned value. It also allows reading the achieved
 * speed.
 */
class SlaveMessageReader implements MessageReader {

    static class MessageForHandler {

        final ResourceChunk resourceChunk;

        final Boolean isFlush;

        final LongRange hashSegment;

        final String hashAlgorithm;

        final byte[] correctHash;

        MessageForHandler(ResourceChunk resourceChunk) {
            this(resourceChunk, null, null, null, null);
        }

        MessageForHandler(Boolean flush) {
            this(null, flush, null, null, null);
        }

        MessageForHandler(LongRange hashSegment, String hashAlgorithm, byte[] correctHash) {
            this(null, null, hashSegment, hashAlgorithm, correctHash);
        }

        MessageForHandler(ResourceChunk resourceChunk, Boolean flush, LongRange hashSegment, String hashAlgorithm, byte[] correctHash) {
            this.resourceChunk = resourceChunk;
            isFlush = flush;
            this.hashSegment = hashSegment;
            this.hashAlgorithm = hashAlgorithm;
            this.correctHash = correctHash;
        }
    }

    /**
     * Speed is measured over this amount of milliseconds
     */
    private static final int MILLIS_SPEED_MEASURE = 3000;

    private static final int PREFERRED_BLOCK_SIZE_FACTOR = 10;

    private final SlaveResourceStreamer slaveResourceStreamer;

    private final SlaveResourceStreamer.ResourceSegmentQueue resourceSegmentQueue;

    private final ResourceReader resourceReader;

    private boolean mustFlush;

    /**
     * The preferred size for resource chunks (number of bytes). This size is set accordingly with the desired speed.
     * This size is set so it takes approximately 0.1 seconds to send the chunk, according to the desired speed
     */
    private long preferredBlockSize;

    /**
     * For measuring and controlling the writing speed
     */
    private SpeedLimiter speedLimiter;

    public SlaveMessageReader(SlaveResourceStreamer slaveResourceStreamer, SlaveResourceStreamer.ResourceSegmentQueue resourceSegmentQueue, ResourceReader resourceReader, float desiredSpeed) {
        this.slaveResourceStreamer = slaveResourceStreamer;
        this.resourceSegmentQueue = resourceSegmentQueue;
        this.resourceReader = resourceReader;
        mustFlush = false;
        speedLimiter = new SpeedLimiter(MILLIS_SPEED_MEASURE, (double) desiredSpeed);
        setSpeed(desiredSpeed);
    }

    public synchronized void setSpeed(float speed) {
        speedLimiter.setSpeedLimit((double) speed);
        setPreferredBlockSize(speed);
    }

    private synchronized void setPreferredBlockSize(float speed) {
        // set to 1 tenth of the speed. Minimum is 1
        preferredBlockSize = (int) speed / PREFERRED_BLOCK_SIZE_FACTOR;
        if (preferredBlockSize < 1) {
            preferredBlockSize = 1;
        }
    }

    public Float getAchievedSpeed() {
        Double speed = speedLimiter.getAverageSpeed();
        return speed == null ? null : speed.floatValue();
    }

    @Override
    public Object readMessage() {
        // retrieve a range from the segment queue, get the corresponding data block from the actual resource
        // and return that block (a ResourceChunk object). The speed limiter is notified so it halts execution the
        // necessary time to adapt to the desired speed
        if (mustFlush) {
            // flush the channel
            mustFlush = false;
            return new MessageForHandler(true);
//            return ResourceChunk.generateFlushChunk();
        } else {
            SlaveResourceStreamer.RemovedRange removedRange;
            synchronized (this) {
                removedRange = resourceSegmentQueue.remove(preferredBlockSize);
            }
            if (removedRange.range == SlaveResourceStreamer.stopMessage) {
                // the slave resource streamer has requested us to die
                resourceReader.stop();
                return new StopReadingMessages();
            }
            if (resourceSegmentQueue.isEmpty()) {
                // mark the need to flush the channel in the next iteration
                mustFlush = true;
            }
            if (removedRange.isHash) {
                // we must send a hash
                try {
                    HashFunction hashFunction = new HashFunction(SlaveResourceStreamer.HASH_ALGORITHM);
                    byte[] correctHash;
                    try {
                        correctHash = hashFunction.digest(resourceReader.read(removedRange.range.getMin(), removedRange.range.size().intValue()));
                        return new MessageForHandler(removedRange.range, SlaveResourceStreamer.HASH_ALGORITHM, correctHash);
                    } catch (IOException e) {
                        slaveResourceStreamer.die(true);
                        return new StopReadingMessages();
                    }
                } catch (NoSuchAlgorithmException e) {
                    // hash cannot be sent, send a flush instead
                    return new MessageForHandler(true);
                }
            } else {
                LongRange rangeToSend = removedRange.range;
                // todo since removed blocks are big in size (e.g. 10kb), speed gives big jumps and it is hard to fix it to a strict value -> CHECK
                speedLimiter.addProgress(rangeToSend.size());
                byte[] data;
                try {
                    data = resourceReader.read(rangeToSend.getMin(), rangeToSend.size().intValue());
                } catch (Exception e) {
                    slaveResourceStreamer.die(true);
                    return new StopReadingMessages();
                }
                return new MessageForHandler(ResourceChunk.generateDataChunk(rangeToSend.getMin(), data));
            }
        }
    }

    @Override
    public void stopped() {

    }
}
