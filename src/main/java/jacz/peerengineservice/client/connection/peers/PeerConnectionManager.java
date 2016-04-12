package jacz.peerengineservice.client.connection.peers;

import com.neovisionaries.i18n.CountryCode;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.clientserver.client.ClientModule;
import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerEncryption;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.GeneralEvents;
import jacz.peerengineservice.client.PeerClientPrivateInterface;
import jacz.peerengineservice.client.connection.*;
import jacz.peerengineservice.client.connection.peers.kb.Management;
import jacz.peerengineservice.client.connection.PeerAddress;
import jacz.peerengineservice.client.connection.peers.kb.PeerEntryFacade;
import jacz.peerengineservice.client.connection.peers.kb.PeerKnowledgeBase;
import jacz.peerengineservice.util.ChannelConstants;
import jacz.peerengineservice.util.PeerRelationship;
import jacz.util.network.IP4Port;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is in charge of achieving connections with other peers. It takes care of maintaining the peer knowledge
 * base and checking authentication. Changes in connection config are done through here.
 * <p/>
 * todo add connection attempts info
 * <p/>
 * todo disconnect from blocked peers. Make another evolving state class that, when we add a blocked peer, starts
 * checking regularly (starts every 5 seconds). Each check we double the check time. Finally, when we reach 5 minutes,
 * we stop checking
 * It might not be possible to modify timers while running transitions... check timer implementation
 * <p/>
 * todo synch
 */
public class PeerConnectionManager {

    // todo remove
    private static final String FAKE_SERVER_SECRET = "@FAKE@";
    private static final String FAKE_ENCODED_SERVER_SECRET = "@FAKE@";

    /**
     * Five seconds for connection (should be enough, increase if needed)
     */
    private static final long CONNECTION_TIMEOUT = 5000;

    /**
     * Our own ID. Cannot be modified after construction time
     */
    private final PeerId ownPeerId;

    /**
     * Our public key for authentication with other peers
     */
    private final PeerEncryption ownPeerEncryption;

    /**
     * Our peer address
     */
    private PeerAddress ownPeerAddress;

    /**
     * URL (including version) of the server
     */
    private final String serverURL;

    private final PeerKnowledgeBase peerKnowledgeBase;

    private final PeerConnectionConfig peerConnectionConfig;

    private final FavoritesConnectionManager favoritesConnectionManager;

    private final RegularsConnectionManager regularsConnectionManager;

    /**
     * Current ongoing connection trials with other friend peers, we act as CLIENT. It allows to not connect to the same peer twice
     * (it only stores ongoing connections in which we are clients, it does not consider other peers connecting to us)
     * <p/>
     * All accessions to this resource are synchronized, allowing us to correctly synchronize connections with peers
     * and avoid duplicate connections.
     * <p/>
     * Connections as client first go here. Then, atomically, go to connected peers
     */
    private final Set<PeerId> ongoingClientConnections;

    private final ConnectedPeers connectedPeers;

    private final AtomicBoolean wishForConnection;

    private final PeerClientPrivateInterface peerClientPrivateInterface;

    private final PeerClientConnectionManager peerClientConnectionManager;

    private final GeneralEvents generalEvents;


    public PeerConnectionManager(
            PeerId ownPeerId,
            PeerEncryption ownPeerEncryption,
            String serverURL,
            PeerConnectionConfig peerConnectionConfig,
            String peerKnowledgeBasePath,
            ConnectedPeers connectedPeers,
            PeerClientPrivateInterface peerClientPrivateInterface,
            PeerClientConnectionManager peerClientConnectionManager,
            GeneralEvents generalEvents) {
        this.ownPeerId = ownPeerId;
        this.ownPeerEncryption = ownPeerEncryption;
        this.ownPeerAddress = null;
        this.serverURL = serverURL;
        this.peerKnowledgeBase = new PeerKnowledgeBase(peerKnowledgeBasePath);
        this.peerConnectionConfig = peerConnectionConfig;
        this.favoritesConnectionManager = new FavoritesConnectionManager(this, peerKnowledgeBase);
        this.regularsConnectionManager = new RegularsConnectionManager(this, peerKnowledgeBase, connectedPeers, peerConnectionConfig);
        this.ongoingClientConnections = new HashSet<>();
        this.connectedPeers = connectedPeers;
        this.wishForConnection = new AtomicBoolean(false);
        this.peerClientPrivateInterface = peerClientPrivateInterface;
        this.peerClientConnectionManager = peerClientConnectionManager;
        this.generalEvents = generalEvents;
    }

