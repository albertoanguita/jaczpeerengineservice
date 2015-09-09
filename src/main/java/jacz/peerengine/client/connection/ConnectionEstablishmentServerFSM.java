package jacz.peerengine.client.connection;

import jacz.peerengine.PeerID;
import jacz.peerengine.util.ChannelConstants;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.channel.TimedChannelFSMAction;

/**
 * This FSM allows negotiating with other peers who want to connect to our PeerClient. This FSM implements the server
 * part of that connection. First it receives the data of a new peer trying to connect to us, and then it answers to
 * that peer if it accepts the connection or not.
 * <p/>
 * // todo tb puede servir para que el servidor testee nuestra conexión (puerto abierto, etc). DO IN HOLE PUNCHING
 */
public class ConnectionEstablishmentServerFSM implements TimedChannelFSMAction<ConnectionEstablishmentServerFSM.State> {

    enum State {
        // Initial state: waiting for peers to try to connect to us. Here, we will receive the data from peers
        // attempting connection to us
        WAITING_DATA,
        // Connection process was successful
        SUCCESS,
        // error in the connection -> must disconnect
        ERROR
    }

    public enum ConnectionResult {

        // all data is correct -> connection established
        CORRECT,
        // our peerClient does not know this peerClient (not among the list of friend peers). Connection will be
        // established, but the client peer will have to be validated prior any operations (basic operations like
        // chatting will be allowed)
        UNKNOWN_FRIEND_PENDING_VALIDATION
    }


    /**
     * FriendConnectionManager object controlling connections with friends
     */
    private FriendConnectionManager friendConnectionManager;

    private PeerID ownPeerID;

    public ConnectionEstablishmentServerFSM(FriendConnectionManager friendConnectionManager, PeerID ownPeerID) {
        this.friendConnectionManager = friendConnectionManager;
        this.ownPeerID = ownPeerID;
    }

    @Override
    public State processMessage(State state, byte channel, Object message, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        switch (state) {

            case WAITING_DATA:
                // we expect to receive an ObjectListWrapper object containing the ID of the client and our own ID
                if (message instanceof ConnectionEstablishmentClientFSM.ConnectionRequest) {
                    ConnectionEstablishmentClientFSM.ConnectionRequest connectionRequest = (ConnectionEstablishmentClientFSM.ConnectionRequest) message;

                    if (!connectionRequest.serverPeerID.equals(ownPeerID)) {
                        // incorrect own id
                        return State.ERROR;
                    }

                    ConnectionResult connectionResult = friendConnectionManager.newRequestConnectionAsServer(connectionRequest.clientPeerID, ccp);
                    if (connectionResult != null) {
                        // ok to connect -> send result to client and finish
                        ccp.write(ChannelConstants.CONNECTION_ESTABLISHMENT_CHANNEL, connectionResult);
                        return State.SUCCESS;
                    } else {
                        // connection denied
                        return State.ERROR;
                    }
                } else if (message instanceof ConnectionEstablishmentClientFSM.TerminationMessage) {
                    // this connection process must terminate
                    return State.ERROR;
                } else {
                    // incorrect data format received
                    return State.ERROR;
                }

            default:
                // should never reach here
                ccp.disconnect();
                return State.ERROR;
        }
    }

    @Override
    public State processMessage(State state, byte channel, byte[] bytes, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        // unexpected data
        return State.ERROR;
    }

    @Override
    public State init(ChannelConnectionPoint ccp) {
        // request permission for proceeding to the PeerClientConnectionManager even though he will also grant us
        // access, we must notify him)
        return State.WAITING_DATA;
    }

    @Override
    public boolean isFinalState(State state, ChannelConnectionPoint ccp) {
        switch (state) {

            case SUCCESS:
                return true;

            case ERROR:
                ccp.disconnect();
                return true;

            default:
                return false;
        }
    }

    @Override
    public void disconnected(ChannelConnectionPoint ccp) {
        // ignore
    }


    @Override
    public void timedOut(State state, ChannelConnectionPoint ccp) {
        // disconnect
        ccp.disconnect();
    }
}