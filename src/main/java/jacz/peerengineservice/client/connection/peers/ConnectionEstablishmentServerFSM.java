package jacz.peerengineservice.client.connection.peers;


import com.neovisionaries.i18n.CountryCode;
import org.aanguita.jtcpserver.channel.ChannelConnectionPoint;
import org.aanguita.jtcpserver.channel.TimedChannelFSMAction;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.connection.peers.kb.Management;
import jacz.peerengineservice.util.ChannelConstants;
import org.aanguita.jacuzzi.id.AlphaNumFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.List;

/**
 * This FSM allows negotiating with other peers who want to connect to our PeerClient. This FSM implements the server
 * part of that connection. First it receives the data of a new peer trying to connect to us, and then it answers to
 * that peer if it accepts the connection or not.
 * <p/>
 * todo avoid serializing objects, it might break legacy in the future. Better serialize data inside those objects and rebuild
 */
public class ConnectionEstablishmentServerFSM implements TimedChannelFSMAction<ConnectionEstablishmentServerFSM.State> {

    enum State {
        // Initial state: waiting for peers to try to connect to us. Here, we will receive the data from peers
        // attempting connection to us
        WAITING_FOR_REQUEST,
        // The client request was accepted. Waiting for client to confirm the connection
        REQUEST_ACCEPTED,
        // The client confirmed the connection
        CONNECTION_SUCCESSFUL,
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
        // - the peer id does not match with the given public key
        WRONG_AUTHENTICATION_ID_KEY_NOT_MATCHING,
        // - the given central server secret is not accepted
        WRONG_AUTHENTICATION_INVALID_CENTRAL_SERVER_SECRET,
        // - the central server secret was not encoded with the corresponding private key
        WRONG_AUTHENTICATION_FALSE_PEER,
        // we would like to accept, but we are full for his offer
        REGULAR_SPOTS_TEMPORARILY_FULL,
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

        final List<PeersLookingForRegularConnectionsRecord.PeerRecord> tryThesePeers;

        public ConnectionResult(ConnectionResultType connectionResultType, List<PeersLookingForRegularConnectionsRecord.PeerRecord> tryThesePeers) {
            this(connectionResultType, null, tryThesePeers);
        }

        public ConnectionResult(ConnectionResultType connectionResultType, ResponseDetail responseDetail, List<PeersLookingForRegularConnectionsRecord.PeerRecord> tryThesePeers) {
            this.connectionResultType = connectionResultType;
            this.responseDetail = responseDetail;
            this.tryThesePeers = tryThesePeers;
        }

        @Override
        public String toString() {
            return "ConnectionResult{" +
                    "connectionResultType=" + connectionResultType +
                    ", responseDetail=" + responseDetail +
                    ", tryThesePeers=" + tryThesePeers +
                    '}';
        }
    }

    static class ResponseDetail implements Serializable {

        final PublicKey serverPublicKey;

        final Management.Relationship serverToClientRelationship;

        public ResponseDetail(PublicKey serverPublicKey, Management.Relationship serverToClientRelationship) {
            this.serverPublicKey = serverPublicKey;
            this.serverToClientRelationship = serverToClientRelationship;
        }

        @Override
        public String toString() {
            return "ResponseDetail{" +
                    "serverPublicKey=..." +
                    ", serverToClientRelationship=" + serverToClientRelationship +
                    '}';
        }
    }

    /**
     * The response when the request contained errors about what the client thought he knew about us
     */
    static final class DetailCorrectedInformation extends ResponseDetail {

        final PeerId serverPeerId;

        final boolean serverWishRegularConnections;

        final CountryCode serverMainCountry;

        public DetailCorrectedInformation(
                PublicKey serverPublicKey,
                boolean serverWishRegularConnections,
                Management.Relationship serverToClientRelationship,
                PeerId serverPeerId,
                CountryCode serverMainCountry) {
            super(serverPublicKey, serverToClientRelationship);
            this.serverPeerId = serverPeerId;
            this.serverWishRegularConnections = serverWishRegularConnections;
            this.serverMainCountry = serverMainCountry;
        }
    }

    static final class DetailAcceptedConnection extends ResponseDetail {

        final String encodedCentralServerSecret;

        public DetailAcceptedConnection(
                PublicKey serverPublicKey,
                Management.Relationship serverToClientRelationship,
                String encodedCentralServerSecret) {
            super(serverPublicKey, serverToClientRelationship);
            this.encodedCentralServerSecret = encodedCentralServerSecret;
        }
    }