    public void setWishForConnect(boolean enabled) {
        wishForConnection.set(enabled);
        if (enabled) {
            // gather our own peer address
            ownPeerAddress = peerClientConnectionManager.getPeerAddress();
        }
//        favoritesConnectionManager.setConnectionGoal(enabled);
//        regularsConnectionManager.setConnectionGoal(enabled);
    }

    public void setWishForRegularsConnection(boolean enabled) {
        peerConnectionConfig.setWishRegularConnections(enabled);
//        regularsConnectionManager.connectionConfigHasChanged();
    }

    public void setMaxRegularConnections(int maxRegularConnections) {
        peerConnectionConfig.setMaxRegularConnections(maxRegularConnections);
//        regularsConnectionManager.connectionConfigHasChanged();
    }

    boolean discardConnectionAttempt(PeerEntryFacade peerEntryFacade) {
        // discard those who:
        // - is now connected
        // - are in ongoing connections
        // - do not temporarily want to connect with us and the last connection attempt is recent
        // - do not definitely want to connect with us and the last connection attempt is very old
        // todo
        return false;
    }

    synchronized void askForMoreRegularPeers(CountryCode country) {
        try {
            ServerAPI.InfoResponse infoResponse = ServerAPI.regularPeersRequest(serverURL, new ServerAPI.RegularPeersRequest(country));
            for (ServerAPI.PeerIdInfo peerIdInfo : infoResponse.getPeerIdInfoList()) {
                // update info for each peer
                // todo use a transaction
                PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(peerIdInfo.getPeerId());
                peerEntryFacade.setMainCountry(peerIdInfo.getMainCountry());
                if (peerIdInfo.isWishRegularConnections()) {
                    peerEntryFacade.setWishForRegularConnections(Management.ConnectionWish.YES);
                } else {
                    peerEntryFacade.setWishForRegularConnections(Management.ConnectionWish.NO);
                }
                peerEntryFacade.setConnected(true);
                IP4Port externalAddress = new IP4Port(peerIdInfo.getExternalIPAddress(), peerIdInfo.getExternalMainServerPort());
                IP4Port localAddress = new IP4Port(peerIdInfo.getLocalIPAddress(), peerIdInfo.getLocalMainServerPort());
                peerEntryFacade.setPeerAddress(new PeerAddress(externalAddress, localAddress));
            }
        } catch (IOException | ServerAccessException e) {
            // error connecting with the server -> will retry later
        }
    }

    synchronized boolean attemptConnection(PeerEntryFacade peerEntryFacade) {
        // A client module is created for each received peer. If connection is achieved, a
        // Connection Client FSM is created. The init method in the FSM will take care
        // of checking if it is actually possible to proceed with the connection

        if (connectedPeers.isConnectedPeer(peerEntryFacade.getPeerId()) || ongoingClientConnections.contains(peerEntryFacade.getPeerId())) {
            // check that we are not connected to this peer, or trying to connect to it
            return true;
        }

        IP4Port externalIP4Port = peerEntryFacade.getPeerAddress().getExternalAddress();
        IP4Port localIP4Port = peerEntryFacade.getPeerAddress().getLocalAddress();
        try {
            // first try public connection
            tryConnection(externalIP4Port, peerEntryFacade.getPeerId(), localIP4Port);
            return true;
        } catch (IOException e) {
            // if this didn't work, try local connection (if exists)
            try {
                tryConnection(localIP4Port, peerEntryFacade.getPeerId(), null);
                return true;
            } catch (IOException e2) {
                // peer not available or wrong/outdated peer data
                return false;
            }
        }
    }

    private void tryConnection(IP4Port ip4Port, PeerId serverPeerId, IP4Port secondaryIP4Port) throws IOException {
        if (ip4Port == null) {
            throw new IOException("No address");
        } else {
            ClientModule friendClientModule = new ClientModule(ip4Port, new PeerClientConnectionToClientChannelActionImpl(this), PeerClientConnectionManager.generateConcurrentChannelSets());
            // first try public connection
            ChannelConnectionPoint ccp = friendClientModule.connect();
            contactWithPeerAchieved(ccp, true, serverPeerId, secondaryIP4Port);
            friendClientModule.start();
        }
    }


    public synchronized void reportClientConnectedToOurPeerServer(ChannelConnectionPoint ccp) {
        contactWithPeerAchieved(ccp, false, null, null);
    }

