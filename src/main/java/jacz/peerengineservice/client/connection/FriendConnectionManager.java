package jacz.peerengineservice.client.connection;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.clientserver.client.ClientModule;
import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClientPrivateInterface;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.util.ChannelConstants;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.concurrency.timer.SimpleTimerAction;
import jacz.util.concurrency.timer.Timer;
import jacz.util.network.IP4Port;

import java.io.IOException;
import java.util.*;

/**
 * Class in charge of managing the connections to friend peers
 */
public class FriendConnectionManager {

    private static class FriendSearchReminder implements SimpleTimerAction {

        private static final long FRIEND_SEARCH_DELAY_NORMAL = 90000L;

        private static final long FRIEND_SEARCH_DELAY_SHORT = 15000L;

        private final FriendConnectionManager friendConnectionManager;

        private final Timer timer;

        private FriendSearchReminder(FriendConnectionManager friendConnectionManager) {
            this.friendConnectionManager = friendConnectionManager;
            timer = new Timer(FRIEND_SEARCH_DELAY_NORMAL, this, false, "FriendSearchReminder");
        }

        synchronized void start() {
            friendConnectionManager.searchFriends();
            timer.reset(FRIEND_SEARCH_DELAY_NORMAL);
        }

        synchronized void searchIssueDetected() {
            timer.reset(FRIEND_SEARCH_DELAY_SHORT);
        }

        synchronized void stop() {
            timer.kill();
        }

        @Override
        public Long wakeUp(Timer timer) {
            friendConnectionManager.searchFriends();
            return FRIEND_SEARCH_DELAY_NORMAL;
        }
    }

    /**
     * Five seconds for connection (should be enough, increase if needed)
     */
    private static final long CONNECTION_TIMEOUT = 5000;

    /**
     * Our own ID. Cannot be modified after construction time
     */
    private final PeerID ownPeerID;

    private final PeerClientPrivateInterface peerClientPrivateInterface;

    private final PeerClientConnectionManager peerClientConnectionManager;

    /**
     * Relations with other peers. The connection manager will retrieve periodically from here the list of friend peers, and try to connect to the
     * ones who are not yet connected with us
     */
    private PeerRelations peerRelations;

    /**
     * Data about connected peers (connection is confirmed)
     */
    private ConnectedPeers connectedPeers;

    /**
     * Current ongoing connection trials with other friend peers, we act as CLIENT. It allows to not connect to the same peer twice
     * (it only stores ongoing connections in which we are clients, it does not consider other peers connecting to us)
     * <p/>
     * All accessions to this resource are synchronized, allowing us to correctly synchronize connections with peers
     * and avoid duplicate connections.
     * <p/>
     * Connections as client first go here. Then, atomically, go to connected peers
     */
    private Set<PeerID> ongoingClientConnections;

    /**
     * Whether we must be actively looking for friends or not
     */
    private boolean wishForFriendSearch;

    /**
     * Timer for launching searches of friends
     */
    private final FriendSearchReminder friendSearchReminder;


    FriendConnectionManager(
            PeerID ownPeerID,
            ConnectedPeers connectedPeers,
            PeerClientPrivateInterface peerClientPrivateInterface,
            PeerClientConnectionManager peerClientConnectionManager,
            PeerRelations peerRelations) {
        this.ownPeerID = ownPeerID;
        this.peerClientPrivateInterface = peerClientPrivateInterface;
        this.peerClientConnectionManager = peerClientConnectionManager;
        this.peerRelations = peerRelations;
        this.connectedPeers = connectedPeers;
        ongoingClientConnections = Collections.synchronizedSet(new HashSet<PeerID>());
        wishForFriendSearch = false;
        friendSearchReminder = new FriendSearchReminder(this);
    }

    synchronized void setWishForFriendSearch(boolean enabled) {
        if (enabled && !wishForFriendSearch) {
            wishForFriendSearch = true;
            friendSearchReminder.start();
        } else if (!enabled && wishForFriendSearch) {
            wishForFriendSearch = false;
            friendSearchReminder.stop();
        }
    }

    private synchronized boolean isWishForFriendSearch() {
        return wishForFriendSearch;
    }

