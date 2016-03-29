package jacz.peerengineservice.client.connection.peers;

import com.neovisionaries.i18n.CountryCode;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.clientserver.client.ClientModule;
import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerClientPrivateInterface;
import jacz.peerengineservice.client.connection.*;
import jacz.peerengineservice.client.connection.peers.kb.Management;
import jacz.peerengineservice.client.connection.peers.kb.PeerAddress;
import jacz.peerengineservice.client.connection.peers.kb.PeerEntryFacade;
import jacz.peerengineservice.client.connection.peers.kb.PeerKnowledgeBase;
import jacz.peerengineservice.util.ChannelConstants;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.util.network.IP4Port;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Alberto on 09/03/2016.
 */
public class PeerConnectionManager {

    /**
     * Five seconds for connection (should be enough, increase if needed)
     */
    private static final long CONNECTION_TIMEOUT = 5000;

    /**
     * Our own ID. Cannot be modified after construction time
     */
    private final PeerId ownPeerId;

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


    public PeerConnectionManager(PeerId ownPeerId, String serverURL, PeerConnectionConfig peerConnectionConfig, String peerKnowledgeBasePath, ConnectedPeers connectedPeers, PeerClientPrivateInterface peerClientPrivateInterface) {
        this.ownPeerId = ownPeerId;
        this.serverURL = serverURL;
        this.peerKnowledgeBase = new PeerKnowledgeBase(peerKnowledgeBasePath);
        this.peerConnectionConfig = peerConnectionConfig;
        this.favoritesConnectionManager = new FavoritesConnectionManager(this, peerKnowledgeBase);
        this.regularsConnectionManager = new RegularsConnectionManager(this, peerKnowledgeBase, peerConnectionConfig);
        this.ongoingClientConnections = new HashSet<>();
        this.connectedPeers = connectedPeers;
        this.wishForConnection = new AtomicBoolean(false);
        this.peerClientPrivateInterface = peerClientPrivateInterface;
    }

    public void setWishForConnection(boolean enabled) {
        wishForConnection.set(enabled);
        favoritesConnectionManager.setConnectionGoal(enabled);
        regularsConnectionManager.setConnectionGoal(enabled);
    }

    public void setWishForRegularsConnection(boolean enabled) {
        peerConnectionConfig.setWishRegularConnections(enabled);
        regularsConnectionManager.connectionConfigHasChanged();
    }

    public void setMaxRegularConnections(int maxRegularConnections) {
        peerConnectionConfig.setMaxRegularConnections(maxRegularConnections);
        regularsConnectionManager.connectionConfigHasChanged();
    }

    boolean discardConnectionAttempt(PeerEntryFacade peerEntryFacade) {
        // discard those who:
        // - is now connected
        // - are in ongoing connections
        // - do not temporarily want to connect with us and the last connection attempt is recent
        // - do not definitely want to connect with us and the last connection attempt is very old
        return false;
    }

    void askForMoreRegularPeers(CountryCode country) {
        try {
            ServerAPI.InfoResponse infoResponse = ServerAPI.regularPeersRequest(serverURL, new ServerAPI.RegularPeersRequest(country));
            for (ServerAPI.PeerIdInfo peerIdInfo : infoResponse.getPeerIdInfoList()) {
                // update info for each peer
                // todo use a transaction
                PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(peerIdInfo.getPeerId());
                peerEntryFacade.setMainCountry(peerIdInfo.getMainCountry());
                if (peerIdInfo.isWishRegularConnections()) {
                    peerEntryFacade.setWishForConnections(Management.ConnectionWish.YES);
                } else {
                    peerEntryFacade.setWishForConnections(Management.ConnectionWish.NO);
                }
                peerEntryFacade.setConnected(true);
                IP4Port externalAddress = new IP4Port(peerIdInfo.getExternalIPAddress(), peerIdInfo.getExternalMainServerPort());
                IP4Port localAddress = new IP4Port(peerIdInfo.getLocalIPAddress(), peerIdInfo.getLocalMainServerPort());
                peerEntryFacade.setPeerAddress(new PeerAddress(externalAddress, localAddress));
                peerEntryFacade.setInfoSource(Management.InfoSource.SERVER);
            }
        } catch (IOException | ServerAccessException e) {
            // error connecting with the server -> will retry later
        }
    }

