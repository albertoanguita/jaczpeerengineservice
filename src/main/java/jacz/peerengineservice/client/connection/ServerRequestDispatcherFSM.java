package jacz.peerengineservice.client.connection;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.channel.ChannelFSMAction;

/**
 * FSM in charge of receiving requests from the PeerServer
 */
public class ServerRequestDispatcherFSM implements ChannelFSMAction<ServerRequestDispatcherFSM.State> {

    /**
     * The FSM has only one state, for receiving new requests. The FSM never terminates
     */
    enum State {
        INIT
    }

    /**
     * The PeerServerManager for which this RequestDispatcher processes new requests
     */
    private PeerServerManager peerServerManager;

    /**
     * Class constructor
     *
     * @param peerServerManager the peerServerManager who owns us
     */
    public ServerRequestDispatcherFSM(PeerServerManager peerServerManager) {
        this.peerServerManager = peerServerManager;
    }

    @Override
    public State processMessage(State state, byte channel, Object message, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        if (message instanceof RequestFromServerToPeer) {
            RequestFromServerToPeer requestFromServerToPeer = (RequestFromServerToPeer) message;
            switch (requestFromServerToPeer.requestType) {

                case PING:
                    peerServerManager.peerServerPingReceived();
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
