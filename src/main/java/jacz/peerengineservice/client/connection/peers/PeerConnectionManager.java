package jacz.peerengineservice.client.connection.peers;

import com.neovisionaries.i18n.CountryCode;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.clientserver.client.ClientModule;
import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerEncryption;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.client.PeerClientPrivateInterface;
import jacz.peerengineservice.client.connection.*;
import jacz.peerengineservice.client.connection.peers.kb.Management;
import jacz.peerengineservice.client.connection.peers.kb.PeerEntryFacade;
import jacz.peerengineservice.client.connection.peers.kb.PeerKnowledgeBase;
import jacz.peerengineservice.util.ChannelConstants;
import jacz.peerengineservice.util.PeerRelationship;
import jacz.util.network.IP4Port;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is in charge of achieving connections with other peers. It takes care of maintaining the peer knowledge
 * base and checking authentication. Changes in connection config are done through here.
 */
public class PeerConnectionManager {

    private final static Logger logger = LoggerFactory.getLogger(PeerConnectionManager.class);

    // todo remove (@CONNECTION-AUTH@)
    private static final String FAKE_SERVER_SECRET = "@FAKE@";
    private static final String FAKE_ENCODED_SERVER_SECRET = "@FAKE@";

    /**
     * 20-minute threshold for recent connections
     */
    private static final long RECENT_CONNECTION_THRESHOLD = 1000L * 60L * 20L;

    /**
     * 1-week threshold for old connections
     */
    private static final long OLD_CONNECTION_THRESHOLD = 1000L * 60L * 60L * 24L * 7L;

    /**
     * Five seconds for connection (should be enough, increase if needed)
     */
    private static final long CONNECTION_TIMEOUT = 5000;

    /**
     * Amount of peers sent as extra information to requesting peers
     */
    private static final int PEER_RECORDS_SIZE = 2;


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
     * <p/>
     * This set is backed by a concurrent map, so it is thread-safe
     */
    private final Set<PeerId> ongoingClientConnections;

    private final ConnectedPeers connectedPeers;

    private final AtomicBoolean wishForConnection;

    private final PeerClientPrivateInterface peerClientPrivateInterface;

    private final PeerClientConnectionManager peerClientConnectionManager;

    private final DisconnectionsManager disconnectionsManager;

    private final PeersEventsBridge peersEvents;

    private final PeersLookingForRegularConnectionsRecord peersLookingForRegularConnectionsRecord;


    public PeerConnectionManager(
            PeerId ownPeerId,
            PeerEncryption ownPeerEncryption,
            String serverURL,
            String peerConnectionConfigPath,
            String peerKnowledgeBasePath,
            ConnectedPeers connectedPeers,
            PeerClientPrivateInterface peerClientPrivateInterface,
            PeerClientConnectionManager peerClientConnectionManager,
            PeersEvents peersEvents) throws IOException {
        this.ownPeerId = ownPeerId;
        this.ownPeerEncryption = ownPeerEncryption;
        this.ownPeerAddress = null;
        this.serverURL = serverURL;
        this.peerKnowledgeBase = new PeerKnowledgeBase(peerKnowledgeBasePath);
        this.peerConnectionConfig = new PeerConnectionConfig(peerConnectionConfigPath);
        this.favoritesConnectionManager = new FavoritesConnectionManager(this, peerKnowledgeBase);
        this.regularsConnectionManager = new RegularsConnectionManager(this, peerKnowledgeBase, connectedPeers, peerConnectionConfig);
        this.ongoingClientConnections = ConcurrentHashMap.newKeySet();
        this.connectedPeers = connectedPeers;
        this.wishForConnection = new AtomicBoolean(false);
        this.peerClientPrivateInterface = peerClientPrivateInterface;
        this.peerClientConnectionManager = peerClientConnectionManager;
        this.disconnectionsManager = new DisconnectionsManager(this, connectedPeers, peerKnowledgeBase);
        this.peersEvents = new PeersEventsBridge(peersEvents, peerClientPrivateInterface);
        this.peersLookingForRegularConnectionsRecord = new PeersLookingForRegularConnectionsRecord(peerConnectionConfig.getMainCountry());
    }

