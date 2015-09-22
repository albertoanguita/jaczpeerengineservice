package jacz.peerengineservice.util.datatransfer.slave;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.datatransfer.ResourceStreamingManager;
import jacz.util.queues.event_processing.MessageHandler;

/**
 *
 */
public class SlaveMessageHandler implements MessageHandler {

    private final ResourceStreamingManager resourceStreamingManager;

    private final PeerID otherPeer;

    private final short outgoingChannel;

    private final UploadSessionStatistics uploadSessionStatistics;


    public SlaveMessageHandler(ResourceStreamingManager resourceStreamingManager, PeerID otherPeer, short outgoingChannel, UploadSessionStatistics uploadSessionStatistics) {
        this.resourceStreamingManager = resourceStreamingManager;
        this.otherPeer = otherPeer;
        this.outgoingChannel = outgoingChannel;
        this.uploadSessionStatistics = uploadSessionStatistics;
    }

    @Override
    public void handleMessage(Object o) {
        // the received message is the ResourceChunk to send to the master. We serialize it before sending it
        SlaveMessageReader.MessageForHandler messageForHandler = (SlaveMessageReader.MessageForHandler) o;
        if (messageForHandler.isFlush != null) {
            resourceStreamingManager.flush(otherPeer);
        } else {
            byte[] dataToSend = SlaveMessage.generateResourceChunkMessage(messageForHandler.resourceChunk);
            resourceStreamingManager.write(otherPeer, outgoingChannel, dataToSend, true);
            uploadSessionStatistics.addUploadedSegment(messageForHandler.resourceChunk.getSegment());
        }
    }

    @Override
    public void finalizeHandler() {
        // nothing to do
    }
}
