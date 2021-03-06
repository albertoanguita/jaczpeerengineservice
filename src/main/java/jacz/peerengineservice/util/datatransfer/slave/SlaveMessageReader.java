package jacz.peerengineservice.util.datatransfer.slave;

import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceReader;
import org.aanguita.jacuzzi.date_time.PerformRegularAction;
import org.aanguita.jacuzzi.numeric.range.LongRange;
import org.aanguita.jacuzzi.queues.event_processing.MessageReader;
import org.aanguita.jacuzzi.queues.event_processing.StopReadingMessages;

import java.nio.file.AccessMode;

/**
 * This class implements a message reader that retrieves chunks of resources for being sent to other peer. These
 * chucks are extracted according to a range queue that indicates which blocks of resource should be sent. This
 * queue is updated outside this class, by the slave resource streamer. Should this queue be empty, this method
 * will block until it is filled.
 * <p/>
 * Code is included to allow controlling the uploading speed of data. The class offers a method for setting the
 * desired speed, and it makes its best for keeping up with the assigned value. It also allows reading the achieved
 * speed.
 * <p/>
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

    private static final double INITIAL_BLOCK_SIZE = 1024d;

    private static final double MINIMUM_BLOCK_SIZE = 8d;

    private static final long MILLIS_PACKET_SIZE_RECALCULATION = 100L;

    private static final double SMALL_BLOCK_SIZE_GROW_FACTOR = 1.001d;

    private static final double MEDIUM_BLOCK_SIZE_GROW_FACTOR = 1.003d;

    private static final double LARGE_BLOCK_SIZE_GROW_FACTOR = 1.010d;

    private static final float AUTO_THROTTLE_FACTOR = 0.999f;

    private static final float SOFT_THROTTLE_FACTOR = 0.992f;

    private static final int NUMBER_OF_GROWS_TO_GO_LARGER = 100;

    private final SlaveResourceStreamer slaveResourceStreamer;

    private final SlaveResourceStreamer.ResourceSegmentQueue resourceSegmentQueue;

    private final ResourceReader resourceReader;

    private final SlaveMessageHandler messageHandler;

    private boolean mustFlush;

    /**
     * The preferred size for resource chunks (number of bytes). This size is set accordingly with the desired speed.
     * This size is set so it takes approximately 0.1 seconds to send the chunk, according to the desired speed
     */
    private double preferredBlockSize;

    private PerformRegularAction increasePacketSizeAction;

    private int numberOfBlockSizeGrows;

    private final Object blockSizeLock = new Object();

    public SlaveMessageReader(SlaveResourceStreamer slaveResourceStreamer, SlaveResourceStreamer.ResourceSegmentQueue resourceSegmentQueue, ResourceReader resourceReader, SlaveMessageHandler messageHandler) {
        this.slaveResourceStreamer = slaveResourceStreamer;
        this.resourceSegmentQueue = resourceSegmentQueue;
        this.resourceReader = resourceReader;
        this.messageHandler = messageHandler;
        mustFlush = false;
        preferredBlockSize = INITIAL_BLOCK_SIZE;
        increasePacketSizeAction = PerformRegularAction.timeElapsePerformRegularAction(MILLIS_PACKET_SIZE_RECALCULATION);
        numberOfBlockSizeGrows = 0;
    }

    public void hardThrottle(float variation) {
        synchronized (blockSizeLock) {
            // reduce the preferred block size, and reset number of grows
            preferredBlockSize *= variation;
            preferredBlockSize = Math.max(preferredBlockSize, MINIMUM_BLOCK_SIZE);
            numberOfBlockSizeGrows = 0;
        }
    }

    public void softThrottle() {
        synchronized (blockSizeLock) {
            // reduce the preferred block size, and reset number of grows
            preferredBlockSize *= SOFT_THROTTLE_FACTOR;
            preferredBlockSize = Math.max(preferredBlockSize, MINIMUM_BLOCK_SIZE);
        }
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
            if (messageHandler.isChoke()) {
                // auto hardThrottle
                hardThrottle(AUTO_THROTTLE_FACTOR);
            } else {
                synchronized (blockSizeLock) {
                    if (increasePacketSizeAction.mustPerformAction()) {
                        // time to recalculate packet size
                        if (numberOfBlockSizeGrows < NUMBER_OF_GROWS_TO_GO_LARGER) {
                            preferredBlockSize *= SMALL_BLOCK_SIZE_GROW_FACTOR;
                            numberOfBlockSizeGrows++;
                        } else if (numberOfBlockSizeGrows < 2 * NUMBER_OF_GROWS_TO_GO_LARGER) {
                            preferredBlockSize *= MEDIUM_BLOCK_SIZE_GROW_FACTOR;
                            numberOfBlockSizeGrows++;
                        } else {
                            preferredBlockSize *= LARGE_BLOCK_SIZE_GROW_FACTOR;
                        }
                    }
                }
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
            LongRange rangeToSend = removedRange.range;
            slaveResourceStreamer.reportResourceSegmentSent(rangeToSend);
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
//        speedMonitor.stop();
    }
}
