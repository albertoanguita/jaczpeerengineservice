package jacz.peerengineservice.client.connection;

import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineservice.PeerEncryption;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerClientPrivateInterface;
import jacz.peerengineservice.client.connection.peers.PeerConnectionManager;
import jacz.peerengineservice.client.connection.peers.PeerInfo;
import jacz.peerengineservice.client.connection.peers.PeersEvents;
import jacz.peerengineservice.client.connection.peers.kb.Management;
import jacz.peerengineservice.util.ChannelConstants;
import jacz.peerengineservice.util.PeerRelationship;
import jacz.peerengineservice.util.datatransfer.TransfersConfig;
import org.aanguita.jacuzzi.AI.evolve.EvolvingState;
import org.aanguita.jacuzzi.AI.evolve.EvolvingStateController;
import org.aanguita.jacuzzi.concurrency.ThreadUtil;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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
public class PeerClientConnectionManager implements TransfersConfig {

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
     * Configuration for network setup (local and external ports) provided by the client
     */
    private final NetworkConfiguration networkConfiguration;

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
    private final PeerConnectionManager peerConnectionManager;

    private final ConnectedPeers connectedPeers;

    private final EvolvingState<ConnectionState, Boolean> dynamicState;

    private final AtomicBoolean wishForConnection;

    public PeerClientConnectionManager(
            ConnectionEvents connectionEvents,
            PeerClientPrivateInterface peerClientPrivateInterface,
            ConnectedPeers connectedPeers,
            PeerId ownPeerId,
            PeerEncryption ownPeerEncryption,
            String serverURL,
            String peerConnectionConfigPath,
            String peerKnowledgeBasePath,
            String networkConfigurationPath,
            PeersEvents peersEvents) throws IOException {
        this.connectionEvents = new ConnectionEventsBridge(connectionEvents, this);
        this.networkConfiguration = new NetworkConfiguration(networkConfigurationPath);

        networkTopologyManager = new NetworkTopologyManager(this, this.connectionEvents);
        localServerManager = new LocalServerManager(
                ownPeerId,
                this,
                networkConfiguration,
                this.connectionEvents);
        peerServerManager = new PeerServerManager(ownPeerId, serverURL, this, this.connectionEvents);
        peerConnectionManager = new PeerConnectionManager(
                ownPeerId,
                ownPeerEncryption,
                serverURL,
                peerConnectionConfigPath,
                peerKnowledgeBasePath,
                connectedPeers,
                peerClientPrivateInterface,
                this,
                peersEvents);

        this.connectedPeers = connectedPeers;

        dynamicState = new EvolvingState<>(PeerClientConnectionManager.ConnectionState.DISCONNECTED, false, new EvolvingState.Transitions<ConnectionState, Boolean>() {
            @Override
            public boolean runTransition(ConnectionState connectionState, Boolean goal, EvolvingStateController<ConnectionState, Boolean> controller) {
                if (goal) {
                    switch (connectionState) {
                        case DISCONNECTED:
                        case DISCONNECTING:
                            controller.setState(PeerClientConnectionManager.ConnectionState.CONNECTING);
                            return false;

                        case CONNECTING:
                            // first, connect the network topology manager
                            networkTopologyManager.setWishForConnect(true);
                            if (!networkTopologyManager.isInWishedState()) {
                                // disconnect next services and wait
                                localServerManager.setWishForConnect(false);
                                peerServerManager.setWishForConnect(false);
                                peerConnectionManager.setWishForConnect(false);
                                delay();
                                return false;
                            }

                            // second, open the server for listening to incoming peer connections, then connect to the peer server
                            localServerManager.setWishForConnect(true);
                            if (!localServerManager.isInWishedState()) {
                                peerServerManager.setWishForConnect(false);
                                peerConnectionManager.setWishForConnect(false);
                                delay();
                                return false;
                            }

                            // third, connect to the peer server
                            peerServerManager.setWishForConnect(true);
                            if (!peerServerManager.isInWishedState()) {
                                peerConnectionManager.setWishForConnect(false);
                                delay();
                                return false;
                            }

                            // fourth, activate the friend search
                            peerConnectionManager.setWishForConnect(true);

                            // finally, set the state to connected
                            controller.setState(PeerClientConnectionManager.ConnectionState.CONNECTED);
                            return true;
                    }
                } else {
                    switch (connectionState) {
                        case CONNECTED:
                        case CONNECTING:
                            // disconnect all services and wait for being in wished state
                            disconnectServices();
                            controller.setState(PeerClientConnectionManager.ConnectionState.DISCONNECTING);
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
                            controller.setState(PeerClientConnectionManager.ConnectionState.DISCONNECTED);
                            return true;
                    }
                }
                // else, everything ok
                return true;
            }

            @Override
            public boolean hasReachedGoal(ConnectionState connectionState, Boolean goal) {
                return goal && connectionState == PeerClientConnectionManager.ConnectionState.CONNECTED || !goal && connectionState == PeerClientConnectionManager.ConnectionState.DISCONNECTED;
            }
        }, "PeerClientConnectionManager");
        wishForConnection = new AtomicBoolean(false);
    }

