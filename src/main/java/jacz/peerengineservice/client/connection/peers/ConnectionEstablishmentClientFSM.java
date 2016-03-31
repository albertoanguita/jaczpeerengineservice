package jacz.peerengineservice.client.connection.peers;


import com.neovisionaries.i18n.CountryCode;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.channel.TimedChannelFSMAction;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.client.connection.peers.kb.Management;
import jacz.peerengineservice.client.connection.peers.kb.PeerEntryFacade;
import jacz.peerengineservice.util.ChannelConstants;
import jacz.util.network.IP4Port;

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

        final PeerId clientPeerId;

        final PeerId serverPeerId;

        ConnectionRequest(PeerId clientPeerId, PeerId serverPeerId) {
            this.clientPeerId = clientPeerId;
            this.serverPeerId = serverPeerId;
        }
    }

    static final class ConnectionRequest2 implements Serializable {

        final PeerId clientPeerId;

        final PeerId serverPeerId;

        final PublicKey clientPublicKey;

        final String centralServerSecret;

        final String encodedCentralServerSecret;

        final boolean clientWishRegularConnections;

        final boolean serverWishRegularConnections;

        final String clientAddress;

        final CountryCode clientMainCountry;

        final CountryCode serverMainCountry;

        final Management.Relationship clientToServerRelationship;

        ConnectionRequest2(
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
            this.serverWishRegularConnections = peerEntryFacade.getWishForConnections();
            this.clientAddress = clientAddress;
            this.clientMainCountry = clientMainCountry;
            this.centralServerSecret = centralServerSecret;
            this.encodedCentralServerSecret = encodedCentralServerSecret;
            this.serverMainCountry = peerEntryFacade.getMainCountry();
            this.clientToServerRelationship = peerEntryFacade.getRelationship();
        }
    }

    static final class TerminationMessage implements Serializable {
    }

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

    private final IP4Port secondaryIP4Port;

    /**
     * Class constructor
     *
     * @param peerConnectionManager peerConnectionManager which is trying to connect to another peer
     * @param ownPeerId             our own ID
     * @param serverPeerId          the ID of the peer we are trying to connect to
     */
    public ConnectionEstablishmentClientFSM(
            PeerConnectionManager peerConnectionManager,
            PeerId ownPeerId,
            PeerId serverPeerId,
            IP4Port secondaryIP4Port) {
        this.peerConnectionManager = peerConnectionManager;
        this.ownPeerId = ownPeerId;
        this.serverPeerId = serverPeerId;
        this.secondaryIP4Port = secondaryIP4Port;
    }

    @Override
    public State processMessage(State state, byte channel, Object message, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        if (message instanceof ConnectionEstablishmentServerFSM.ConnectionResult) {
            ConnectionEstablishmentServerFSM.ConnectionResult connectionResult = (ConnectionEstablishmentServerFSM.ConnectionResult) message;
            switch (connectionResult.connectionResultType) {

                case OK:
                    if (connectionResult.responseDetail instanceof ConnectionEstablishmentServerFSM.DetailAcceptedConnection) {
                        ConnectionEstablishmentServerFSM.DetailAcceptedConnection detailAcceptedConnection = (ConnectionEstablishmentServerFSM.DetailAcceptedConnection) connectionResult.responseDetail;
                        // todo check server auth
                        peerConnectionManager.connectionAsClientCompleted(serverPeerId, ccp, detailAcceptedConnection);
                        return State.CONNECTION_ACCEPTED;
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
                case WRONG_AUTHENTICATION:
                    return State.CONNECTION_DENIED;
                case REGULAR_SPOTS_TEMPORARILY_FULL:
                    // todo update pkb
                    return State.CONNECTION_DENIED;
                case REJECT_REGULARS:
                    // todo update pkb
                    return State.CONNECTION_DENIED;
                case BLOCKED:
                    // todo update pkb
                    return State.CONNECTION_DENIED;
                case DENY:
                    // todo update pkb
                    return State.CONNECTION_DENIED;
                case ALREADY_CONNECTED:
                    // todo notify manager
                    return State.CONNECTION_DENIED;
                default:
                    // log error
                    PeerClient.reportError("Incorrect data received from server peer when establishing connection", state, channel, message, ccp, connectionResult);
                    return State.ERROR;
            }
        } else {
            // log error
            PeerClient.reportError("Incorrect data received from server peer when establishing connection", state, channel, message, ccp);
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
        // if we are granted access, send our own PeerId to the server peer (only the client performs this in the INIT)
        // if the PeerClientConnectionManager does not grant permission, terminate the connection process
        ccp.write(ChannelConstants.CONNECTION_ESTABLISHMENT_CHANNEL, new ConnectionRequest(ownPeerId, serverPeerId));
        return State.REQUEST_SENT;
    }

    @Override
    public boolean isFinalState(State state, ChannelConnectionPoint ccp) {
        // SUCCESS and ERROR states are the final states. In both cases we invoke reportCompletion to our PeerClientConnectionManager
        switch (state) {

            case CONNECTION_ACCEPTED:
                peerConnectionManager.connectionAsClientCompleted(serverPeerId, ccp, true);
                return true;

            case CONNECTION_DENIED:
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
        // todo check when this must be run
        peerConnectionManager.connectionAsClientFailed(serverPeerId, secondaryIP4Port, serverPeerId);
    }
}