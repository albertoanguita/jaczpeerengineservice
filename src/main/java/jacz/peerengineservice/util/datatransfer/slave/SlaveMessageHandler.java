package jacz.peerengineservice.util.datatransfer.slave;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.datatransfer.ResourceStreamingManager;
import jacz.util.date_time.PerformRegularAction;
import jacz.util.date_time.SpeedLimiter;
import jacz.util.queues.event_processing.MessageHandler;

/**
 *
 */
public class SlaveMessageHandler implements MessageHandler {

    private static final long CHOKE_THRESHOLD = 150L;

    private static final long PACKETS_PER_SECOND_MEASURE_TIME = 1000L;

    private static final double MAX_PACKETS_PER_SECOND = 50d;

    private static final long MILLIS_BETWEEN_FLUSHES = 200L;



    private final ResourceStreamingManager resourceStreamingManager;

    private final PeerID otherPeer;

    private final short outgoingChannel;

    private final UploadSessionStatistics uploadSessionStatistics;

    private boolean isChoke;

    private final SpeedLimiter sendPacketSpeedLimiter;

    private final PerformRegularAction flushDataRegularAction;


    public SlaveMessageHandler(ResourceStreamingManager resourceStreamingManager, PeerID otherPeer, short outgoingChannel, UploadSessionStatistics uploadSessionStatistics) {
        this.resourceStreamingManager = resourceStreamingManager;
        this.otherPeer = otherPeer;
        this.outgoingChannel = outgoingChannel;
        this.uploadSessionStatistics = uploadSessionStatistics;
        this.isChoke = false;
        sendPacketSpeedLimiter = new SpeedLimiter(PACKETS_PER_SECOND_MEASURE_TIME, MAX_PACKETS_PER_SECOND);
        flushDataRegularAction = PerformRegularAction.timeElapsePerformRegularAction(MILLIS_BETWEEN_FLUSHES);
    }

    @Override
    public void handleMessage(Object o) {
        // the received message is the ResourceChunk to send to the master. We serialize it before sending it
        SlaveMessageReader.MessageForHandler messageForHandler = (SlaveMessageReader.MessageForHandler) o;
        if (messageForHandler.isFlush != null) {
            resourceStreamingManager.flush(otherPeer);
        } else {
            byte[] dataToSend = SlaveMessage.generateResourceChunkMessage(messageForHandler.resourceChunk);
            sendPacketSpeedLimiter.addProgress(1L);
            long time = resourceStreamingManager.write(otherPeer, outgoingChannel, dataToSend, false);
            if (flushDataRegularAction.mustPerformAction()) {
                resourceStreamingManager.flush(otherPeer);
            }
            synchronized (this) {
                isChoke = time > CHOKE_THRESHOLD;
            }
            uploadSessionStatistics.addUploadedSegment(messageForHandler.resourceChunk.getSegment());
        }
    }

    @Override
    public void finalizeHandler() {
        // nothing to do
    }

    synchronized boolean isChoke() {
        return isChoke;
    }
}