    public void setWishForConnect(boolean enabled) {
        wishForConnection.set(enabled);
        if (enabled) {
            // gather our own peer address
            ownPeerAddress = peerClientConnectionManager.getPeerAddress();
        }
        favoritesConnectionManager.setConnectionGoal(enabled);
        regularsConnectionManager.setConnectionGoal(enabled);
        disconnectionsManager.checkDisconnections();
    }

    public boolean isWishForRegularConnections() {
        return peerConnectionConfig.isWishRegularConnections();
    }

    public void setWishForRegularsConnections(boolean enabled) {
        peerConnectionConfig.setWishRegularConnections(enabled);
        regularsConnectionManager.connectionConfigHasChanged();
        disconnectionsManager.checkDisconnections();
    }

    public int getMaxRegularConnections() {
        return peerConnectionConfig.getMaxRegularConnections();
    }

    public void setMaxRegularConnections(int maxRegularConnections) {
        peerConnectionConfig.setMaxRegularConnections(maxRegularConnections);
        regularsConnectionManager.connectionConfigHasChanged();
        disconnectionsManager.checkDisconnections();
    }

    public int getMaxRegularConnectionsForAdditionalCountries() {
        return peerConnectionConfig.getMaxRegularConnectionsForAdditionalCountries();
    }

    public void setMaxRegularConnectionsForAdditionalCountries(int maxRegularConnections) {
        peerConnectionConfig.setMaxRegularConnectionsForAdditionalCountries(maxRegularConnections);
        regularsConnectionManager.connectionConfigHasChanged();
        disconnectionsManager.checkDisconnections();
    }

    public synchronized Float getMaxDownloadSpeed() {
        return peerConnectionConfig.getMaxDownloadSpeed();
    }

    public synchronized void setMaxDownloadSpeed(Float speed) {
        peerConnectionConfig.setMaxDownloadSpeed(speed);
    }

    public synchronized Float getMaxUploadSpeed() {
        return peerConnectionConfig.getMaxUploadSpeed();
    }

    public synchronized void setMaxUploadSpeed(Float speed) {
        peerConnectionConfig.setMaxUploadSpeed(speed);
    }

    public double getDownloadPartSelectionAccuracy() {
        return peerConnectionConfig.getDownloadPartSelectionAccuracy();
    }

    public void setDownloadPartSelectionAccuracy(double accuracy) {
        peerConnectionConfig.setDownloadPartSelectionAccuracy(accuracy);
    }

    public int getMaxRegularConnectionsForOtherCountries() {
        return peerConnectionConfig.getMaxRegularConnectionsForOtherCountries();
    }

    public CountryCode getMainCountry() {
        return peerConnectionConfig.getMainCountry();
    }

    public void setMainCountry(CountryCode mainCountry) {
        if (peerConnectionConfig.setMainCountry(mainCountry)) {
            // notify all connected peers
            peerClientPrivateInterface.modifiedMainCountry(mainCountry);
            disconnectionsManager.checkDisconnections();
            peersLookingForRegularConnectionsRecord.setMainCountry(mainCountry);
        }
    }

    public List<CountryCode> getAdditionalCountries() {
        return peerConnectionConfig.getAdditionalCountries();
    }

    public boolean isAdditionalCountry(CountryCode country) {
        return peerConnectionConfig.isAdditionalCountry(country);
    }

    public void setAdditionalCountries(List<CountryCode> additionalCountries) {
        peerConnectionConfig.setAdditionalCountries(additionalCountries);
        disconnectionsManager.checkDisconnections();
    }

    public List<CountryCode> getAllCountries() {
        return peerConnectionConfig.getAllCountries();
    }