    private void contactWithPeerAchieved(ChannelConnectionPoint ccp, boolean isClient, PeerId remotePeerId, IP4Port secondaryIP4Port) {
        // depending on whether we are client or server, the corresponding FSM for establishing connection with the
        // other peer is created. This FSMs will send and receive data about the ID of the other peer to accept or
        // revoke the connection
        pauseChannelsExceptConnection(ccp);
        if (isClient) {
            // first mark as ongoing client connection
            ongoingClientConnections.add(remotePeerId);
            PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(remotePeerId);
            ccp.registerTimedFSM(
                    new ConnectionEstablishmentClientFSM(
                            this,
                            ownPeerId,
                            remotePeerId,
                            ownPeerEncryption.getPublicKey(),
                            FAKE_SERVER_SECRET,
                            FAKE_ENCODED_SERVER_SECRET,
                            peerConnectionConfig.isWishRegularConnections(),
                            ownPeerAddress.serialize(),
                            peerConnectionConfig.getMainCountry(),
                            peerEntryFacade,
                            secondaryIP4Port),
                    CONNECTION_TIMEOUT,
                    "ConnectionEstablishmentClientFSM",
                    ChannelConstants.CONNECTION_ESTABLISHMENT_CHANNEL);
        } else {
            ccp.registerTimedFSM(new ConnectionEstablishmentServerFSM(this, ownPeerId), CONNECTION_TIMEOUT, "ConnectionEstablishmentServerFSM", ChannelConstants.CONNECTION_ESTABLISHMENT_CHANNEL);
        }
    }

    /**
     * This methods sets all channels in a ChannelConnectionPoint to pause, except the channels used in the
     * connection establishment process
     *
     * @param ccp ChannelConnectionPoint object whose channels must be paused
     */
    private static void pauseChannelsExceptConnection(ChannelConnectionPoint ccp) {
        // currently we use only two channel sets: one for the connection process and request dispatcher, and one
        // for everything else (data streaming and custom FSMs). Simply pause the data streaming channel
        for (byte channel : getConcurrentChannelsExceptConnection()) {
            ccp.getChannelModule().pause(channel);
        }
    }

    /**
     * This method allows resuming the paused channels in a ChannelConnectionPoint object. These channels were paused
     * for the connection establishment process, and must be resumed when the connection process is complete for
     * receiving messages through the rest of channels
     *
     * @param ccp ChannelConnectionPoint object whose channels are resumed
     */
    private static void resumeChannels(ChannelConnectionPoint ccp) {
        // again, we resume the concurrent channel sets except the one made for the connection process
        for (byte channel : getConcurrentChannelsExceptConnection()) {
            ccp.getChannelModule().resume(channel);
        }
    }