    void stop() {
        setWishForFriendSearch(false);
        disconnectAllPeers();
        // actively wait until there are zero peers connected
        while (connectedPeers.connectedPeersCount() > 0) {
            ThreadUtil.safeSleep(100L);
        }
//        boolean mustWait;
//        synchronized (this) {
//            mustWait = connectedPeers.connectedPeersCount() > 0;
//        }
//        while (mustWait) {
//            ThreadUtil.safeSleep(100L);
//            synchronized (this) {
//                mustWait = connectedPeers.connectedPeersCount() > 0;
//            }
//        }
    }

    /**
     * Performs a connected friend search. Searches are performed periodically, but the user can force a search using this method. If we are not
     * connected to the server, this method will have no effect
     */
    void searchFriends() {
        // check if there are friends to which we are not connected yet
        // we can assume that we are connected to the server
        if (isWishForFriendSearch()) {
            // we must perform a friend search
            Set<PeerID> friendPeers = peerRelations.getFriendPeers();
            List<PeerID> disconnectedFriends = buildDisconnectedFriendsSet(friendPeers, connectedPeers, ongoingClientConnections);
            if (!disconnectedFriends.isEmpty()) {
                // request friends data to the server
                // this code simply sets up the FSM in charge of asking the peer server for connected friends. Only friends to which we are not
                // still connected are searched
                try {
                    ServerAPI.InfoResponse infoResponse = ServerAPI.info(new ServerAPI.InfoRequest(disconnectedFriends));
                    reportConnectedFriendsData(infoResponse);
                } catch (IOException | ServerAccessException e) {
                    // could not connect with server -> ignore and repeat search soon
                    friendSearchReminder.searchIssueDetected();
                } catch (IllegalArgumentException e) {
                    peerClientConnectionManager.unrecognizedServerMessage();
                    friendSearchReminder.searchIssueDetected();
                }
            }

            // now remove any peer to which we should not be connected
            Set<PeerID> mustDisconnectPeerSet = buildMustDisconnectPeerSet(friendPeers, peerRelations.getBlockedPeers(), connectedPeers);
            for (PeerID peerID : mustDisconnectPeerSet) {
                connectedPeers.disconnectPeer(peerID);
            }
        }
    }

    private static List<PeerID> buildDisconnectedFriendsSet(Set<PeerID> friendPeerIDs, ConnectedPeers connectedPeers, Set<PeerID> ongoingConnections) {
        // the disconnected friends are the total friend set, minus the connected friends, minus the friends with ongoing connections
        List<PeerID> disconnectedFriends = new ArrayList<>(friendPeerIDs);
        disconnectedFriends.removeAll(connectedPeers.getConnectedPeers());
        disconnectedFriends.removeAll(ongoingConnections);
//        for (PeerID ongoingConnection : ongoingConnections) {
//            disconnectedFriends.remove(ongoingConnection);
//        }
        return disconnectedFriends;
    }

    void disconnectAllPeers() {
        connectedPeers.disconnectAllPeers();
    }

    /**
     * This method allows the FSM in charge of searching for connected friends to give the list of found friends.
     * The PeerClientConnectionManager created this FSM to ask the peer server for some IDs of friends of us.
     * <p/>
     * This method will also try to connect to the received peers. It will only obtain ChannelConnectionPoints with
     * them, and pass them to our PeerClient so he handles the connection details. We add each peer with which we
     * achieve connection to the list of ongoing connections, so this is not done twice. The PeerClient will be in
     * charge of informing when the connection process is completed, either successfully or not
     *
     * @param infoResponse the list of found friends in the peer server
     */
    void reportConnectedFriendsData(ServerAPI.InfoResponse infoResponse) {
        for (ServerAPI.PeerIDInfo peerIDInfo : infoResponse.getPeerIDInfoList()) {
            tryToConnectToAPeer(peerIDInfo);
        }
    }