    public void updatePeersAddress(List<PeerEntryFacade> peerEntryFacades) {
        // todo
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


    void reportClientConnectedToOurPeerServer(ChannelConnectionPoint ccp) {
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
            ccp.registerTimedFSM(new ConnectionEstablishmentClientFSM(this, ownPeerId, remotePeerId, secondaryIP4Port), CONNECTION_TIMEOUT, "ConnectionEstablishmentClientFSM", ChannelConstants.CONNECTION_ESTABLISHMENT_CHANNEL);
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
        for (byte channel : FriendConnectionManager.getConcurrentChannelsExceptConnection()) {
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
        for (byte channel : FriendConnectionManager.getConcurrentChannelsExceptConnection()) {
            ccp.getChannelModule().resume(channel);
        }
    }


    ConnectionEstablishmentServerFSM.ConnectionResult newRequestConnectionAsServer(PeerId remotePeerId, ChannelConnectionPoint ccp) {
        // the server invokes this one. This PeerClient didn't know about this connection, so it must be confirmed
        // if we have higher priority in the connection process, check that we are not already trying to connect to
        // this same peer as clients
        // todo: first update the pkb with the received information
        PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(remotePeerId);
        if (!wishForConnection.get()) {
            // not connected
            return ConnectionEstablishmentServerFSM.ConnectionResult.DENY;
        } else if (connectedPeers.isConnectedPeer(remotePeerId)) {
            // already connected
            return ConnectionEstablishmentServerFSM.ConnectionResult.ALREADY_CONNECTED;
        } else if ((ownPeerId.hasHigherPriorityThan(remotePeerId) && ongoingClientConnections.contains(remotePeerId))) {
            // ongoing connection with higher priority
            return ConnectionEstablishmentServerFSM.ConnectionResult.ALREADY_CONNECTED;
        }
        // check our relation with this peer
        Management.Relationship relationship = peerEntryFacade.getRelationship();
        switch (relationship) {

            case FAVORITE:
                // accept connection always
                return ConnectionEstablishmentServerFSM.ConnectionResult.OK;
            case REGULAR:
                // check what this peer offers to us, and see if we have room for his offer
                // todo
                return null;
            case BLOCKED:
                // deny always
                return ConnectionEstablishmentServerFSM.ConnectionResult.BLOCKED;
            default:
                return ConnectionEstablishmentServerFSM.ConnectionResult.DENY;
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

    /**
     * This method is invoked by the connection client FSM to report that connection has been established with a
     * server peer.
     *
     * @param peerId                   ID of the peer to which we connected
     * @param ccp                      ChannelConnectionPoint object of the connected peer
     * @param detailAcceptedConnection details provided by the server peer
     */
    void connectionAsClientCompleted(PeerId peerId, ChannelConnectionPoint ccp, ConnectionEstablishmentServerFSM.DetailAcceptedConnection detailAcceptedConnection) {
        // the client invokes this one. This PeerClient itself asked for it, so no confirmation is returned
        // if something is wrong, the PeerClient must deal with it itself, since connection is already established
        ongoingClientConnections.remove(peerId);
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
            // todo remove validated. Update pkb with returned information
            ConnectionStatus status = validated ? ConnectionStatus.CORRECT : ConnectionStatus.WAITING_FOR_REMOTE_VALIDATION;
            newConnection(peerId, ccp, status);
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
                // todo if info source is server, inform him of possible wrong data
            }
        }
    }

    /**
     * The server peer sends corrected information about him. The connection was denied, but we can update our info
     * about this peer
     */
    void correctedPeerInformation(PeerId peerId, ConnectionEstablishmentServerFSM.DetailCorrectedInformation detailCorrectedInformation) {
        // todo
    }

    /**
     * A new connection with a peer has been achieved (either as client or as server). This method performs the
     * necessary set up for the new connection
     *
     * @param peerId ID of the peer to which we connected
     * @param ccp    ChannelConnectionPoint object of the peer to which we connected
     * @param status the connection status to give to this new peer connection
     */
    private void newConnection(PeerId peerId, ChannelConnectionPoint ccp, ConnectionStatus status) {
        // the request dispatcher for this connections is registered, the connection manager is notified, and the
        // peer is marked in the list of connected peers
        // also our client is informed of this new connection
        // finally, the blocked channels of this connection are resumed, so data transfer can begin
        ccp.registerGenericFSM(new PeerRequestDispatcherFSM(peerClientPrivateInterface, peerId), "PeerRequestDispatcherFSM", ChannelConstants.REQUEST_DISPATCHER_CHANNEL);
        connectedPeers.setConnectedPeer(peerId, ccp, status);
        peerClientPrivateInterface.newPeerConnected(peerId, ccp, status);
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
    void channelFreed(ChannelConnectionPoint ccp, byte channel) {
        // the PeerClient must be informed of the freed channels, through the PeerClientPrivateInterface
        connectedPeers.channelFreed(ccp, channel);
    }

    void peerDisconnected(ChannelConnectionPoint ccp) {
        PeerId peerId = connectedPeers.peerDisconnected(ccp);
        if (peerId != null) {
            peerClientPrivateInterface.peerDisconnected(peerId);
        }
    }

    void peerError(ChannelConnectionPoint ccp, CommError error) {
        PeerId peerId = connectedPeers.peerDisconnected(ccp);
        if (peerId != null) {
            peerClientPrivateInterface.peerError(peerId, error);
        }
    }


    public void stop() {
        favoritesConnectionManager.stop();
        regularsConnectionManager.stop();
    }
}
