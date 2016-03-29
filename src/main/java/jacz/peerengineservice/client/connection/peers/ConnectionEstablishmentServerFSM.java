package jacz.peerengineservice.client.connection.peers;


import com.neovisionaries.i18n.CountryCode;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.channel.TimedChannelFSMAction;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.connection.peers.kb.Management;
import jacz.peerengineservice.util.ChannelConstants;

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
        WAITING_FOR_REQUEST,
        // Connection process was successful
        REQUEST_ACCEPTED,
        // the request was denied
        REQUEST_DENIED,
        // we responded to a ping request -> finished
        PING_ANSWERED,
        // error in the connection -> must disconnect
        ERROR
    }

    public enum ConnectionResultType {

        // we accept the connection request
        OK,
        // we are not who the requester thinks we are -> deny
        INCORRECT_SERVER_INFO,
        // the requester failed in the authentication process -> validation could not be done -> deny
        WRONG_AUTHENTICATION,
        // we would like to accept, but we are full for his offer
        REGULAR_SPOTS_TEMPORARILY_FULL,
        // this client does not accept regular connections
        REJECT_REGULARS,
        // this client has blocked the requester
        BLOCKED,
        // we are no longer accepting any connections
        DENY,
        // we are already connected/connecting to the requester, and we have higher priority
        ALREADY_CONNECTED
    }

    static class ConnectionResult implements Serializable {

        final ConnectionResultType connectionResultType;

        final ResponseDetail responseDetail;

        public ConnectionResult(ConnectionResultType connectionResultType) {
            this.connectionResultType = connectionResultType;
            this.responseDetail = null;
        }

        public ConnectionResult(ConnectionResultType connectionResultType, ResponseDetail responseDetail) {
            this.connectionResultType = connectionResultType;
            this.responseDetail = responseDetail;
        }
    }

    static class ResponseDetail implements Serializable {
    }

    /**
     * The response when the request contained errors about what the client thought he knew about us
     */
    static final class DetailCorrectedInformation extends ResponseDetail {

        final PeerId serverPeerId;

        final String serverPublicKey;

        final Management.ConnectionWish serverWishRegularConnections;

        final CountryCode serverMainCountry;

        final Management.Relationship serverToClientRelationship;

        public DetailCorrectedInformation(
                PeerId serverPeerId,
                String serverPublicKey,
                Management.ConnectionWish serverWishRegularConnections,
                CountryCode serverMainCountry,
                Management.Relationship serverToClientRelationship) {
            this.serverPeerId = serverPeerId;
            this.serverPublicKey = serverPublicKey;
            this.serverWishRegularConnections = serverWishRegularConnections;
            this.serverMainCountry = serverMainCountry;
            this.serverToClientRelationship = serverToClientRelationship;
        }
    }

    static final class DetailAcceptedConnection extends ResponseDetail {

        final Management.ConnectionWish serverWishRegularConnections;

        final String serverPublicKey;

        final Management.Relationship serverToClientRelationship;

        final String encodedServerSecret;

        public DetailAcceptedConnection(
                Management.ConnectionWish serverWishRegularConnections,
                String serverPublicKey,
                Management.Relationship serverToClientRelationship,
                String encodedServerSecret) {
            this.serverWishRegularConnections = serverWishRegularConnections;
            this.serverPublicKey = serverPublicKey;
            this.serverToClientRelationship = serverToClientRelationship;
            this.encodedServerSecret = encodedServerSecret;
        }
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
    private PeerConnectionManager peerConnectionManager;

    private PeerId ownPeerId;

    public ConnectionEstablishmentServerFSM(PeerConnectionManager peerConnectionManager, PeerId ownPeerId) {
        this.peerConnectionManager = peerConnectionManager;
        this.ownPeerId = ownPeerId;
    }

    @Override
    public State processMessage(State state, byte channel, Object message, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        if (state == State.WAITING_FOR_REQUEST) {
            if (message instanceof ConnectionEstablishmentClientFSM.ConnectionRequest2) {
                ConnectionEstablishmentClientFSM.ConnectionRequest2 connectionRequest = (ConnectionEstablishmentClientFSM.ConnectionRequest2) message;
                return processInitialRequest(connectionRequest);
            } else if (message instanceof ConnectionEstablishmentClientFSM.TerminationMessage) {
                // todo do we need this??
                // this connection process must terminate
                return State.ERROR;
            } else if (message instanceof PingRequest) {
                // a ping request, probably from a PortTestServer --> answer with a true and finish
                PingRequest pingRequest = (PingRequest) message;
                ccp.write(pingRequest.channel, ownPeerId);
                return State.PING_ANSWERED;
            } else {
                // incorrect data format received
                return State.ERROR;
            }
        } else {
            // should never reach here
            return State.ERROR;
        }
    }

    private State processInitialRequest(ConnectionEstablishmentClientFSM.ConnectionRequest2 connectionRequest) {
        if (!connectionRequest.serverPeerId.equals(ownPeerId)) {
            // incorrect own id
            return State.ERROR;
        }
        ConnectionResult connectionResult = peerConnectionManager.newRequestConnectionAsServer(connectionRequest.clientPeerId, ccp);
        if (connectionResult != null) {
            // ok to connect -> send result to client and finish
            ccp.write(ChannelConstants.CONNECTION_ESTABLISHMENT_CHANNEL, connectionResult);
            return State.SUCCESS;
        } else {
            // connection denied
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
        return State.WAITING_FOR_REQUEST;
    }

    @Override
    public boolean isFinalState(State state, ChannelConnectionPoint ccp) {
        switch (state) {

            case REQUEST_ACCEPTED:
            case REQUEST_DENIED:
            case PING_ANSWERED:
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