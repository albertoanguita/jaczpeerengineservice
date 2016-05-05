package jacz.peerengineservice.client.connection.peers;


import com.neovisionaries.i18n.CountryCode;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.channel.TimedChannelFSMAction;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.client.connection.peers.kb.Management;
import jacz.peerengineservice.client.connection.peers.kb.PeerEntryFacade;
import jacz.peerengineservice.util.ChannelConstants;
import jacz.util.id.AlphaNumFactory;
import jacz.util.network.IP4Port;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.security.PublicKey;

/**
 * This FSM negotiates the first part of a connection with a peerClient. Info about the PeerId is obtained.
 * <p/>
 * This FSM implements the client code. There is really no difference between client and server, just that the client
 * will initiate the conversation, and knows the PeerId of the server (supposedly)
 */
public class ConnectionEstablishmentClientFSM implements TimedChannelFSMAction<ConnectionEstablishmentClientFSM.State> {

    enum State {
        /**
         * Initial state: we have sent our data (initial request) to the server peer
         */
        REQUEST_SENT,

        /**
         * The server peer has answered to our connection request successfully
         */
        CONNECTION_ACCEPTED,

        /**
         * The server peer denied our request, for some reason
         */
        CONNECTION_DENIED,

        /**
         * The server peer has answered to our connection request with an error message
         */
        ERROR
    }

    static final class ConnectionRequest implements Serializable {

        /**
         * Our peer id
         */
        final PeerId clientPeerId;

        /**
         * The target server peer id
         */
        final PeerId serverPeerId;

        /**
         * Our public key, for authentication
         */
        final PublicKey clientPublicKey;

        /**
         * The server secret retrieved from the central server (not too long ago, so the server peer also knows it)
         */
        final String centralServerSecret;

        /**
         * The server secret, encoded with our private key, so the server peer can authenticate us
         */
        final String encodedCentralServerSecret;

        /**
         * Our wish for regular connections
         */
        final boolean clientWishRegularConnections;

        /**
         * Our knowledge of the server peer wish for regular connections (null if unknown)
         */
        final Boolean serverWishRegularConnections;

        /**
         * Our own address, encoded as a string. Includes external and local address
         */
        final String clientAddress;

        /**
         * Our own main country, as stated in the configuration options
         */
        final CountryCode clientMainCountry;

        /**
         * Our knowledge of the server peer main country (null if unknown)
         */
        final CountryCode serverMainCountry;

        /**
         * Our relation with the server peer
         */
        final Management.Relationship clientToServerRelationship;

        ConnectionRequest(
                PeerId clientPeerId,
                PeerId serverPeerId,
                PublicKey clientPublicKey,
                String centralServerSecret,
                String encodedCentralServerSecret,
                boolean clientWishRegularConnections,
                String clientAddress,
                CountryCode clientMainCountry,
                PeerEntryFacade peerEntryFacade) {
            this.clientPeerId = clientPeerId;
            this.serverPeerId = serverPeerId;
            this.clientPublicKey = clientPublicKey;
            this.clientWishRegularConnections = clientWishRegularConnections;
            this.serverWishRegularConnections = peerEntryFacade.isWishForRegularConnections();
            this.clientAddress = clientAddress;
            this.clientMainCountry = clientMainCountry;
            this.centralServerSecret = centralServerSecret;
            this.encodedCentralServerSecret = encodedCentralServerSecret;
            this.serverMainCountry = peerEntryFacade.getMainCountry();
            this.clientToServerRelationship = peerEntryFacade.getRelationship();
        }

        @Override
        public String toString() {
            return "ConnectionRequest{" +
                    "clientPeerId=" + clientPeerId +
                    ", serverPeerId=" + serverPeerId +
                    //", clientPublicKey=" + clientPublicKey +
                    //", centralServerSecret='" + centralServerSecret + '\'' +
                    //", encodedCentralServerSecret='" + encodedCentralServerSecret + '\'' +
                    ", clientWishRegularConnections=" + clientWishRegularConnections +
                    ", serverWishRegularConnections=" + serverWishRegularConnections +
                    ", clientAddress='" + clientAddress + '\'' +
                    ", clientMainCountry=" + clientMainCountry +
                    ", serverMainCountry=" + serverMainCountry +
                    ", clientToServerRelationship=" + clientToServerRelationship +
                    '}';
        }
    }