    public NetworkTopologyManager getNetworkTopologyManager() {
        return networkTopologyManager;
    }

//    FriendConnectionManager getFriendConnectionManager() {
//        return friendConnectionManager;
//    }

    PeerConnectionManager getPeerConnectionManager() {
        return peerConnectionManager;
    }

    LocalServerManager getLocalServerManager() {
        return localServerManager;
    }

    public synchronized PeerAddress getPeerAddress() {
        return new PeerAddress(networkTopologyManager.getExternalAddress() + ":" + localServerManager.getActualExternalPort(), networkTopologyManager.getLocalAddress() + ":" + localServerManager.getActualLocalPort());
    }

    public jacz.peerengineservice.client.connection.ConnectionState getConnectionState() {
        return connectionEvents.getState();
    }

//    public NetworkConfiguration buildNetworkConfiguration() {
//        return new NetworkConfiguration(getListeningPort(), localServerManager.getDefaultExternalPort());
//    }

    /**
     * This method sets the necessity of connecting to the stored peer server, or disconnecting from it. After this invocation, the peer client
     * connection manager will do its best to connect to the specified server. The method returns immediately, the connection procedure is performed
     * in a separate thread
     *
     * @param enabled true to connect to the peer server, false otherwise
     */
    public void setWishForConnection(boolean enabled) {
        wishForConnection.set(enabled);
        dynamicState.setGoal(enabled);
    }

    public boolean isWishForConnection() {
        return wishForConnection.get();
    }

    /**
     * Disconnects and stops all resources and blocks until all resources are freed
     */
    public void stop() {
        setWishForConnection(false);
        networkTopologyManager.stop();
        peerServerManager.stop();
        localServerManager.stop();
        peerConnectionManager.stop();
        dynamicState.blockUntilGoalReached(500);
        dynamicState.stop();
        connectionEvents.stop();
    }

//    public synchronized int getListeningPort() {
//        return wishedConnectionInformation.getLocalPort();
//    }

    /**
     * Sets the port at which we must listen to incoming friend connections. Can be set at any time
     *
//     * @param port port to listen to
     */
//    public synchronized void setListeningPort(int port) {
//        if (wishedConnectionInformation.setListeningPort(port)) {
//            connectionEvents.listeningPortModified(port);
//        }
//        peerServerManager.updateState();
//        localServerManager.updateState();
//        dynamicState.evolve();
//    }

    public synchronized int getLocalPort() {
        return networkConfiguration.getLocalPort();
    }

    public synchronized int getExternalPort() {
        return networkConfiguration.getExternalPort();
    }

    public synchronized void setLocalPort(int port) {
        if (networkConfiguration.setLocalPort(port)) {
            reconnectDueToNetworkConfigurationChange();
            connectionEvents.localPortModified(port);
        }
    }

    public synchronized void setExternalPort(int port) {
        if (networkConfiguration.setExternalPort(port)) {
            reconnectDueToNetworkConfigurationChange();
            connectionEvents.externalPortModified(port);
        }
    }

    private void reconnectDueToNetworkConfigurationChange() {
        boolean mustReconnect = dynamicState.goal();
        setWishForConnection(false);
        dynamicState.blockUntilGoalReached(500);
        if (mustReconnect) {
            setWishForConnection(true);
        }
    }

    void networkProblem() {
        // the local address is not available due to some problem in the local network
        // the user has been notified. Disconnect services until we have connection.
        dynamicState.evolve();
    }

    private void disconnectServices() {
        peerConnectionManager.setWishForConnect(false);
        peerServerManager.setWishForConnect(false);
        connectedPeers.disconnectAllPeers();
        localServerManager.setWishForConnect(false);
        networkTopologyManager.setWishForConnect(false);
    }

    public synchronized PeerRelationship getPeerRelationship(PeerId peerId) {
        return peerConnectionManager.getPeerRelationship(peerId);
    }

    public synchronized boolean isFavoritePeer(PeerId peerId) {
        return peerConnectionManager.isFavoritePeer(peerId);
    }

    public synchronized boolean isRegularPeer(PeerId peerId) {
        return peerConnectionManager.isRegularPeer(peerId);
    }

    public synchronized boolean isBlockedPeer(PeerId peerId) {
        return peerConnectionManager.isBlockedPeer(peerId);
    }

    public synchronized Set<PeerId> getFavoritePeers() {
        return peerConnectionManager.getFavoritePeers();
    }