    synchronized ConnectionEstablishmentServerFSM.ConnectionResult newRequestConnectionAsServer(ConnectionEstablishmentClientFSM.ConnectionRequest connectionRequest) {
        // the server invokes this one. This PeerClient didn't know about this connection, so it must be confirmed
        // if we have higher priority in the connection process, check that we are not already trying to connect to
        // this same peer as clients
        PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(connectionRequest.clientPeerId);
        if (connectionRequest.clientWishRegularConnections) {
            peerEntryFacade.setWishForRegularConnections(Management.ConnectionWish.YES);
        } else {
            peerEntryFacade.setWishForRegularConnections(Management.ConnectionWish.NO);
        }
        try {
            peerEntryFacade.setPeerAddress(new PeerAddress(connectionRequest.clientAddress));
        } catch (IOException e) {
            // invalid address code -> deny connection
            return null;
        }
        peerEntryFacade.setMainCountry(connectionRequest.clientMainCountry);
        peerEntryFacade.setRelationshipToUs(connectionRequest.clientToServerRelationship);

        // first check the basic situations: no longer connected, already connected
        if (!wishForConnection.get()) {
            // not connected
            return new ConnectionEstablishmentServerFSM.ConnectionResult(ConnectionEstablishmentServerFSM.ConnectionResultType.DENY);
        } else if (connectedPeers.isConnectedPeer(connectionRequest.clientPeerId)) {
            // already connected
            return new ConnectionEstablishmentServerFSM.ConnectionResult(ConnectionEstablishmentServerFSM.ConnectionResultType.ALREADY_CONNECTED);
        } else if ((ownPeerId.hasHigherPriorityThan(connectionRequest.clientPeerId) && ongoingClientConnections.contains(connectionRequest.clientPeerId))) {
            // ongoing connection with higher priority
            return new ConnectionEstablishmentServerFSM.ConnectionResult(ConnectionEstablishmentServerFSM.ConnectionResultType.ALREADY_CONNECTED);
        }

        // check incorrect information passed by client and referring to us
        if (incorrectInfoFromClient(connectionRequest)) {
            return new ConnectionEstablishmentServerFSM.ConnectionResult(
                    ConnectionEstablishmentServerFSM.ConnectionResultType.INCORRECT_SERVER_INFO,
                    buildCorrectedInfo(peerEntryFacade));
        }

        // check client authentication
        // first, check that the public key corresponds to the client id
        if (!connectionRequest.clientPeerId.equals(new PeerId(connectionRequest.clientPublicKey.getEncoded()))) {
            // provided key does not correspond to provided client peer id
            return new ConnectionEstablishmentServerFSM.ConnectionResult(ConnectionEstablishmentServerFSM.ConnectionResultType.WRONG_AUTHENTICATION_ID_KEY_NOT_MATCHING);
        }
        // second, check that the provided central server secret is good
        // todo
        // third, check that the central server secret is truly encoded by this peer
        // todo

        // information and authentication are good -> check our relation with this peer
        Management.Relationship relationship = peerEntryFacade.getRelationship();
        switch (relationship) {

            case FAVORITE:
                // accept connection always
                return new ConnectionEstablishmentServerFSM.ConnectionResult(
                        ConnectionEstablishmentServerFSM.ConnectionResultType.OK,
                        buildAcceptedConnectionDetail(peerEntryFacade, connectionRequest.centralServerSecret));
            case REGULAR:
                // check what this peer offers to us, and see if we have room for his offer
                CountryCode clientMainCountry = connectionRequest.clientMainCountry;
                if (peerConnectionConfig.getMainCountry().equals(clientMainCountry)) {
                    // this peer offers the same country as us
                    if (connectedPeers.getConnectedPeersCountryCount(clientMainCountry) < peerConnectionConfig.getMaxRegularConnections()) {
                        // allowed to connect
                        return new ConnectionEstablishmentServerFSM.ConnectionResult(
                                ConnectionEstablishmentServerFSM.ConnectionResultType.OK,
                                buildAcceptedConnectionDetail(peerEntryFacade, connectionRequest.centralServerSecret));
                    } else {
                        // full
                        return new ConnectionEstablishmentServerFSM.ConnectionResult(ConnectionEstablishmentServerFSM.ConnectionResultType.REGULAR_SPOTS_TEMPORARILY_FULL);
                    }
                } else if (peerConnectionConfig.getAdditionalCountries().contains(clientMainCountry)) {
                    // this client offers a country in the list of additional countries
                    if (connectedPeers.getConnectedPeersCountryCount(clientMainCountry) < peerConnectionConfig.getMaxRegularConnectionsForAdditionalCountries()) {
                        // allowed to connect
                        return new ConnectionEstablishmentServerFSM.ConnectionResult(
                                ConnectionEstablishmentServerFSM.ConnectionResultType.OK,
                                buildAcceptedConnectionDetail(peerEntryFacade, connectionRequest.centralServerSecret));
                    } else {
                        // full
                        return new ConnectionEstablishmentServerFSM.ConnectionResult(ConnectionEstablishmentServerFSM.ConnectionResultType.REGULAR_SPOTS_TEMPORARILY_FULL);
                    }
                } else {
                    // this peer offers a country we are not interested in
                    if (connectedPeers.getConnectedPeersCountryCountExcept(peerConnectionConfig.getAllCountries()) < peerConnectionConfig.getMaxRegularConnectionsForAdditionalCountries()) {
                        // allowed to connect
                        return new ConnectionEstablishmentServerFSM.ConnectionResult(
                                ConnectionEstablishmentServerFSM.ConnectionResultType.OK,
                                buildAcceptedConnectionDetail(peerEntryFacade, connectionRequest.centralServerSecret));
                    } else {
                        // full
                        return new ConnectionEstablishmentServerFSM.ConnectionResult(ConnectionEstablishmentServerFSM.ConnectionResultType.REGULAR_SPOTS_TEMPORARILY_FULL);
                    }
                }
            case BLOCKED:
                // deny always
                return new ConnectionEstablishmentServerFSM.ConnectionResult(ConnectionEstablishmentServerFSM.ConnectionResultType.BLOCKED);
            default:
                // cannot happen
                return null;
        }


//        if (!isWishForFriendSearch()) {
//            // check we are disconnected
//            return null;
//        } else if (connectedPeers.isConnectedPeer(remotePeerId)) {
//            // check already connected
//            return null;
//        } else if ((ownPeerId.hasHigherPriorityThan(remotePeerId) && ongoingClientConnections.contains(remotePeerId))) {
//            // check ongoing connection with higher priority
//            return null;
//        }
//        // check what type of relation we have with this peer
//        if (peerRelations.isFriendPeer(remotePeerId)) {
//            // everything ok, accept connection request and add this peer to the list of connected peers
//            newConnection(remotePeerId, ccp, ConnectionStatus.CORRECT);
//            return ConnectionEstablishmentServerFSM.ConnectionResult.CORRECT;
//        } else if (peerRelations.isBlockedPeer(remotePeerId)) {
//            // forbidden peer, deny connection
//            return null;
//        } else {
//            // unknown peer, accept connection but set as validation required
//            newConnection(remotePeerId, ccp, ConnectionStatus.UNVALIDATED);
//            return ConnectionEstablishmentServerFSM.ConnectionResult.UNKNOWN_FRIEND_PENDING_VALIDATION;
//        }
    }

