package jacz.peerengineservice.util.datatransfer.resource_accession;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.datatransfer.master.MasterMessage;
import org.aanguita.jacuzzi.numeric.range.LongRange;
import jacz.peerengineservice.util.datatransfer.ResourceStreamingManager;
import jacz.peerengineservice.util.datatransfer.slave.SlaveResourceStreamer;

/**
 * A resource link implementation for resources provided by peers
 */
public class PeerResourceLink implements ResourceLink {

    private final ResourceStreamingManager resourceStreamingManager;

    private final PeerId otherPeer;

    private short outgoingSubchannel;

    public PeerResourceLink(ResourceStreamingManager resourceStreamingManager, PeerId otherPeer) {
        this.resourceStreamingManager = resourceStreamingManager;
        this.otherPeer = otherPeer;
    }

    @Override
    public String toString() {
        return "PeerResourceLink{" +
                "otherPeer=" + otherPeer +
                '}';
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
        resourceStreamingManager.write(otherPeer, outgoingSubchannel, MasterMessage.generateReportResourceLengthMessage(), false);
    }

    @Override
    public void requestAvailableSegments() {
        resourceStreamingManager.write(otherPeer, outgoingSubchannel, MasterMessage.generateReportAvailableSegmentsMessage(), false);
    }

    @Override
    public void requestAssignedSegments() {
        resourceStreamingManager.write(otherPeer, outgoingSubchannel, MasterMessage.generateReportAssignedSegmentsMessage(), false);
    }

    @Override
    public void eraseSegments() {
        resourceStreamingManager.write(otherPeer, outgoingSubchannel, MasterMessage.generateEraseSegmentsMessage(), false);
    }

    @Override
    public void addNewSegment(LongRange segment) {
        resourceStreamingManager.write(otherPeer, outgoingSubchannel, MasterMessage.generateAddNewSegmentsMessage(segment), false);
    }

    @Override
    public void hardThrottle(float variation) {
        resourceStreamingManager.write(otherPeer, outgoingSubchannel, MasterMessage.generateHardThrottleMessage(variation), false);
    }

    @Override
    public void softThrottle() {
        resourceStreamingManager.write(otherPeer, outgoingSubchannel, MasterMessage.generateSoftThrottleMessage(), false);
    }

    @Override
    public void ping() {
        resourceStreamingManager.write(otherPeer, outgoingSubchannel, MasterMessage.generatePingMessage(), false);
    }

    @Override
    public void die() {
        resourceStreamingManager.write(otherPeer, outgoingSubchannel, MasterMessage.generateDiedMessage(), false);
    }
}