    private final static Logger logger = LoggerFactory.getLogger(ConnectionEstablishmentClientFSM.class);

    private final String id;

    /**
     * FriendConnectionManager which is trying to connect to another peer (server peer)
     */
    private final PeerConnectionManager peerConnectionManager;

    /**
     * Our own ID
     */
    private final PeerId ownPeerId;

    /**
     * The ID of the peer we are trying to connect to
     */
    private final PeerId serverPeerId;

    private final PublicKey ownPublicKey;

    private final String centralServerSecret;

    private final String encodedCentralServerSecret;

    private final boolean clientWishRegularConnections;

    private final String clientAddress;

    private final CountryCode clientMainCountry;

    private final CountryCode serverMainCountry;

    private final PeerEntryFacade peerEntryFacade;


    private final IP4Port secondaryIP4Port;

    /**
     * Class constructor
     *  @param peerConnectionManager peerConnectionManager which is trying to connect to another peer
     * @param ownPeerId             our own ID
     * @param serverPeerId          the ID of the peer we are trying to connect to
     * @param ownPublicKey
     * @param centralServerSecret
     * @param encodedCentralServerSecret
     * @param clientWishRegularConnections
     * @param clientAddress
     * @param clientMainCountry
     * @param peerEntryFacade
     */
    public ConnectionEstablishmentClientFSM(
            PeerConnectionManager peerConnectionManager,
            PeerId ownPeerId,
            PeerId serverPeerId,
            PublicKey ownPublicKey,
            String centralServerSecret,
            String encodedCentralServerSecret,
            Boolean clientWishRegularConnections,
            String clientAddress,
            CountryCode clientMainCountry,
            PeerEntryFacade peerEntryFacade,
            IP4Port secondaryIP4Port) {
        this.id = AlphaNumFactory.getStaticId();
        this.peerConnectionManager = peerConnectionManager;
        this.ownPeerId = ownPeerId;
        this.serverPeerId = serverPeerId;
        this.ownPublicKey = ownPublicKey;
        this.centralServerSecret = centralServerSecret;
        this.encodedCentralServerSecret = encodedCentralServerSecret;
        this.clientWishRegularConnections = clientWishRegularConnections;
        this.clientAddress = clientAddress;
        this.clientMainCountry = clientMainCountry;
        this.serverMainCountry = peerEntryFacade.getMainCountry();
        this.peerEntryFacade = peerEntryFacade;
        this.secondaryIP4Port = secondaryIP4Port;
    }

    @Override
    public State processMessage(State state, byte channel, Object message, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        if (message instanceof ConnectionEstablishmentServerFSM.ConnectionResult) {
            ConnectionEstablishmentServerFSM.ConnectionResult connectionResult = (ConnectionEstablishmentServerFSM.ConnectionResult) message;
            logMessage("Connection request to " + serverPeerId + " answered: " + connectionResult);
            peerConnectionManager.processExtraPeersInfo(connectionResult.tryThesePeers, serverMainCountry);
            switch (connectionResult.connectionResultType) {

                case OK:
                    if (connectionResult.responseDetail instanceof ConnectionEstablishmentServerFSM.DetailAcceptedConnection) {
                        ConnectionEstablishmentServerFSM.DetailAcceptedConnection detailAcceptedConnection = (ConnectionEstablishmentServerFSM.DetailAcceptedConnection) connectionResult.responseDetail;
                        return processDetailAcceptedConnection(detailAcceptedConnection, ccp);
                    } else {
                        return State.ERROR;
                    }
                case INCORRECT_SERVER_INFO:
                    if (connectionResult.responseDetail instanceof ConnectionEstablishmentServerFSM.DetailCorrectedInformation) {
                        ConnectionEstablishmentServerFSM.DetailCorrectedInformation detailCorrectedInformation = (ConnectionEstablishmentServerFSM.DetailCorrectedInformation) connectionResult.responseDetail;
                        peerConnectionManager.correctedPeerInformation(serverPeerId, detailCorrectedInformation);
                        return State.CONNECTION_DENIED;
                    } else {
                        return State.ERROR;
                    }
                case WRONG_AUTHENTICATION_ID_KEY_NOT_MATCHING:
                case WRONG_AUTHENTICATION_INVALID_CENTRAL_SERVER_SECRET:
                case WRONG_AUTHENTICATION_FALSE_PEER:
                    return State.CONNECTION_DENIED;
                case REGULAR_SPOTS_TEMPORARILY_FULL:
                    peerEntryFacade.openTransaction();
                    peerEntryFacade.setRelationshipToUs(Management.Relationship.REGULAR);
                    peerEntryFacade.setWishForRegularConnections(Management.ConnectionWish.NOT_NOW);
                    peerEntryFacade.updateConnectionAttempt();
                    peerEntryFacade.commitTransaction();
                    return State.CONNECTION_DENIED;
                case BLOCKED:
                    peerEntryFacade.openTransaction();
                    peerEntryFacade.setRelationshipToUs(Management.Relationship.BLOCKED);
                    peerEntryFacade.updateConnectionAttempt();
                    peerEntryFacade.commitTransaction();
                    return State.CONNECTION_DENIED;
                case DENY:
                    // peer no longer available, no need to update any info
                    return State.CONNECTION_DENIED;
                case ALREADY_CONNECTED:
                    return State.CONNECTION_DENIED;
                default:
                    // log error
                    PeerClient.reportError("Incorrect data received from server peer when establishing connection", state, channel, message, ccp, connectionResult);
                    return State.ERROR;
            }
        } else {
            // log error
            logMessage("Invalid connection result received from " + serverPeerId);
            PeerClient.reportError("Incorrect data received from server peer when establishing connection", state, channel, message, ccp);
            return State.ERROR;
        }
    }

