package jacz.peerengineservice.client.connection;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.channel.ChannelFSMAction;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerClientPrivateInterface;

/**
 * This class implements the FSM in charge of processing requests received from other PeerClients to which we are
 * already connected.
 * <p/>
 * It has a unique state INIT, in which it processes a single request (then it returns to the INIT state, so it never
 * ends and is always available for subsequent requests)
 */
public class PeerRequestDispatcherFSM implements ChannelFSMAction<PeerRequestDispatcherFSM.State> {

    /**
     * The FSM has only one state, for receiving new requests. The FSM never terminates
     */
    enum State {
        INIT
    }

    /**
     * The PeerClient for which this RequestDispatcher processes new requests
     */
    private final PeerClientPrivateInterface peerClientPrivateInterface;

    /**
     * The Peer to which this RequestDispatcher listens
     */
    private final PeerId peerId;

    /**
     * Class constructor
     *
     * @param peerClientPrivateInterface the interface to the PeerClient for which this RequestDispatcher processes new requests
     * @param peerId     The ID of out client
     */
    public PeerRequestDispatcherFSM(PeerClientPrivateInterface peerClientPrivateInterface, PeerId peerId) {
        this.peerClientPrivateInterface = peerClientPrivateInterface;
        this.peerId = peerId;
    }

    @Override
    public State processMessage(State state, byte channel, Object message, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        if (message instanceof RequestFromPeerToPeer) {
            RequestFromPeerToPeer requestFromPeerToPeer = (RequestFromPeerToPeer) message;
            Byte outgoingChannel = requestFromPeerToPeer.channel;
            switch (requestFromPeerToPeer.requestType) {

                case OBJECT_MESSAGE:
                    // report our PeerClient
                    peerClientPrivateInterface.newObjectMessageReceived(peerId, requestFromPeerToPeer.customObject);
                    break;

                case CUSTOM:
                    // requestFromPeerToPeer for a new custom FSM received, look for the FSM in the custom FSM factory
                    // if we don't have that custom FSM registered, simply ignore
                    String value = requestFromPeerToPeer.value;
                    peerClientPrivateInterface.requestServerCustomFSM(requestFromPeerToPeer, value, peerId, ccp, outgoingChannel);
                    break;
            }
        }
        return State.INIT;
    }

    @Override
    public State processMessage(State state, byte channel, byte[] data, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        // unexpected message -> ignore (we must always receive objects)
        return State.INIT;
    }

    @Override
    public State init(ChannelConnectionPoint ccp) {
        // the INIT state is the initial state
        return State.INIT;
    }

    @Override
    public boolean isFinalState(State state, ChannelConnectionPoint ccp) {
        // this FSM is always in the INIT state, it never terminates
        return false;
    }

    @Override
    public void disconnected(ChannelConnectionPoint ccp) {
        // ignore
    }
}