    boolean discardConnectionAttempt(PeerEntryFacade peerEntryFacade) {
        // discard those who:
        // - is now connected
        // - are in ongoing connections
        // - do not temporarily want to connect with us and the last connection attempt is recent
        // - do not definitely want to connect with us and the last connection attempt is very old
        if (connectedPeers.isConnectedPeer(peerEntryFacade.getPeerId())) {
            logger.info("Connection attempt discarded due to already connected");
            return true;
        }
        if (peerEntryFacade.getPeerAddress().isNull()) {
            logger.info("Connection attempt discarded due to unavailable peer address");
            return true;
        }
        if (ongoingClientConnections.contains(peerEntryFacade.getPeerId())) {
            logger.info("Connection attempt discarded due to ongoing connection");
            return true;
        }
        if (peerEntryFacade.getRelationship() == Management.Relationship.BLOCKED) {
            logger.info("Connection attempt discarded due to being blocked");
            return true;
        }
        if (peerEntryFacade.getRelationship() == Management.Relationship.REGULAR &&
                peerEntryFacade.getWishForRegularConnections() == Management.ConnectionWish.NOT_NOW &&
                connectionAttemptIsRecent(peerEntryFacade)) {
            logger.info("Connection attempt discarded due to peer not currently accepting us, and recently tried");
            return true;
        }
        if (peerEntryFacade.getRelationship() == Management.Relationship.REGULAR &&
                peerEntryFacade.getWishForRegularConnections() == Management.ConnectionWish.NO &&
                !connectionAttemptIsVeryOld(peerEntryFacade)) {
            logger.info("Connection attempt discarded due to peer not wishing to connect with us");
            return true;
        }
        return false;
    }

    private boolean connectionAttemptIsRecent(PeerEntryFacade peerEntryFacade) {
        return peerEntryFacade.getLastConnectionAttempt() != null && peerEntryFacade.getLastConnectionAttempt().getTime() + RECENT_CONNECTION_THRESHOLD > System.currentTimeMillis();
    }

    private boolean connectionAttemptIsVeryOld(PeerEntryFacade peerEntryFacade) {
        return peerEntryFacade.getLastConnectionAttempt() != null && peerEntryFacade.getLastConnectionAttempt().getTime() + OLD_CONNECTION_THRESHOLD < System.currentTimeMillis();
    }

    synchronized void askForFavoritePeersInfo(List<PeerId> needInfoFavorites) {
        if (!needInfoFavorites.isEmpty()) {
            // request peers data to the server
            try {
                ServerAPI.InfoResponse infoResponse = ServerAPI.info(serverURL, new ServerAPI.InfoRequest(needInfoFavorites));
                digestServerInfoResponse(infoResponse);
            } catch (IOException | ServerAccessException e) {
                // error connecting with the server -> will retry later
            }
        }
    }

    synchronized boolean askForMoreRegularPeers(CountryCode country) {
        try {
            ServerAPI.InfoResponse infoResponse = ServerAPI.regularPeersRequest(serverURL, new ServerAPI.RegularPeersRequest(country));
            return digestServerInfoResponse(infoResponse);
        } catch (IOException | ServerAccessException e) {
            // error connecting with the server -> will retry later
            return false;
        }
    }

    private boolean digestServerInfoResponse(ServerAPI.InfoResponse infoResponse) {
        boolean hasLoadedAny = false;
        for (ServerAPI.PeerIdInfo peerIdInfo : infoResponse.getPeerIdInfoList()) {
            // we must filter out our own peer id, as the server might provide this info to us
            if (!peerIdInfo.getPeerId().equals(ownPeerId)) {
                // update info for each peer
                PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(peerIdInfo.getPeerId());
                peerEntryFacade.openTransaction();
                peerEntryFacade.setMainCountry(peerIdInfo.getMainCountry());
                if (peerIdInfo.isWishRegularConnections()) {
                    peerEntryFacade.setWishForRegularConnections(Management.ConnectionWish.YES);
                } else {
                    peerEntryFacade.setWishForRegularConnections(Management.ConnectionWish.NO);
                }
                IP4Port externalAddress = new IP4Port(peerIdInfo.getExternalIPAddress(), peerIdInfo.getExternalMainServerPort());
                IP4Port localAddress = new IP4Port(peerIdInfo.getLocalIPAddress(), peerIdInfo.getLocalMainServerPort());
                peerEntryFacade.setPeerAddress(new PeerAddress(externalAddress, localAddress));
                peerEntryFacade.commitTransaction();
                hasLoadedAny = true;
            }
        }
        return hasLoadedAny;
    }

