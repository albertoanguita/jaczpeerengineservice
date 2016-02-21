package jacz.peerengineservice.client.connection;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClientPrivateInterface;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.util.ChannelConstants;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.concurrency.daemon.Daemon;
import jacz.util.concurrency.daemon.DaemonAction;

import java.util.HashSet;
import java.util.Set;

/**
 * This class (and all related classes in this package) are basically in charge of connecting to the friend peers.
 * That's their only task. Once they start obtaining connections with other clients, they will report and provide the
 * obtained ChannelConnectionPoint.
 * This class also creates the ServerModule needed to receive connections from other clients.
 * <p/>
 * Every time connection is achieved with a friend peer (either connecting to him or receiving a connection), this
 * class will inform its client through the appropriate interface
 * <p/>
 * This class works with a one-step-at-a-time scheme. The idea is that the client, through some set methods, can establish whether he wishes us
 * to connect to the server or not, to what server, and which port to listen to connections from. Periodically, we will perform one step to be
 * closer to what he wishes. This way the code is much simpler and clearer (it was getting to messy to code the complete workflow in order to
 * connect to the server and to all available friends).
 * <p/>
 * We store a few variables to know in which state are we (connected to the server or not, open server for friends to connect or closed, server
 * data to which we are connected, port we are using for listening to incoming connections, etc)
 */
public class PeerClientConnectionManager implements DaemonAction {

    /**
     * This enum indicates the possibilities for the client wishing us to establish connection with the server or not
     */
    private enum ClientsWishForConnection {
        POSITIVE,
        NEGATIVE
    }

//    public enum WishState {
//        NO,
//        YES,
//        NO_BUT_WAITING
//    }

    /**
     * Actions to invoke upon certain events
     */
    private final ConnectionEventsBridge connectionEvents;

    /**
     * Clients wish for us to connect or not to the peer server and open the server for peer connections (altogether) (can be dynamically set)
     */
    private ClientsWishForConnection clientsWishForConnection;

    /**
     * Collection of all information related to connection to the server, as provided by the user
     */
    private final ConnectionInformation wishedConnectionInformation;

    /**
     * Daemon thread that helps keep the state in its desired values
     */
    private final Daemon stateDaemon;

    /**
     * Handles detection of network properties
     */
    private final NetworkTopologyManager networkTopologyManager;

    /**
     * Handles connection with the server
     */
    private final PeerServerManager peerServerManager;

    /**
     * Manager for the server that accepts connections from other peers
     */
    private final LocalServerManager localServerManager;

    /**
     * Manages the connections with friend peers
     */
    private final FriendConnectionManager friendConnectionManager;


    public PeerClientConnectionManager(
            ConnectionEvents connectionEvents,
            PeerClientPrivateInterface peerClientPrivateInterface,
            ConnectedPeers connectedPeers,
            PeerID ownPeerID,
            NetworkConfiguration networkConfiguration,
            PeerRelations peerRelations) {
        this.connectionEvents = new ConnectionEventsBridge(connectionEvents, this);

        clientsWishForConnection = ClientsWishForConnection.NEGATIVE;
        wishedConnectionInformation = new ConnectionInformation();
        wishedConnectionInformation.setListeningPort(networkConfiguration.getLocalPort());

        stateDaemon = new Daemon(this);

        networkTopologyManager = new NetworkTopologyManager(this, this.connectionEvents);
        peerServerManager = new PeerServerManager(ownPeerID, this, networkTopologyManager, this.connectionEvents);
        friendConnectionManager = new FriendConnectionManager(ownPeerID, connectedPeers, peerClientPrivateInterface, this, peerRelations);
        localServerManager = new LocalServerManager(
                ownPeerID,
                networkConfiguration.getExternalPort(),
                networkTopologyManager,
                friendConnectionManager,
                wishedConnectionInformation,
                this.connectionEvents);
        peerServerManager.setLocalServerManager(localServerManager);
    }

    public State getConnectionState() {
        return connectionEvents.getState();
    }

    public NetworkConfiguration buildNetworkConfiguration() {
        return new NetworkConfiguration(getListeningPort(), localServerManager.getDefaultExternalPort());
    }

    /**
     * This method sets the necessity of connecting to the stored peer server, or disconnecting from it. After this invocation, the peer client
     * connection manager will do its best to connect to the specified server. The method returns immediately, the connection procedure is performed
     * in a separate thread
     *
     * @param enabled true to connect to the peer server, false otherwise
     */
    public synchronized void setWishForConnection(boolean enabled) {
        if (enabled) {
            clientsWishForConnection = ClientsWishForConnection.POSITIVE;
        } else {
            clientsWishForConnection = ClientsWishForConnection.NEGATIVE;
        }
        updateState();
    }

    /**
     * Disconnects and stops all resources and blocks until all resources are freed
     */
    public void stop() {
        setWishForConnection(false);
        networkTopologyManager.stop();
        peerServerManager.stop();
        localServerManager.stop();
        friendConnectionManager.stop();
    }

    public synchronized int getListeningPort() {
        return wishedConnectionInformation.getLocalPort();
    }