    private boolean incorrectInfoFromClient(ConnectionEstablishmentClientFSM.ConnectionRequest connectionRequest) {
        // check cases of info incorrectness
        // - wrong server peer id
        // - wrong serverWishRegularConnections
        // - wrong server main country
        return (!jacz.util.objects.Util.equals(connectionRequest.serverPeerId, ownPeerId)
                || !jacz.util.objects.Util.equals(connectionRequest.serverWishRegularConnections, peerConnectionConfig.isWishRegularConnections())
                || !jacz.util.objects.Util.equals(connectionRequest.serverMainCountry, peerConnectionConfig.getMainCountry())
        );
    }

    private ConnectionEstablishmentServerFSM.DetailCorrectedInformation buildCorrectedInfo(PeerEntryFacade peerEntryFacade) {
        return new ConnectionEstablishmentServerFSM.DetailCorrectedInformation(
                ownPeerEncryption.getPublicKey(),
                peerConnectionConfig.isWishRegularConnections(),
                peerEntryFacade.getRelationship(),
                ownPeerId,
                peerConnectionConfig.getMainCountry());
    }

    private ConnectionEstablishmentServerFSM.DetailAcceptedConnection buildAcceptedConnectionDetail(PeerEntryFacade peerEntryFacade, String centralServerSecret) {
        // todo build encoded
        return new ConnectionEstablishmentServerFSM.DetailAcceptedConnection(ownPeerEncryption.getPublicKey(), peerEntryFacade.getRelationship(), FAKE_ENCODED_SERVER_SECRET);
    }

    synchronized void connectionAsServerCompleted(PeerId peerId, ChannelConnectionPoint ccp, CountryCode clientMainCountry) {
        connectionCompleted(peerId, ccp, clientMainCountry);
    }

    /**
     * This method is invoked by the connection client FSM to report that connection has been established with a
     * server peer.
     *
     * @param peerId ID of the peer to which we connected
     * @param ccp    ChannelConnectionPoint object of the connected peer
     */
    synchronized void connectionAsClientCompleted(PeerId peerId, ChannelConnectionPoint ccp, Management.Relationship serverToClientRelationship, CountryCode serverMainCountry) {
        // the client invokes this one. This PeerClient itself asked for it, so no confirmation is returned
        // if something is wrong, the PeerClient must deal with it itself, since connection is already established
        ongoingClientConnections.remove(peerId);
        peerKnowledgeBase.getPeerEntryFacade(peerId).setRelationshipToUs(serverToClientRelationship);
        connectionCompleted(peerId, ccp, serverMainCountry);
    }