    synchronized void attemptConnection(PeerEntryFacade peerEntryFacade) {
        // A client module is created for each received peer. If connection is achieved, a
        // Connection Client FSM is created. The init method in the FSM will take care
        // of checking if it is actually possible to proceed with the connection

        if (connectedPeers.isConnectedPeer(peerEntryFacade.getPeerId()) || ongoingClientConnections.contains(peerEntryFacade.getPeerId())) {
            // check that we are not connected to this peer, or trying to connect to it
            return;
        }

        if (peerEntryFacade.getPeerId().equals(ownPeerId)) {
            // this entry corresponds to owr own peer -> ignore and delete entry
            // todo delete entry
            return;
        }

        IP4Port externalIP4Port = peerEntryFacade.getPeerAddress().getExternalAddress();
        IP4Port localIP4Port = peerEntryFacade.getPeerAddress().getLocalAddress();
        logger.info("Attempting to connect with " + peerEntryFacade.getPeerId());
        try {
            // first try public connection
            tryConnection(externalIP4Port, peerEntryFacade.getPeerId(), localIP4Port);
        } catch (IOException e) {
            // if this didn't work, try local connection (if exists)
            try {
                tryConnection(localIP4Port, peerEntryFacade.getPeerId(), null);
            } catch (IOException e2) {
                // peer not available or wrong/outdated peer data
                invalidatePeerAddressInfo(peerEntryFacade.getPeerId());
            }
        }
    }