    /**
     * Sets the port at which we must listen to incoming friend connections. Can be set at any time
     *
     * @param port port to listen to
     */
    public synchronized void setListeningPort(int port) {
        if (wishedConnectionInformation.setListeningPort(port)) {
            connectionEvents.listeningPortModified(port);
        }
        peerServerManager.updateState();
        localServerManager.updateState();
        updateState();
    }

    void networkProblem() {
        // the local address is not available due to some problem in the local network
        // the user has been notified. Disconnect services until we have connection.
        updateState();
    }

    void publicIPMismatch(State.ConnectionToServerState connectionToServerStatus) {
        connectionEvents.connectionParametersChanged(connectionToServerStatus);
        networkTopologyManager.publicIPMismatch();
        updateState();
    }

    private void disconnectServices() {
        friendConnectionManager.setWishForFriendSearch(false);
        peerServerManager.setWishForConnect(false);
        friendConnectionManager.disconnectAllPeers();
        localServerManager.setWishForConnect(false);
        networkTopologyManager.setWishForConnect(false);
    }

    private void updateState() {
        stateDaemon.stateChange();
        stateDaemon.interrupt();
    }


    /**
     * Performs a connected friend search. Searches are performed periodically, but the user can force a search using this method. If we are not
     * connected to the server, this method will have no effect
     */
    public synchronized void searchFriends() {
        friendConnectionManager.searchFriends();
    }

    /**
     * Executes one step towards fulfilling the desired state
     *
     * @return true if the state is now the desired one, false if more steps are required
     */
    @Override
    public synchronized boolean solveState() {
        // in any case, solve the network topology first
//        if (!networkTopologyManager.isInWishedState()) {
//        switch (networkTopologyManager.isInWishedState()) {
//            case NO:
//                disconnectServices();
//                longDelay();
//                return false;
//
//            case NO_BUT_WAITING:
//                return true;
//        }

        if (clientsWishForConnection == ClientsWishForConnection.POSITIVE) {
            // first, connect the network topology manager
            networkTopologyManager.setWishForConnect(true);
            if (!networkTopologyManager.isInWishedState()) {
                    // disconnect next services and wait
                    localServerManager.setWishForConnect(false);
                    peerServerManager.setWishForConnect(false);
                    friendConnectionManager.setWishForFriendSearch(false);
                    longDelay();
                    return false;
            }

            // second, open the server for listening to incoming peer connections, then connect to the peer server
            localServerManager.setWishForConnect(true);
            if (!localServerManager.isInWishedState()) {
//                localServerManager.updateState();
                peerServerManager.setWishForConnect(false);
                friendConnectionManager.setWishForFriendSearch(false);
                shortDelay();
                return false;
            }

            // third, connect to the peer server
            peerServerManager.setWishForConnect(true);
            if (!peerServerManager.isInWishedState()) {
//                peerServerManager.updateState();
                friendConnectionManager.setWishForFriendSearch(false);
                longDelay();
                return false;
            }

            // fourth, activate the friend search
            friendConnectionManager.setWishForFriendSearch(true);

        } else {
            // disconnect all services and wait for being in wished state
            disconnectServices();

            if (!peerServerManager.isInWishedState()) {
                peerServerManager.updateState();
                longDelay();
                return false;
            }

            if (!localServerManager.isInWishedState()) {
                shortDelay();
                return false;
            }

            if (!networkTopologyManager.isInWishedState()) {
                shortDelay();
                return false;
            }

            // finally, disconnect all remaining peers
            friendConnectionManager.disconnectAllPeers();
        }
        // all connection status is ok
        return true;
    }

    private void shortDelay() {
        ThreadUtil.safeSleep(500);
    }

    private void longDelay() {
        ThreadUtil.safeSleep(1000);
    }

    /**
     * The peer server manager reports that he received an unrecognized server message. Stop trying to connect and
     * notify client
     */
    void unrecognizedServerMessage() {
        connectionEvents.unrecognizedMessageFromServer(peerServerManager.getConnectionToServerStatus());
        setWishForConnection(false);
        updateState();
    }

    static Set<Set<Byte>> generateConcurrentChannelSets() {
        // there will be three concurrent sets. One for the request dispatcher, one for the data streaming, and one
        // for the rest of channels (including the channel for establishing connection)
        // the connection channel is concurrent from the rest
        Set<Set<Byte>> concurrentChannels = new HashSet<>(3);

        Set<Byte> requestsChannel = new HashSet<>(1);
        requestsChannel.add(ChannelConstants.REQUEST_DISPATCHER_CHANNEL);
        concurrentChannels.add(requestsChannel);

        Set<Byte> dataStreamingChannel = new HashSet<>(1);
        dataStreamingChannel.add(ChannelConstants.RESOURCE_STREAMING_MANAGER_CHANNEL);
        concurrentChannels.add(dataStreamingChannel);

        Set<Byte> restOfChannels = new HashSet<>(255);
        for (Byte channel = ChannelConstants.RESOURCE_STREAMING_MANAGER_CHANNEL + 1; channel != ChannelConstants.REQUEST_DISPATCHER_CHANNEL; channel++) {
            restOfChannels.add(channel);
        }
        concurrentChannels.add(restOfChannels);
        return concurrentChannels;
    }
}