    private void tryToConnectToAPeer(ServerAPI.PeerIDInfo peerIDInfo) {
        // A client module is created for each received peer. If connection is achieved, a
        // Connection Client FSM is created. The init method in the FSM will take care
        // of checking if it is actually possible to proceed with the connection

        if (connectedPeers.isConnectedPeer(peerIDInfo.getPeerID()) || ongoingClientConnections.contains(peerIDInfo.getPeerID())) {
            // check that we are not connected to this peer, or trying to connect to it
            return;
        }

        IP4Port ip4Port = new IP4Port(peerIDInfo.getExternalIPAddress(), peerIDInfo.getExternalMainServerPort());
        try {
            // first try public connection
            tryConnection(ip4Port, peerIDInfo.getPeerID(), new IP4Port(peerIDInfo.getLocalIPAddress(), peerIDInfo.getLocalMainServerPort()));
        } catch (IOException e) {
            // if this didn't work, try local connection (if exists)
            IP4Port localIP4Port = new IP4Port(peerIDInfo.getLocalIPAddress(), peerIDInfo.getLocalMainServerPort());
            try {
                tryConnection(localIP4Port, peerIDInfo.getPeerID(), null);
            } catch (IOException e2) {
                // peer not available or wrong peer data received, repeat soon
                friendSearchReminder.searchIssueDetected();
            }
        }
    }

    private void tryConnection(IP4Port ip4Port, PeerID serverPeerID, IP4Port secondaryIP4Port) throws IOException {
        ClientModule friendClientModule = new ClientModule(ip4Port, new PeerClientConnectionToClientChannelActionImpl(this), PeerClientConnectionManager.generateConcurrentChannelSets());
        // first try public connection
        ChannelConnectionPoint ccp = friendClientModule.connect();
        contactWithPeerAchieved(ccp, true, serverPeerID, secondaryIP4Port);
        friendClientModule.start();
    }

    void reportClientConnectedToOurPeerServer(ChannelConnectionPoint ccp) {
        contactWithPeerAchieved(ccp, false, null, null);
    }

    private void contactWithPeerAchieved(ChannelConnectionPoint ccp, boolean isClient, PeerID remotePeerID, IP4Port secondaryIP4Port) {
        // depending on whether we are client or server, the corresponding FSM for establishing connection with the
        // other peer is created. This FSMs will send and receive data about the ID of the other peer to accept or
        // revoke the connection
        pauseChannelsExceptConnection(ccp);
        if (isClient) {
            // first mark as ongoing client connection
            ongoingClientConnections.add(remotePeerID);
            ccp.registerTimedFSM(new ConnectionEstablishmentClientFSM(this, ownPeerID, remotePeerID, secondaryIP4Port), CONNECTION_TIMEOUT, "ConnectionEstablishmentClientFSM", ChannelConstants.CONNECTION_ESTABLISHMENT_CHANNEL);
        } else {
            ccp.registerTimedFSM(new ConnectionEstablishmentServerFSM(this, ownPeerID), CONNECTION_TIMEOUT, "ConnectionEstablishmentServerFSM", ChannelConstants.CONNECTION_ESTABLISHMENT_CHANNEL);
        }
    }

    ConnectionEstablishmentServerFSM.ConnectionResult newRequestConnectionAsServer(PeerID remotePeerID, ChannelConnectionPoint ccp) {
        // the server invokes this one. This PeerClient didn't know about this connection, so it must be confirmed
        // if we have higher priority in the connection process, check that we are not already trying to connect to
        // this same peer as clients
        if (!isWishForFriendSearch()) {
            // check we are disconnected
            return null;
        } else if (connectedPeers.isConnectedPeer(remotePeerID)) {
            // check already connected
            return null;
        } else if ((ownPeerID.hasHigherPriorityThan(remotePeerID) && ongoingClientConnections.contains(remotePeerID))) {
            // check ongoing connection with higher priority
            return null;
        }
        // check what type of relation we have with this peer
        if (peerRelations.isFriendPeer(remotePeerID)) {
            // everything ok, accept connection request and add this peer to the list of connected peers
            newConnection(remotePeerID, ccp, ConnectionStatus.CORRECT);
            return ConnectionEstablishmentServerFSM.ConnectionResult.CORRECT;
        } else if (peerRelations.isBlockedPeer(remotePeerID)) {
            // forbidden peer, deny connection
            return null;
        } else {
            // unknown peer, accept connection but set as validation required
            newConnection(remotePeerID, ccp, ConnectionStatus.UNVALIDATED);
            return ConnectionEstablishmentServerFSM.ConnectionResult.UNKNOWN_FRIEND_PENDING_VALIDATION;
        }
    }