    private void invalidatePeerAddressInfo(PeerId peerId) {
        logger.info("Invalidating address for " + peerId);
        PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(peerId);
        peerEntryFacade.setPeerAddress(PeerAddress.nullPeerAddress());
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
        logger.info("Contact with peer achieved: " + remotePeerId + ". We are client: " + isClient);
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
        PeerAddress clientAddress;
        try {
            clientAddress = new PeerAddress(connectionRequest.clientAddress);
        } catch (IOException e) {
            // invalid address code -> deny connection
            return null;
        }
        peerEntryFacade.setPeerAddress(clientAddress);
        peerEntryFacade.setMainCountry(connectionRequest.clientMainCountry);
        peerEntryFacade.setRelationshipToUs(connectionRequest.clientToServerRelationship);

        // prepare the list of peer records that will be sent together with the response
        List<PeersLookingForRegularConnectionsRecord.PeerRecord> peerRecords;
        if (connectionRequest.clientWishRegularConnections && peersLookingForRegularConnectionsRecord.getMainCountry().equals(connectionRequest.serverMainCountry)) {
            // the client seeks the country that we have stored -> return peers to him
            peerRecords = peersLookingForRegularConnectionsRecord.getRecords(connectionRequest.clientPeerId, PEER_RECORDS_SIZE);
            if (peersLookingForRegularConnectionsRecord.getMainCountry().equals(connectionRequest.clientMainCountry)) {
                // the client's country is our own country -> also store this peer in our records, for future requests from other peers
                peersLookingForRegularConnectionsRecord.addPeer(connectionRequest.clientPeerId, clientAddress);
            }
        } else {
            peerRecords = new ArrayList<>();
        }

        // first check the basic situations: no longer connected, already connected
        if (!wishForConnection.get()) {
            // not connected
            logConnectionRequestAsServer(connectionRequest, ConnectionEstablishmentServerFSM.ConnectionResultType.DENY, "no longer wish to connect");
            return new ConnectionEstablishmentServerFSM.ConnectionResult(ConnectionEstablishmentServerFSM.ConnectionResultType.DENY, peerRecords);
        } else if (connectedPeers.isConnectedPeer(connectionRequest.clientPeerId)) {
            // already connected
            logConnectionRequestAsServer(connectionRequest, ConnectionEstablishmentServerFSM.ConnectionResultType.DENY, "already connected with this peer");
            return new ConnectionEstablishmentServerFSM.ConnectionResult(ConnectionEstablishmentServerFSM.ConnectionResultType.ALREADY_CONNECTED, peerRecords);
        } else if ((ownPeerId.hasHigherPriorityThan(connectionRequest.clientPeerId) && ongoingClientConnections.contains(connectionRequest.clientPeerId))) {
            // ongoing connection with higher priority
            logConnectionRequestAsServer(connectionRequest, ConnectionEstablishmentServerFSM.ConnectionResultType.DENY, "ongoing connection of higher priority with this peer");
            return new ConnectionEstablishmentServerFSM.ConnectionResult(ConnectionEstablishmentServerFSM.ConnectionResultType.ALREADY_CONNECTED, peerRecords);
        }

        // check incorrect information passed by client and referring to us
        if (incorrectInfoFromClient(connectionRequest)) {
            logConnectionRequestAsServer(connectionRequest, ConnectionEstablishmentServerFSM.ConnectionResultType.INCORRECT_SERVER_INFO, "server info in request is not correct");
            return new ConnectionEstablishmentServerFSM.ConnectionResult(
                    ConnectionEstablishmentServerFSM.ConnectionResultType.INCORRECT_SERVER_INFO,
                    buildCorrectedInfo(peerEntryFacade),
                    peerRecords);
        }

        // check client authentication
        // first, check that the public key corresponds to the client id
        // todo activate (@CONNECTION-AUTH@)
//        if (!connectionRequest.clientPeerId.equals(new PeerId(connectionRequest.clientPublicKey.getEncoded()))) {
//            // provided key does not correspond to provided client peer id
//            return new ConnectionEstablishmentServerFSM.ConnectionResult(ConnectionEstablishmentServerFSM.ConnectionResultType.WRONG_AUTHENTICATION_ID_KEY_NOT_MATCHING);
//        }
        // second, check that the provided central server secret is good
        // todo (@CONNECTION-AUTH@)
        // third, check that the central server secret is truly encoded by this peer
        // todo (@CONNECTION-AUTH@)

        // information and authentication are good -> check our relation with this peer
        Management.Relationship relationship = peerEntryFacade.getRelationship();
        switch (relationship) {

            case FAVORITE:
                // accept connection always
                logConnectionRequestAsServer(connectionRequest, ConnectionEstablishmentServerFSM.ConnectionResultType.OK, "client peer is favorite");
                return new ConnectionEstablishmentServerFSM.ConnectionResult(
                        ConnectionEstablishmentServerFSM.ConnectionResultType.OK,
                        buildAcceptedConnectionDetail(peerEntryFacade, connectionRequest.centralServerSecret),
                        peerRecords);
            case REGULAR:
                // check what this peer offers to us, and see if we have room for his offer
                CountryCode clientMainCountry = connectionRequest.clientMainCountry;
                if (peerConnectionConfig.getMainCountry().equals(clientMainCountry)) {
                    // this peer offers the same country as us
                    if (connectedPeers.getConnectedPeersCountryCount(clientMainCountry) < peerConnectionConfig.getMaxRegularConnections()) {
                        // allowed to connect
                        logConnectionRequestAsServer(connectionRequest, ConnectionEstablishmentServerFSM.ConnectionResultType.OK, "regular peer, offers our main country and we have free slots");
                        return new ConnectionEstablishmentServerFSM.ConnectionResult(
                                ConnectionEstablishmentServerFSM.ConnectionResultType.OK,
                                buildAcceptedConnectionDetail(peerEntryFacade, connectionRequest.centralServerSecret),
                                peerRecords);
                    } else {
                        // full
                        logConnectionRequestAsServer(connectionRequest, ConnectionEstablishmentServerFSM.ConnectionResultType.REGULAR_SPOTS_TEMPORARILY_FULL, "regular peer, offers our main country but we do not have free slots");
                        return new ConnectionEstablishmentServerFSM.ConnectionResult(
                                ConnectionEstablishmentServerFSM.ConnectionResultType.REGULAR_SPOTS_TEMPORARILY_FULL,
                                peerRecords);
                    }
                } else if (peerConnectionConfig.getAdditionalCountries().contains(clientMainCountry)) {
                    // this client offers a country in the list of additional countries
                    if (connectedPeers.getConnectedPeersCountryCount(clientMainCountry) < peerConnectionConfig.getMaxRegularConnectionsForAdditionalCountries()) {
                        // allowed to connect
                        logConnectionRequestAsServer(connectionRequest, ConnectionEstablishmentServerFSM.ConnectionResultType.OK, "regular peer, offers one of our additional countries and we have free slots");
                        return new ConnectionEstablishmentServerFSM.ConnectionResult(
                                ConnectionEstablishmentServerFSM.ConnectionResultType.OK,
                                buildAcceptedConnectionDetail(peerEntryFacade, connectionRequest.centralServerSecret),
                                peerRecords);
                    } else {
                        // full
                        logConnectionRequestAsServer(connectionRequest, ConnectionEstablishmentServerFSM.ConnectionResultType.REGULAR_SPOTS_TEMPORARILY_FULL, "regular peer, offers one of our additional countries but we do not have free slots");
                        return new ConnectionEstablishmentServerFSM.ConnectionResult(
                                ConnectionEstablishmentServerFSM.ConnectionResultType.REGULAR_SPOTS_TEMPORARILY_FULL,
                                peerRecords);
                    }
                } else {
                    // this peer offers a country we are not interested in
                    if (connectedPeers.getConnectedPeersCountryCountExcept(peerConnectionConfig.getAllCountries()) < peerConnectionConfig.getMaxRegularConnectionsForOtherCountries()) {
                        // allowed to connect
                        logConnectionRequestAsServer(connectionRequest, ConnectionEstablishmentServerFSM.ConnectionResultType.OK, "regular peer, undesired country but we have free slots");
                        return new ConnectionEstablishmentServerFSM.ConnectionResult(
                                ConnectionEstablishmentServerFSM.ConnectionResultType.OK,
                                buildAcceptedConnectionDetail(peerEntryFacade, connectionRequest.centralServerSecret),
                                peerRecords);
                    } else {
                        // full
                        logConnectionRequestAsServer(connectionRequest, ConnectionEstablishmentServerFSM.ConnectionResultType.REGULAR_SPOTS_TEMPORARILY_FULL, "regular peer, undesired country and we have free slots");
                        return new ConnectionEstablishmentServerFSM.ConnectionResult(
                                ConnectionEstablishmentServerFSM.ConnectionResultType.REGULAR_SPOTS_TEMPORARILY_FULL,
                                peerRecords);
                    }
                }
            case BLOCKED:
                // deny always
                logConnectionRequestAsServer(connectionRequest, ConnectionEstablishmentServerFSM.ConnectionResultType.BLOCKED, "blocked peer");
                return new ConnectionEstablishmentServerFSM.ConnectionResult(
                        ConnectionEstablishmentServerFSM.ConnectionResultType.BLOCKED,
                        peerRecords);
            default:
                // cannot happen
                return null;
        }
    }