    private State processDetailAcceptedConnection(
            ConnectionEstablishmentServerFSM.DetailAcceptedConnection detailAcceptedConnection,
            ChannelConnectionPoint ccp) {
        // todo check server auth (@CONNECTION-AUTH@)
        // send confirmation message
        ccp.write(ChannelConstants.CONNECTION_ESTABLISHMENT_CHANNEL, true);
        peerConnectionManager.connectionAsClientCompleted(serverPeerId, ccp, detailAcceptedConnection.serverToClientRelationship, peerEntryFacade.getMainCountry());
        return State.CONNECTION_ACCEPTED;
    }

    @Override
    public State processMessage(State state, byte channel, byte[] data, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        // unexpected data
        return State.ERROR;
    }

    @Override
    public State init(ChannelConnectionPoint ccp) {
        // first we must ask permission to the PeerClientConnectionManager for proceeding with this new connection
        // if we are granted access, send our own PeerId to the server peer (only the client performs this in the INIT)
        // if the PeerClientConnectionManager does not grant permission, terminate the connection process
        ConnectionRequest connectionRequest = new ConnectionRequest(
                ownPeerId,
                serverPeerId,
                ownPublicKey,
                centralServerSecret,
                encodedCentralServerSecret,
                clientWishRegularConnections,
                clientAddress,
                clientMainCountry,
                peerEntryFacade);
        logMessage("Connection request sent to " + serverPeerId + ": " + connectionRequest);
        ccp.write(ChannelConstants.CONNECTION_ESTABLISHMENT_CHANNEL, connectionRequest);
        return State.REQUEST_SENT;
    }

    @Override
    public boolean isFinalState(State state, ChannelConnectionPoint ccp) {
        // SUCCESS and ERROR states are the final states. In both cases we invoke reportCompletion to our PeerClientConnectionManager
        switch (state) {

            case CONNECTION_ACCEPTED:
                logMessage("connection succeeded");
                return true;

            case CONNECTION_DENIED:
            case ERROR:
                logMessage("connection failed");
                connectionFailed();
                ccp.disconnect();
                return true;

            default:
                return false;
        }
    }

    @Override
    public void disconnected(ChannelConnectionPoint ccp) {
        logMessage("disconnected");
        connectionFailed();
    }


    @Override
    public void timedOut(State state, ChannelConnectionPoint ccp) {
        logMessage("timed out");
        connectionFailed();
    }

    /**
     * Reports the PeerClientConnectionManager that this connection is no longer ongoing
     */
    private void connectionFailed() {
        peerConnectionManager.connectionAsClientFailed(serverPeerId, secondaryIP4Port, serverPeerId);
    }

    private void logMessage(String message) {
        logger.info("{id=" + id + "} " + message);
    }
}