package jacz.peerengineservice.client.connection;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.ChannelConstants;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.channel.TimedChannelFSMAction;

import java.io.Serializable;

/**
 * This FSM allows negotiating with other peers who want to connect to our PeerClient. This FSM implements the server
 * part of that connection. First it receives the data of a new peer trying to connect to us, and then it answers to
 * that peer if it accepts the connection or not.
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

    public static final class PingRequest implements Serializable {

        public final byte channel;

        public PingRequest(byte channel) {
            this.channel = channel;
        }
    }


    /**
     * FriendConnectionManager object controlling connections with friends
     */
    private FriendConnectionManager friendConnectionManager;

    private PeerId ownPeerId;

    public ConnectionEstablishmentServerFSM(FriendConnectionManager friendConnectionManager, PeerId ownPeerId) {
        this.friendConnectionManager = friendConnectionManager;
        this.ownPeerId = ownPeerId;
    }

    @Override
    public State processMessage(State state, byte channel, Object message, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        switch (state) {

            case WAITING_DATA:
                // we expect to receive an ObjectListWrapper object containing the ID of the client and our own ID
                if (message instanceof ConnectionEstablishmentClientFSM.ConnectionRequest) {
                    ConnectionEstablishmentClientFSM.ConnectionRequest connectionRequest = (ConnectionEstablishmentClientFSM.ConnectionRequest) message;
                    if (!connectionRequest.serverPeerId.equals(ownPeerId)) {
                        // incorrect own id
                        return State.ERROR;
                    }
                    ConnectionResult connectionResult = friendConnectionManager.newRequestConnectionAsServer(connectionRequest.clientPeerId, ccp);
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
                } else if (message instanceof PingRequest) {
                    // a ping request, probably from a PortTestServer --> answer with a true and finish
                    PingRequest pingRequest = (PingRequest) message;
                    ccp.write(pingRequest.channel, ownPeerId);
                    return State.SUCCESS;
                } else {
                    // incorrect data format received
                    return State.ERROR;
                }

            default:
                // should never reach here
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