    /**
     * This method creates a new connection (as client or server). It first makes a few checks
     *
     * @param peerId ID of the peer to which we connected
     * @param ccp    ChannelConnectionPoint object of the connected peer
     */
    private void connectionCompleted(PeerId peerId, ChannelConnectionPoint ccp, CountryCode peerMainCountry) {
        // the client invokes this one. This PeerClient itself asked for it, so no confirmation is returned
        // if something is wrong, the PeerClient must deal with it itself, since connection is already established
        if (!wishForConnection.get()) {
            // we no longer wish to connect to peers
            ccp.disconnect();
        } else if (connectedPeers.isConnectedPeer(peerId)) {
            // check already connected
            ccp.disconnect();
        } else if (peerKnowledgeBase.getPeerEntryFacade(peerId).getRelationship() == Management.Relationship.BLOCKED) {
            // check he is now blocked
            ccp.disconnect();
        } else {
            newConnection(peerId, ccp, peerMainCountry);
        }
    }

    /**
     * This method allows the connection FSMs to inform that a connection process has concluded (either successfully
     * or not...)
     *
     * @param peerId the ID of the friend whose ongoing connection process has finished
     */
    void connectionAsClientFailed(PeerId peerId, IP4Port secondaryIP4Port, PeerId serverPeerId) {
        // if the connection was not successful, disconnect from the other peer (only for client role)
        ongoingClientConnections.remove(peerId);
        // if there is a secondary address available, try it
        if (secondaryIP4Port != null) {
            try {
                tryConnection(secondaryIP4Port, serverPeerId, null);
            } catch (IOException e) {
                // peer not available or wrong peer data received
            }
        }
    }

    /**
     * The server peer sends corrected information about him. The connection was denied, but we can update our info
     * about this peer
     */
    void correctedPeerInformation(PeerId peerId, ConnectionEstablishmentServerFSM.DetailCorrectedInformation detailCorrectedInformation) {
        PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(peerId);
        if (!peerId.equals(detailCorrectedInformation.serverPeerId)) {
            // the peer id was incorrect -> delete the address in the initial peer id, and add the info for the new peer id
            peerEntryFacade.setPeerAddress(null);
            peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(detailCorrectedInformation.serverPeerId);
        }
        peerEntryFacade.setRelationshipToUs(detailCorrectedInformation.serverToClientRelationship);
        if (detailCorrectedInformation.serverWishRegularConnections) {
            peerEntryFacade.setWishForRegularConnections(Management.ConnectionWish.YES);
        } else {
            peerEntryFacade.setWishForRegularConnections(Management.ConnectionWish.NO);
        }
        peerEntryFacade.setMainCountry(detailCorrectedInformation.serverMainCountry);
    }

    /**
     * A new connection with a peer has been achieved (either as client or as server). This method performs the
     * necessary set up for the new connection
     *
     * @param peerId ID of the peer to which we connected
     * @param ccp    ChannelConnectionPoint object of the peer to which we connected
     */
    private void newConnection(PeerId peerId, ChannelConnectionPoint ccp, CountryCode clientCountryCode) {
        // the request dispatcher for this connections is registered, the connection manager is notified, and the
        // peer is marked in the list of connected peers
        // also our client is informed of this new connection
        // finally, the blocked channels of this connection are resumed, so data transfer can begin
        ccp.registerGenericFSM(new PeerRequestDispatcherFSM(peerClientPrivateInterface, peerId), "PeerRequestDispatcherFSM", ChannelConstants.REQUEST_DISPATCHER_CHANNEL);
        connectedPeers.setConnectedPeer(peerId, ccp, clientCountryCode);
        peerClientPrivateInterface.newPeerConnected(peerId, ccp, getPeerRelationship(peerId));
        resumeChannels(ccp);
    }

    /**
     * This method allows to notify that any of the currently used ChannelConnectionPoints with other peers has freed some channels.
     * <p/>
     * This method can be invoked either from the PeerClientServerActionImpl, because it will handle the
     * ChannelConnectionPoints obtained from peers connected to us, or from the
     * PeerClientConnectionToClientChannelActionImpl, because it will handle the ccps of peers to which we are connected
     *
     * @param ccp     the ChannelConnectionPoint that has freed some channels
     * @param channel the freed channel
     */
    public void channelFreed(ChannelConnectionPoint ccp, byte channel) {
        // the PeerClient must be informed of the freed channels, through the PeerClientPrivateInterface
        connectedPeers.channelFreed(ccp, channel);
    }

    public void peerDisconnected(ChannelConnectionPoint ccp) {
        PeerId peerId = connectedPeers.peerDisconnected(ccp);
        if (peerId != null) {
            peerClientPrivateInterface.peerDisconnected(peerId);
        }
    }

