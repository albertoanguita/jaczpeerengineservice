package jacz.peerengine.server;

import jacz.peerengine.client.connection.RequestFromServerToPeer;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.channel.TimedChannelFSMAction;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.io.object_serialization.ObjectListWrapper;
import jacz.peerengine.PeerID;

import java.util.Map;

/**
 * This class allows peer servers to dispatch requests from clients. So far the only available request is to search
 * for connected friends (more are to be added in the future). Custom FSM are not available at the moment, but could
 */
class RequestDispatcherFSM implements TimedChannelFSMAction<RequestDispatcherFSM.State> {

    /**
     * The FSM has only one state, for receiving new requests. The FSM never terminates
     */
    enum State {
        INIT
    }

    /**
     * The PeerServer for which this RequestDispatcher processes new requests
     */
    private PeerServer peerServer;

    /**
     * ID of the client for which this FSM accepts requests
     */
    UniqueIdentifier clientID;

    /**
     * Class constructor
     *
     * @param peerServer The PeerServer for which this RequestDispatcher processes new requests
     * @param clientID   the id of the client which this request dispatcher will be listening
     */
    public RequestDispatcherFSM(PeerServer peerServer, UniqueIdentifier clientID) {
        this.peerServer = peerServer;
        this.clientID = clientID;
    }

    @Override
    public State processMessage(State state, byte channel, Object message, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        if (message instanceof RequestFromPeerToServer) {
            RequestFromPeerToServer requestFromPeerToServer = (RequestFromPeerToServer) message;
            Byte outgoingChannel = requestFromPeerToServer.channel;
            switch (requestFromPeerToServer.requestType) {

                case SEARCH_FRIENDS:
                    // ask peer server about these clients, and inform client (send a message) of successful connection
                    Map<PeerID, PeerConnectionInfo> friendsConnectionData = peerServer.searchConnectedClients(requestFromPeerToServer.friendPeerIDs);
                    ccp.write(outgoingChannel, new ObjectListWrapper(friendsConnectionData));
                    break;

                case PING:
                    // answer with a ping to the client
                    ccp.write(outgoingChannel, RequestFromServerToPeer.pingRequest());
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

    @Override
    public void timedOut(State state, ChannelConnectionPoint ccp) {
        // report the peer server that this client has timed out
        peerServer.clientTimedOut(clientID);
    }
}
