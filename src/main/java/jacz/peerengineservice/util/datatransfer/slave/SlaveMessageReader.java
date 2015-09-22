package jacz.peerengineservice.util.datatransfer.slave;

import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceReader;
import jacz.util.date_time.SpeedMonitor;
import jacz.util.numeric.LongRange;
import jacz.util.queues.event_processing.MessageReader;
import jacz.util.queues.event_processing.StopReadingMessages;

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

        MessageForHandler(ResourceChunk resourceChunk) {
            this(resourceChunk, null);
        }

        MessageForHandler(Boolean flush) {
            this(null, flush);
        }

        MessageForHandler(ResourceChunk resourceChunk, Boolean flush) {
            this.resourceChunk = resourceChunk;
            isFlush = flush;
        }
    }

    /**
     * Speed is measured over this amount of milliseconds
     */
    private static final int MILLIS_SPEED_MEASURE = 3000;

    private static final double INITIAL_BLOCK_SIZE = 1000d;

    private static final double BLOCK_SIZE_GROW_FACTOR = 1.003d;

    private final SlaveResourceStreamer slaveResourceStreamer;

    private final SlaveResourceStreamer.ResourceSegmentQueue resourceSegmentQueue;

    private final ResourceReader resourceReader;

    private boolean mustFlush;

    /**
     * The preferred size for resource chunks (number of bytes). This size is set accordingly with the desired speed.
     * This size is set so it takes approximately 0.1 seconds to send the chunk, according to the desired speed
     */
    private double preferredBlockSize;

    /**
     * For measuring and controlling the writing speed
     */
    private SpeedMonitor speedLimiter;

    public SlaveMessageReader(SlaveResourceStreamer slaveResourceStreamer, SlaveResourceStreamer.ResourceSegmentQueue resourceSegmentQueue, ResourceReader resourceReader) {
        this.slaveResourceStreamer = slaveResourceStreamer;
        this.resourceSegmentQueue = resourceSegmentQueue;
        this.resourceReader = resourceReader;
        mustFlush = false;
        speedLimiter = new SpeedMonitor(MILLIS_SPEED_MEASURE);
        preferredBlockSize = INITIAL_BLOCK_SIZE;
    }

    public synchronized void throttle(float variation) {
        // insert a set of wait blocks among the following sending messages
        preferredBlockSize *= variation;
        preferredBlockSize = Math.max(preferredBlockSize, INITIAL_BLOCK_SIZE);
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
        } else {
            SlaveResourceStreamer.RemovedRange removedRange;
            synchronized (this) {
                removedRange = resourceSegmentQueue.remove((long) preferredBlockSize);
            }
            preferredBlockSize *= BLOCK_SIZE_GROW_FACTOR;
            if (removedRange.range == SlaveResourceStreamer.stopMessage) {
                // the slave resource streamer has requested us to die
                resourceReader.stop();
                return new StopReadingMessages();
            }
            if (resourceSegmentQueue.isEmpty()) {
                // mark the need to flush the channel in the next iteration
                mustFlush = true;
            }
            LongRange rangeToSend = removedRange.range;
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

    @Override
    public void stopped() {

    }
}
