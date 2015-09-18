package jacz.peerengineservice.util.datatransfer;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.channel.ChannelFSMAction;

/**
 * This class handles both incoming packages to a channel assigned for data streaming purposes (by defining an FSM
 * that never ends, redirects all packages) and outgoing traffic . When created, the PeerClient registers it
 * to the correct channel so it receives all needed incoming traffic.
 * <p/>
 * It also handles outgoing packages. This way, the corresponding DataStreamingManager has
 * direct communication with the rest of DataStreamingManagers.
 * <p/>
 * There is one PeerDataReceiver object for each peer we are connected to, handling communication with one
 * of those peers. In addition, this class offers a new multiplexing level of 16 bits (subchannels).
 */
class PeerDataReceiver implements ChannelFSMAction<PeerDataReceiver.DataTransferConnectionManagerState> {

    public enum DataTransferConnectionManagerState {
        CONNECTED
    }

    /**
     * The ResourceStreamingManager object that will use this PeerDataReceiver. All incoming messages are
     * redirected to this object
     */
    private final ResourceStreamingManager resourceStreamingManager;

    public PeerDataReceiver(ResourceStreamingManager resourceStreamingManager) {
        this.resourceStreamingManager = resourceStreamingManager;
    }

    @Override
    public DataTransferConnectionManagerState processMessage(
            DataTransferConnectionManagerState state,
            byte b,
            Object o,
            ChannelConnectionPoint ccp
    ) throws IllegalArgumentException {
        // received packages are redirected to the corresponding ResourceStreamingManager
        resourceStreamingManager.processMessage(o);
        return DataTransferConnectionManagerState.CONNECTED;
    }

    @Override
    public DataTransferConnectionManagerState processMessage(
            DataTransferConnectionManagerState state,
            byte b,
            byte[] bytes,
            ChannelConnectionPoint ccp
    ) throws IllegalArgumentException {
        resourceStreamingManager.processMessage(bytes);
        return DataTransferConnectionManagerState.CONNECTED;
    }

    @Override
    public DataTransferConnectionManagerState init(ChannelConnectionPoint ccp) {
        return DataTransferConnectionManagerState.CONNECTED;
    }

    @Override
    public boolean isFinalState(DataTransferConnectionManagerState state, ChannelConnectionPoint ccp) {
        return false;
    }

    @Override
    public void disconnected(ChannelConnectionPoint ccp) {
        // ignore
    }
}
