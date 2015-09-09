package jacz.peerengine.util.datatransfer.resource_accession;

import jacz.peerengine.util.datatransfer.master.MasterMessage;
import jacz.util.numeric.LongRange;
import jacz.peerengine.PeerID;
import jacz.peerengine.util.datatransfer.ResourceStreamingManager;
import jacz.peerengine.util.datatransfer.slave.SlaveResourceStreamer;

/**
 * A resource link implementation for resources provided by peers
 */
public class PeerResourceLink implements ResourceLink {

    private final ResourceStreamingManager resourceStreamingManager;

    private final PeerID otherPeer;

    private short outgoingSubchannel;

    public PeerResourceLink(ResourceStreamingManager resourceStreamingManager, PeerID otherPeer) {
        this.resourceStreamingManager = resourceStreamingManager;
        this.otherPeer = otherPeer;
    }

    @Override
    public long recommendedMillisForRequest() {
        return 10000;
    }

    @Override
    public void initialize(Object initializationMessage) {
        outgoingSubchannel = (Short) initializationMessage;
    }

    @Override
    public Long surviveTimeMillis() {
        return SlaveResourceStreamer.SURVIVE_TIME_MILLIS;
    }

    @Override
    public void requestResourceLength() {
        resourceStreamingManager.write(otherPeer, outgoingSubchannel, MasterMessage.generateReportResourceLengthMessage());
    }

    @Override
    public void requestAvailableSegments() {
        resourceStreamingManager.write(otherPeer, outgoingSubchannel, MasterMessage.generateReportAvailableSegmentsMessage());
    }

    @Override
    public void requestAssignedSegments() {
        resourceStreamingManager.write(otherPeer, outgoingSubchannel, MasterMessage.generateReportAssignedSegmentsMessage());
    }

    @Override
    public void eraseSegments() {
        resourceStreamingManager.write(otherPeer, outgoingSubchannel, MasterMessage.generateEraseSegmentsMessage());
    }

    @Override
    public void addNewSegment(LongRange segment) {
        resourceStreamingManager.write(otherPeer, outgoingSubchannel, MasterMessage.generateAddNewSegmentsMessage(segment));
    }

    @Override
    public void setSpeed(Float speed) {
        resourceStreamingManager.write(otherPeer, outgoingSubchannel, MasterMessage.generateSetSpeedMessage(speed));
    }

    @Override
    public void ping() {
        resourceStreamingManager.write(otherPeer, outgoingSubchannel, MasterMessage.generatePingMessage());
    }

    @Override
    public void die() {
        resourceStreamingManager.write(otherPeer, outgoingSubchannel, MasterMessage.generateDiedMessage());
    }
}
