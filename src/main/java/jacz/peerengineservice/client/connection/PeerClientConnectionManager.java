package jacz.peerengineservice.client.connection;

import jacz.peerengineservice.PeerEncryption;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerClientPrivateInterface;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.connection.peers.PeerConnectionManager;
import jacz.peerengineservice.util.ChannelConstants;
import jacz.util.AI.evolve.EvolvingState;
import jacz.util.AI.evolve.EvolvingStateController;
import jacz.util.concurrency.ThreadUtil;

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
public class PeerClientConnectionManager {

    private enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING
    }

    private static final long DELAY = 500L;

    /**
     * Actions to invoke upon certain events
     */
    private final ConnectionEventsBridge connectionEvents;

    /**
     * Collection of all information related to connection to the server, as provided by the user
     */
    private final ConnectionInformation wishedConnectionInformation;

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
    private final FriendConnectionManager friendConnectionManager; // todo remove
    private final PeerConnectionManager peerConnectionManager;

    private final EvolvingState<ConnectionState, Boolean> dynamicState;

    public PeerClientConnectionManager(
            ConnectionEvents connectionEvents,
            PeerClientPrivateInterface peerClientPrivateInterface,
            ConnectedPeers connectedPeers,
            PeerId ownPeerId,
            PeerEncryption ownPeerEncryption,
            String serverURL,
            NetworkConfiguration networkConfiguration,
            PeerRelations peerRelations) {
        this.connectionEvents = new ConnectionEventsBridge(connectionEvents, this);

        wishedConnectionInformation = new ConnectionInformation();
        wishedConnectionInformation.setListeningPort(networkConfiguration.getLocalPort());

        networkTopologyManager = new NetworkTopologyManager(this, this.connectionEvents);
        localServerManager = new LocalServerManager(
                ownPeerId,
                networkConfiguration.getExternalPort(),
                networkTopologyManager,
                this,
                wishedConnectionInformation,
                this.connectionEvents);
        peerServerManager = new PeerServerManager(ownPeerId, serverURL, this, networkTopologyManager, this.connectionEvents);
        friendConnectionManager = new FriendConnectionManager(ownPeerId, serverURL, connectedPeers, peerClientPrivateInterface, this, peerRelations);
        peerConnectionManager = new PeerConnectionManager(ownPeerId, ownPeerEncryption, , serverURL, , , connectedPeers, peerClientPrivateInterface);

        dynamicState = new EvolvingState<>(ConnectionState.DISCONNECTED, false, new EvolvingState.Transitions<ConnectionState, Boolean>() {
            @Override
            public boolean runTransition(ConnectionState state, Boolean goal, EvolvingStateController<ConnectionState, Boolean> controller) {
                if (goal) {
                    switch (state) {
                        case DISCONNECTED:
                        case DISCONNECTING:
                            controller.setState(ConnectionState.CONNECTING);
                            return false;

                        case CONNECTING:
                            // first, connect the network topology manager
                            networkTopologyManager.setWishForConnect(true);
                            if (!networkTopologyManager.isInWishedState()) {
                                // disconnect next services and wait
                                localServerManager.setWishForConnect(false);
                                peerServerManager.setWishForConnect(false);
                                friendConnectionManager.setWishForFriendSearch(false);
                                delay();
                                return false;
                            }

                            // second, open the server for listening to incoming peer connections, then connect to the peer server
                            localServerManager.setWishForConnect(true);
                            if (!localServerManager.isInWishedState()) {
                                peerServerManager.setWishForConnect(false);
                                friendConnectionManager.setWishForFriendSearch(false);
                                delay();
                                return false;
                            }

                            // third, connect to the peer server
                            peerServerManager.setWishForConnect(true);
                            if (!peerServerManager.isInWishedState()) {
                                friendConnectionManager.setWishForFriendSearch(false);
                                delay();
                                return false;
                            }

                            // fourth, activate the friend search
                            friendConnectionManager.setWishForFriendSearch(true);

                            // finally, set the state to connected
                            controller.setState(ConnectionState.CONNECTED);
                            return true;
                    }
                } else {
                    switch (state) {
                        case CONNECTED:
                        case CONNECTING:
                            // disconnect all services and wait for being in wished state
                            disconnectServices();
                            controller.setState(ConnectionState.DISCONNECTING);
                            return false;

                        case DISCONNECTING:
                            if (!peerServerManager.isInWishedState()) {
                                delay();
                                return false;
                            }

                            if (!localServerManager.isInWishedState()) {
                                delay();
                                return false;
                            }

                            if (!networkTopologyManager.isInWishedState()) {
                                delay();
                                return false;
                            }

                            // and set the state to disconnected
                            controller.setState(ConnectionState.DISCONNECTED);
                            return true;
                    }
                }
                // else, everything ok
                return true;
            }

            @Override
            public boolean hasReachedGoal(ConnectionState state, Boolean goal) {
                return goal && state == ConnectionState.CONNECTED || !goal && state == ConnectionState.DISCONNECTED;
            }
        }
        );
    }

    FriendConnectionManager getFriendConnectionManager() {
        return friendConnectionManager;
    }

    PeerConnectionManager getPeerConnectionManager() {
        return peerConnectionManager;
    }

    LocalServerManager getLocalServerManager() {
        return localServerManager;
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
        dynamicState.setGoal(enabled);
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
        dynamicState.blockUntilGoalReached(500);
        dynamicState.stop();
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
        dynamicState.evolve();
    }

    void networkProblem() {
        // the local address is not available due to some problem in the local network
        // the user has been notified. Disconnect services until we have connection.
        dynamicState.evolve();
    }

    private void disconnectServices() {
        friendConnectionManager.setWishForFriendSearch(false);
        peerServerManager.setWishForConnect(false);
        friendConnectionManager.disconnectAllPeers();
        localServerManager.setWishForConnect(false);
        networkTopologyManager.setWishForConnect(false);
    }

    /**
     * Performs a connected friend search. Searches are performed periodically, but the user can force a search using this method. If we are not
     * connected to the server, this method will have no effect
     */
    public synchronized void searchFriends() {
        friendConnectionManager.searchFriends();
    }


    private void delay() {
        ThreadUtil.safeSleep(DELAY);
    }

    /**
     * The peer server manager reports that he received an unrecognized server message. Stop trying to connect and
     * notify client
     */
    void unrecognizedServerMessage() {
        connectionEvents.unrecognizedMessageFromServer(peerServerManager.getConnectionToServerStatus());
        setWishForConnection(false);
        dynamicState.evolve();
    }

    public static Set<Set<Byte>> generateConcurrentChannelSets() {
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