    public static final class PingRequest implements Serializable {

        public final byte channel;

        public PingRequest(byte channel) {
            this.channel = channel;
        }
    }

    private final static Logger logger = LoggerFactory.getLogger(ConnectionEstablishmentServerFSM.class);

    private final String id;

    /**
     * FriendConnectionManager object controlling connections with friends
     */
    private final PeerConnectionManager peerConnectionManager;

    private final PeerId ownPeerId;

    /**
     * Once we receive the client's connection request, we store it in case the connection is confirmed
     */
    private ConnectionEstablishmentClientFSM.ConnectionRequest clientConnectionRequest;

    public ConnectionEstablishmentServerFSM(PeerConnectionManager peerConnectionManager, PeerId ownPeerId) {
        this.id = AlphaNumFactory.getStaticId();
        this.peerConnectionManager = peerConnectionManager;
        this.ownPeerId = ownPeerId;
    }

    @Override
    public State processMessage(State state, byte channel, Object message, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        if (state == State.WAITING_FOR_REQUEST) {
            if (message instanceof ConnectionEstablishmentClientFSM.ConnectionRequest) {
                clientConnectionRequest = (ConnectionEstablishmentClientFSM.ConnectionRequest) message;
                return processInitialRequest(clientConnectionRequest, ccp);
            } else if (message instanceof PingRequest) {
                // a ping request, probably from a PortTestServer --> answer with a true and finish
                PingRequest pingRequest = (PingRequest) message;
                logMessage("Ping request received");
                ccp.write(pingRequest.channel, ownPeerId.toString());
                return State.PING_ANSWERED;
            } else {
                // incorrect data format received
                return State.ERROR;
            }
        } else if (state == State.REQUEST_ACCEPTED) {
            if (message instanceof Boolean) {
                Boolean clientConfirmation = (Boolean) message;
                if (clientConfirmation) {
                    // the client confirms the connection
                    logMessage("Client confirmed connection");
                    peerConnectionManager.connectionAsServerCompleted(clientConnectionRequest.getClientPeerId(), ccp, clientConnectionRequest.clientMainCountry);
                    return State.CONNECTION_SUCCESSFUL;
                } else {
                    // the client dismissed this connection due to failed authentication
                    logMessage("Client denied connection");
                    // todo notify client (not currently used) (@CONNECTION-AUTH@)
                    return State.ERROR;
                }
            } else {
                // incorrect data format received
                logMessage("Invalid confirmation message");
                return State.ERROR;
            }
        } else {
            // should never reach here
            return State.ERROR;
        }
    }

    private State processInitialRequest(ConnectionEstablishmentClientFSM.ConnectionRequest connectionRequest, ChannelConnectionPoint ccp) {
        logMessage("Connection request received: " + connectionRequest);
        ConnectionResult connectionResult = peerConnectionManager.newRequestConnectionAsServer(connectionRequest);
        if (connectionResult != null) {
            logMessage("Connection result is " + connectionResult.connectionResultType.name());
            ccp.write(ChannelConstants.CONNECTION_ESTABLISHMENT_CHANNEL, connectionResult);
            if (connectionResult.connectionResultType == ConnectionResultType.OK) {
                return State.REQUEST_ACCEPTED;
            } else {
                return State.REQUEST_DENIED;
            }
        } else {
            // connection failed
            logMessage("Connection denied due to invalid request");
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

            case CONNECTION_SUCCESSFUL:
            case PING_ANSWERED:
                logMessage("success");
                return true;

            case REQUEST_DENIED:
            case ERROR:
                logMessage("disconnect");
                ccp.disconnect();
                return true;

            default:
                return false;
        }
    }

    @Override
    public void disconnected(ChannelConnectionPoint ccp) {
        logMessage("disconnected");
        // ignore
    }

    @Override
    public void raisedUnhandledException(Exception e, ChannelConnectionPoint ccp) {
        logMessage("unhandled exception");
        e.printStackTrace();
        ccp.disconnect();
    }

    @Override
    public void timedOut(State state, ChannelConnectionPoint ccp) {
        // disconnect
        logMessage("timed out");
        ccp.disconnect();
    }

    private void logMessage(String message) {
        logger.info("{id=" + id + "} " + message);
    }
}