    /**
     * This method is invoked by the connection client FSM to report that connection has been established with a
     * server peer.
     *
     * @param peerID    ID of the peer to which we connected
     * @param ccp       ChannelConnectionPoint object of the connected peer
     * @param validated true if the server peer validated this connection (he acknowledge our friendship),
     *                  false if the server has not acknowledge friendship but accepts the connection
     */
    void connectionAsClientCompleted(PeerID peerID, ChannelConnectionPoint ccp, boolean validated) {
        // the client invokes this one. This PeerClient itself asked for it, so no confirmation is returned
        // if something is wrong, the PeerClient must deal with it itself, since connection is already established
        ongoingClientConnections.remove(peerID);
        if (!isWishForFriendSearch()) {
            ccp.disconnect();
        } else if (connectedPeers.isConnectedPeer(peerID)) {
            // check already connected
            ccp.disconnect();
        } else if (!peerRelations.isFriendPeer(peerID)) {
            // check he is not friend anymore
            ccp.disconnect();
        } else {
            ConnectionStatus status = validated ? ConnectionStatus.CORRECT : ConnectionStatus.WAITING_FOR_REMOTE_VALIDATION;
            newConnection(peerID, ccp, status);
        }
    }

    /**
     * This method allows the connection FSMs to inform that a connection process has concluded (either successfully
     * or not...)
     *
     * @param peerID the ID of the friend whose ongoing connection process has finished
     */
    void connectionAsClientFailed(PeerID peerID, IP4Port secondaryIP4Port, PeerID serverPeerID) {
        // if the connection was not successful, disconnect from the other peer (only for client role)
        ongoingClientConnections.remove(peerID);
        // if there is a secondary address available, try it
        if (secondaryIP4Port != null) {
            try {
                tryConnection(secondaryIP4Port, serverPeerID, null);
            } catch (IOException e) {
                // peer not available or wrong peer data received, repeat soon
                friendSearchReminder.searchIssueDetected();
            }

        }
    }

    /**
     * A new connection with a peer has been achieved (either as client or as server). This method performs the
     * necessary set up for the new connection
     *
     * @param peerID ID of the peer to which we connected
     * @param ccp    ChannelConnectionPoint object of the peer to which we connected
     * @param status the connection status to give to this new peer connection
     */
    private void newConnection(PeerID peerID, ChannelConnectionPoint ccp, ConnectionStatus status) {
        // the request dispatcher for this connections is registered, the connection manager is notified, and the
        // peer is marked in the list of connected peers
        // also our client is informed of this new connection
        // finally, the blocked channels of this connection are resumed, so data transfer can begin
        ccp.registerGenericFSM(new PeerRequestDispatcherFSM(peerClientPrivateInterface, peerID), "PeerRequestDispatcherFSM", ChannelConstants.REQUEST_DISPATCHER_CHANNEL);
        connectedPeers.setConnectedPeer(peerID, ccp, status);
        peerClientPrivateInterface.newPeerConnected(peerID, ccp, status);
        resumeChannels(ccp);
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

    public static byte[] getConcurrentChannelsExceptConnection() {
        byte[] concurrentChannels = new byte[2];
        concurrentChannels[0] = ChannelConstants.REQUEST_DISPATCHER_CHANNEL;
        concurrentChannels[1] = ChannelConstants.RESOURCE_STREAMING_MANAGER_CHANNEL;
        return concurrentChannels;
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
        PeerID peerID = connectedPeers.peerDisconnected(ccp);
        if (peerID != null) {
            peerClientPrivateInterface.peerDisconnected(peerID);
        }
    }

    void peerError(ChannelConnectionPoint ccp, CommError error) {
        PeerID peerID = connectedPeers.peerDisconnected(ccp);
        if (peerID != null) {
            peerClientPrivateInterface.peerError(peerID, error);
        }
    }

    private static Set<PeerID> buildMustDisconnectPeerSet(Set<PeerID> friendPeers, Set<PeerID> blockedPeers, ConnectedPeers connectedPeers) {
        Set<PeerID> mustDisconnectPeerSet = new HashSet<>();
        for (PeerID peerID : connectedPeers.getConnectedPeers()) {
            if (connectedPeers.getPeerConnectionStatus(peerID) == ConnectionStatus.CORRECT && (!friendPeers.contains(peerID) || blockedPeers.contains(peerID))) {
                mustDisconnectPeerSet.add(peerID);
            }
        }
        return mustDisconnectPeerSet;
    }
}