    private void logConnectionRequestAsServer(ConnectionEstablishmentClientFSM.ConnectionRequest connectionRequest, ConnectionEstablishmentServerFSM.ConnectionResultType resultType, String reason) {
        String message = reason == null ? resultType.name() : resultType.name() + ": " + reason;
        logger.info("Connection request received from " + connectionRequest.clientPeerId + ". " + message);
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
        // todo build encoded (@CONNECTION-AUTH@)
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
            logger.info("Connection completed with " + peerId + " (" + peerMainCountry.name() + ")");
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
                invalidatePeerAddressInfo(serverPeerId);
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
            invalidatePeerAddressInfo(peerId);
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

    void processExtraPeersInfo(List<PeersLookingForRegularConnectionsRecord.PeerRecord> peerRecords, CountryCode country) {
        // some peer records received from other peer -> load into pkb
        logger.info("Extra peers info received from server: " + peerRecords + ". Country: " + country);
        for (PeersLookingForRegularConnectionsRecord.PeerRecord peerRecord : peerRecords) {
            // avoid owr own peer to be processed
            if (!peerRecord.peerId.equals(ownPeerId)) {
                PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(peerRecord.peerId);
                peerEntryFacade.setPeerAddress(peerRecord.peerAddress);
                peerEntryFacade.setMainCountry(country);
                peerEntryFacade.setWishForRegularConnections(Management.ConnectionWish.YES);
            }
        }
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
        PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(peerId);
        peerEntryFacade.setConnected(true);
        peersEvents.newPeerConnected(peerId, ccp, new PeerInfo(peerEntryFacade));
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
        PeerId peerId = disconnectPeer(ccp);
        if (peerId != null) {
            peersEvents.peerDisconnected(peerId, new PeerInfo(peerKnowledgeBase.getPeerEntryFacade(peerId)), null);
        }
    }

    public void peerError(ChannelConnectionPoint ccp, CommError error) {
        if (error.getType() == CommError.Type.WRITE_NON_SERIALIZABLE_OBJECT || error.getType() == CommError.Type.CLASS_CANNOT_BE_SERIALIZED) {
            PeerClient.reportFatalError("Tried to write a non-serializable object through a ccp", error);
        }
        PeerId peerId = disconnectPeer(ccp);
        if (peerId != null) {
            peersEvents.peerDisconnected(peerId, new PeerInfo(peerKnowledgeBase.getPeerEntryFacade(peerId)), error);
        }
    }

    private PeerId disconnectPeer(ChannelConnectionPoint ccp) {
        PeerId peerId = connectedPeers.peerDisconnected(ccp);
        peerKnowledgeBase.getPeerEntryFacade(peerId).setConnected(false);
        return peerId;
    }

    public void stop() {
        setWishForConnect(false);
        favoritesConnectionManager.stop();
        regularsConnectionManager.stop();
        disconnectionsManager.stop();
        peersEvents.stop();
    }

    public static byte[] getConcurrentChannelsExceptConnection() {
        byte[] concurrentChannels = new byte[2];
        concurrentChannels[0] = ChannelConstants.REQUEST_DISPATCHER_CHANNEL;
        concurrentChannels[1] = ChannelConstants.RESOURCE_STREAMING_MANAGER_CHANNEL;
        return concurrentChannels;
    }

    public synchronized PeerRelationship getPeerRelationship(PeerId peerId) {
        return getPeerRelationship(peerKnowledgeBase.getPeerEntryFacade(peerId));
    }

    static synchronized PeerRelationship getPeerRelationship(PeerEntryFacade peerEntryFacade) {
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
            searchFavorites();
            peersEvents.modifiedPeerRelationship(peerId, Management.Relationship.FAVORITE, new PeerInfo(peerKnowledgeBase.getPeerEntryFacade(peerId)), true);
        }
    }

