package jacz.peerengineservice.client;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.channel.TimedChannelFSMAction;

/**
 * A timed version of custom FSMs
 */
public class CustomTimedPeerFSM<T> extends CustomPeerFSM<T> implements TimedChannelFSMAction<T> {

    /**
     * Additional action (timed version). Only used for invoking the timeOut method.
     */
    private PeerTimedFSMAction<T> timedInternalFSM;

    /**
     * Class constructor for clients
     *
     * @param internalFSM
     * @param serverFSMName
     * @param assignedChannel
     */
    public CustomTimedPeerFSM(PeerTimedFSMAction<T> internalFSM, String serverFSMName, byte assignedChannel) {
        super(internalFSM, serverFSMName, assignedChannel);
        this.timedInternalFSM = internalFSM;
    }

    /**
     * Class constructor for servers
     *
     * @param internalFSM
     * @param assignedChannel
     * @param outgoingChannel
     */
    public CustomTimedPeerFSM(PeerTimedFSMAction<T> internalFSM, byte assignedChannel, byte outgoingChannel) {
        super(internalFSM, assignedChannel, outgoingChannel);
        this.timedInternalFSM = internalFSM;
    }

    @Override
    public void timedOut(T state, ChannelConnectionPoint ccp) {
        timedInternalFSM.timedOut(state);
    }
}
