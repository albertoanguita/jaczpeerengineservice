package jacz.peerengineservice.client.connection;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.channel.TimedChannelFSMAction;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.util.ChannelConstants;
import jacz.util.log.ErrorLog;
import jacz.util.network.IP4Port;

import java.io.Serializable;

/**
 * This FSM negotiates the first part of a connection with a peerClient. Info about the PeerID is obtained.
 * <p/>
 * This FSM implements the client code. There is really no difference between client and server, just that the client
 * will initiate the conversation, and knows the PeerID of the server (supposedly)
 */
public class ConnectionEstablishmentClientFSM implements TimedChannelFSMAction<ConnectionEstablishmentClientFSM.State> {

    enum State {
        /**
         * Initial state: we have sent our data (our ID and the ID to which we pretend to connect) to the server peer
         */
        DATA_SENT,

        /**
         * The server peer has answered to our connection request successfully
         */
        SUCCESS_CORRECT_CONNECTION,

        SUCCESS_PENDING_VALIDATION,

        /**
         * The server peer has answered to our connection request with an error
         */
        ERROR
    }

    static final class ConnectionRequest implements Serializable {

        final PeerID clientPeerID;

        final PeerID serverPeerID;

        ConnectionRequest(PeerID clientPeerID, PeerID serverPeerID) {
            this.clientPeerID = clientPeerID;
            this.serverPeerID = serverPeerID;
        }
    }

    static final class TerminationMessage implements Serializable {
    }

    /**
     * FriendConnectionManager which is trying to connect to another peer (server peer)
     */
    private final FriendConnectionManager friendConnectionManager;

    /**
     * Our own ID
     */
    private final PeerID ownPeerID;

    /**
     * The ID of the peer we are trying to connect to
     */
    private final PeerID serverPeerID;

    private final IP4Port secondaryIP4Port;

    /**
     * Class constructor
     *
     * @param friendConnectionManager friendConnectionManager which is trying to connect to another peer
     * @param ownPeerID               our own ID
     * @param serverPeerID            the ID of the peer we are trying to connect to
     */
    public ConnectionEstablishmentClientFSM(
            FriendConnectionManager friendConnectionManager,
            PeerID ownPeerID,
            PeerID serverPeerID,
            IP4Port secondaryIP4Port) {
        this.friendConnectionManager = friendConnectionManager;
        this.ownPeerID = ownPeerID;
        this.serverPeerID = serverPeerID;
        this.secondaryIP4Port = secondaryIP4Port;
    }

    @Override
    public State processMessage(State state, byte channel, Object message, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        if (message instanceof ConnectionEstablishmentServerFSM.ConnectionResult) {
            ConnectionEstablishmentServerFSM.ConnectionResult connectionResult = (ConnectionEstablishmentServerFSM.ConnectionResult) message;
            switch (connectionResult) {

                case CORRECT:
                    // the server answered OK, we can establish connection
                    // report our PeerClient that we achieved connection with the server peer
                    return State.SUCCESS_CORRECT_CONNECTION;

                case UNKNOWN_FRIEND_PENDING_VALIDATION:
                    // the server peer reported that we are not his friend, so validation is pending
                    return State.SUCCESS_PENDING_VALIDATION;

                default:
                    // log error
                    ErrorLog.reportError(PeerClient.ERROR_LOG, "Incorrect data received from server peer when establishing connection", state, channel, message, ccp, connectionResult);
                    return State.ERROR;
            }
        } else {
            // log error
            ErrorLog.reportError(PeerClient.ERROR_LOG, "Incorrect data received from server peer when establishing connection", state, channel, message, ccp);
            return State.ERROR;
        }
    }

    @Override
    public State processMessage(State state, byte channel, byte[] data, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        // unexpected data
        return State.ERROR;
    }

    @Override
    public State init(ChannelConnectionPoint ccp) {
        // first we must ask permission to the PeerClientConnectionManager for proceeding with this new connection
        // if we are granted access, send our own PeerID to the server peer (only the client performs this in the INIT)
        // if the PeerClientConnectionManager does not grant permission, terminate the connection process
        ccp.write(ChannelConstants.CONNECTION_ESTABLISHMENT_CHANNEL, new ConnectionRequest(ownPeerID, serverPeerID));
        return State.DATA_SENT;
    }

    @Override
    public boolean isFinalState(State state, ChannelConnectionPoint ccp) {
        // SUCCESS and ERROR states are the final states. In both cases we invoke reportCompletion to our PeerClientConnectionManager
        switch (state) {

            case SUCCESS_CORRECT_CONNECTION:
                friendConnectionManager.connectionAsClientCompleted(serverPeerID, ccp, true);
                return true;

            case SUCCESS_PENDING_VALIDATION:
                friendConnectionManager.connectionAsClientCompleted(serverPeerID, ccp, false);
                return true;

            case ERROR:
                error();
                return true;

            default:
                return false;
        }
    }

    @Override
    public void disconnected(ChannelConnectionPoint ccp) {
        error();
    }


    @Override
    public void timedOut(State state, ChannelConnectionPoint ccp) {
        error();
    }

    /**
     * Reports the PeerClientConnectionManager that this connection is no longer ongoing
     */
    private void error() {
        friendConnectionManager.connectionAsClientFailed(serverPeerID, secondaryIP4Port, serverPeerID);
    }
}