    public synchronized void addFavoritePeer(PeerId peerId) {
        peerConnectionManager.addFavoritePeer(peerId);
    }

    public synchronized void removeFavoritePeer(PeerId peerId) {
        peerConnectionManager.removeFavoritePeer(peerId);
    }

    public synchronized Set<PeerId> getBlockedPeers() {
        return peerConnectionManager.getBlockedPeers();
    }

    public synchronized void addBlockedPeer(PeerId peerId) {
        peerConnectionManager.addBlockedPeer(peerId);
    }

    public synchronized void removeBlockedPeer(PeerId peerId) {
        peerConnectionManager.removeBlockedPeer(peerId);
    }

    /**
     * Performs a connected friend search. Searches are performed periodically, but the user can force a search using this method. If we are not
     * connected to the server, this method will have no effect
     */
    public void searchFavorites() {
        peerConnectionManager.searchFavorites();
    }

    public void clearAllPeerAddresses() {
        peerConnectionManager.clearAllPeerAddresses();
    }

    public void clearAllData() {
        peerConnectionManager.clearAllData();
    }

    public boolean isWishForRegularConnections() {
        return peerConnectionManager.isWishForRegularConnections();
    }

    public void setWishForRegularsConnections(boolean enabled) {
        peerConnectionManager.setWishForRegularsConnections(enabled);
    }

    public int getMaxRegularConnections() {
        return peerConnectionManager.getMaxRegularConnections();
    }

    public void setMaxRegularConnections(int maxRegularConnections) {
        peerConnectionManager.setMaxRegularConnections(maxRegularConnections);
    }

    public int getMaxRegularConnectionsForAdditionalCountries() {
        return peerConnectionManager.getMaxRegularConnectionsForAdditionalCountries();
    }

    public void setMaxRegularConnectionsForAdditionalCountries(int maxRegularConnections) {
        peerConnectionManager.setMaxRegularConnectionsForAdditionalCountries(maxRegularConnections);
    }

    public int getMaxRegularConnectionsForOtherCountries() {
        return peerConnectionManager.getMaxRegularConnectionsForOtherCountries();
    }

    public CountryCode getMainCountry() {
        return peerConnectionManager.getMainCountry();
    }

    public void setMainCountry(CountryCode mainCountry) {
        peerConnectionManager.setMainCountry(mainCountry);
    }

    public List<CountryCode> getAdditionalCountries() {
        return peerConnectionManager.getAdditionalCountries();
    }

    public boolean isAdditionalCountry(CountryCode country) {
        return peerConnectionManager.isAdditionalCountry(country);
    }

    public void setAdditionalCountries(List<CountryCode> additionalCountries) {
        peerConnectionManager.setAdditionalCountries(additionalCountries);
    }

    public synchronized Float getMaxDownloadSpeed() {
        return peerConnectionManager.getMaxDownloadSpeed();
    }

    public synchronized void setMaxDownloadSpeed(Float speed) {
        peerConnectionManager.setMaxDownloadSpeed(speed);
    }

    public synchronized Float getMaxUploadSpeed() {
        return peerConnectionManager.getMaxUploadSpeed();
    }

    public synchronized void setMaxUploadSpeed(Float speed) {
        peerConnectionManager.setMaxUploadSpeed(speed);
    }

    public double getDownloadPartSelectionAccuracy() {
        return peerConnectionManager.getDownloadPartSelectionAccuracy();
    }

    public void setDownloadPartSelectionAccuracy(double accuracy) {
        peerConnectionManager.setDownloadPartSelectionAccuracy(accuracy);
    }

    private void delay() {
        ThreadUtil.safeSleep(DELAY);
    }

    /**
     * The peer server manager reports that he received an unrecognized server message. Stop trying to connect and
     * notify client
     */
    public void unrecognizedServerMessage() {
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

    public PeerInfo getPeerInfo(PeerId peerId) {
        return peerConnectionManager.getPeerInfo(peerId);
    }

    public int getPeerAffinity(PeerId peerId) {
        return peerConnectionManager.getPeerAffinity(peerId);
    }

    public void updatePeerAffinity(PeerId peerId, int affinity) {
        peerConnectionManager.updatePeerAffinity(peerId, affinity);
    }

    public void newPeerNick(PeerId peerId, String nick) {
        peerConnectionManager.newPeerNick(peerId, nick);
    }

    public void newRelationshipToUs(PeerId peerId, Management.Relationship relationship) {
        peerConnectionManager.newRelationshipToUs(peerId, relationship);
    }

    public void peerModifiedHisMainCountry(PeerId peerId, CountryCode mainCountry) {
        peerConnectionManager.peerModifiedHisMainCountry(peerId, mainCountry);
    }
}