    public synchronized void removeFavoritePeer(final PeerId peerId) {
        PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(peerId);
        if (peerEntryFacade.getRelationship() == Management.Relationship.FAVORITE) {
            peerEntryFacade.setRelationship(Management.Relationship.REGULAR);
            disconnectionsManager.checkDisconnections();
            peersEvents.modifiedPeerRelationship(peerId, Management.Relationship.REGULAR, new PeerInfo(peerKnowledgeBase.getPeerEntryFacade(peerId)), true);
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
            disconnectionsManager.checkDisconnections();
            peersEvents.modifiedPeerRelationship(peerId, Management.Relationship.BLOCKED, new PeerInfo(peerKnowledgeBase.getPeerEntryFacade(peerId)), true);
        }
    }

    public synchronized void removeBlockedPeer(final PeerId peerId) {
        PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(peerId);
        if (peerEntryFacade.getRelationship() == Management.Relationship.BLOCKED) {
            peerEntryFacade.setRelationship(Management.Relationship.REGULAR);
            peersEvents.modifiedPeerRelationship(peerId, Management.Relationship.REGULAR, new PeerInfo(peerKnowledgeBase.getPeerEntryFacade(peerId)), true);
        }
    }

    public void searchFavorites() {
        favoritesConnectionManager.searchFavoritesNow();
    }

    public void clearAllPeerAddresses() {
        peerKnowledgeBase.clearAllPeerAddresses();
    }

    public void clearAllData() {
        peerKnowledgeBase.clearAllData();
    }

    public CountryCode getOwnMainCountry() {
        return peerConnectionConfig.getMainCountry();
    }

    public boolean isOwnWishForRegularConnections() {
        return peerConnectionConfig.isWishRegularConnections();
    }

    public int getPeerAffinity(PeerId peerId) {
        return peerKnowledgeBase.getPeerEntryFacade(peerId).getAffinity();
    }

    public void updatePeerAffinity(PeerId peerId, int affinity) {
        peerKnowledgeBase.getPeerEntryFacade(peerId).setAffinity(affinity);
    }

    public void newPeerNick(PeerId peerId, String nick) {
        peersEvents.newPeerNick(peerId, nick, new PeerInfo(peerKnowledgeBase.getPeerEntryFacade(peerId)));
    }

    public void newRelationshipToUs(PeerId peerId, Management.Relationship relationship) {
        PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(peerId);
        peerEntryFacade.setRelationshipToUs(relationship);
        peersEvents.modifiedPeerRelationship(peerId, relationship, new PeerInfo(peerKnowledgeBase.getPeerEntryFacade(peerId)), false);
    }

    public void peerModifiedHisMainCountry(PeerId peerId, CountryCode mainCountry) {
        PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(peerId);
        peerEntryFacade.setMainCountry(mainCountry);
        connectedPeers.setConnectedPeerMainCountry(peerId, mainCountry);
        disconnectionsManager.checkDisconnections();
    }
}