    public void peerError(ChannelConnectionPoint ccp, CommError error) {
        PeerId peerId = connectedPeers.peerDisconnected(ccp);
        if (peerId != null) {
            peerClientPrivateInterface.peerError(peerId, error);
        }
    }


    public void stop() {
        setWishForConnect(false);
        favoritesConnectionManager.stop();
        regularsConnectionManager.stop();
    }

    public static byte[] getConcurrentChannelsExceptConnection() {
        byte[] concurrentChannels = new byte[2];
        concurrentChannels[0] = ChannelConstants.REQUEST_DISPATCHER_CHANNEL;
        concurrentChannels[1] = ChannelConstants.RESOURCE_STREAMING_MANAGER_CHANNEL;
        return concurrentChannels;
    }

    public synchronized PeerRelationship getPeerRelationship(PeerId peerId) {
        PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(peerId);
        Management.Relationship usToHim = peerEntryFacade.getRelationship();
        Management.Relationship himToUs = peerEntryFacade.getRelationshipToUs();
        if (usToHim == Management.Relationship.BLOCKED) {
            return PeerRelationship.BLOCKED;
        } else if (usToHim == Management.Relationship.REGULAR && himToUs == Management.Relationship.REGULAR) {
            return PeerRelationship.REGULARS;
        } else if (usToHim == Management.Relationship.REGULAR && himToUs == Management.Relationship.FAVORITE) {
            return PeerRelationship.REGULAR_TO_FAVORITE;
        } else if (usToHim == Management.Relationship.FAVORITE && himToUs == Management.Relationship.REGULAR) {
            return PeerRelationship.FAVORITE_TO_REGULAR;
        } else {
            return PeerRelationship.FAVORITES;
        }
    }

    public synchronized boolean isFavoritePeer(PeerId peerId) {
        PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(peerId);
        return peerEntryFacade.getRelationship() == Management.Relationship.FAVORITE;
    }

    public synchronized boolean isRegularPeer(PeerId peerId) {
        PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(peerId);
        return peerEntryFacade.getRelationship() == Management.Relationship.REGULAR;
    }

    public synchronized boolean isBlockedPeer(PeerId peerId) {
        PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(peerId);
        return peerEntryFacade.getRelationship() == Management.Relationship.BLOCKED;
    }

    public synchronized Set<PeerId> getFavoritePeers() {
        Set<PeerId> peers = new HashSet<>();
        for (PeerEntryFacade peerEntryFacade : peerKnowledgeBase.getFavoritePeers(PeerKnowledgeBase.ConnectedQuery.ALL)) {
            peers.add(peerEntryFacade.getPeerId());
        }
        return peers;
    }

    public synchronized void addFavoritePeer(final PeerId peerId) {
        PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(peerId);
        if (peerEntryFacade.getRelationship() != Management.Relationship.FAVORITE) {
            peerEntryFacade.setRelationship(Management.Relationship.FAVORITE);
            searchFriends();
            generalEvents.peerAddedAsFriend(peerId);
        }
    }

    public synchronized void removeFavoritePeer(final PeerId peerId) {
        PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(peerId);
        if (peerEntryFacade.getRelationship() == Management.Relationship.FAVORITE) {
            peerEntryFacade.setRelationship(Management.Relationship.REGULAR);
            searchFriends();
            generalEvents.peerRemovedAsFriend(peerId);
        }
    }

    public synchronized Set<PeerId> getBlockedPeers() {
        Set<PeerId> peers = new HashSet<>();
        for (PeerEntryFacade peerEntryFacade : peerKnowledgeBase.getBlockedPeers(PeerKnowledgeBase.ConnectedQuery.ALL)) {
            peers.add(peerEntryFacade.getPeerId());
        }
        return peers;
    }

    public synchronized void addBlockedPeer(final PeerId peerId) {
        PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(peerId);
        if (peerEntryFacade.getRelationship() != Management.Relationship.BLOCKED) {
            peerEntryFacade.setRelationship(Management.Relationship.BLOCKED);
            searchFriends();
            generalEvents.peerAddedAsBlocked(peerId);
        }
    }

    public synchronized void removeBlockedPeer(final PeerId peerId) {
        PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(peerId);
        if (peerEntryFacade.getRelationship() == Management.Relationship.BLOCKED) {
            peerEntryFacade.setRelationship(Management.Relationship.REGULAR);
            searchFriends();
            generalEvents.peerRemovedAsBlocked(peerId);
        }
    }

    public synchronized void searchFriends() {
        // todo currently unavailable

    }


}