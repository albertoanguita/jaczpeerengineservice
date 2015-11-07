package jacz.peerengineservice.client.connection;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClientPrivateInterface;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeerServerData;
import jacz.peerengineservice.util.ChannelConstants;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.concurrency.daemon.Daemon;
import jacz.util.concurrency.daemon.DaemonAction;
import jacz.util.concurrency.timer.SimpleTimerAction;
import jacz.util.concurrency.timer.Timer;

import java.net.InetAddress;
import java.net.UnknownHostException;
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
     * This class checks that the local internet address is not modified. In case of modification, it notifies the PeerClientConfigurationManager
     * <p/>
     * This class must be stopped after its use
     */
    private static class LocalAddressChecker implements SimpleTimerAction {

        private final static long LOCAL_ADDRESS_TIMER = 60000L;

        private final PeerClientConnectionManager peerClientConnectionManager;

        private final Timer timer;

        private LocalAddressChecker(PeerClientConnectionManager peerClientConnectionManager) {
            this.peerClientConnectionManager = peerClientConnectionManager;
            timer = new Timer(LOCAL_ADDRESS_TIMER, this, true, "LocalAddressChecker");
        }

        @Override
        public Long wakeUp(Timer timer) {
            peerClientConnectionManager.updateLocalAddress(detectLocalAddress());
            return null;
        }

        /**
         * Detects the local address assigned to our machine
         *
         * @return the value has changed from the last detection
         */
        public static InetAddress detectLocalAddress() {
            try {
                return InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                return null;
            }
        }


        public void stop() {
            timer.kill();
        }
    }

    /**
     * This enum indicates the possibilities for the client wishing us to establish connection with the server or not
     */
    private enum ClientsWishForConnection {
        POSITIVE,
        NEGATIVE
    }

    /**
     * Actions to invoke by the PeerClientConnectionManager in order to communicate with the PeerClient which owns us
     */
    private PeerClientPrivateInterface peerClientPrivateInterface;

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
     * Periodically checks the local address
     */
    private final LocalAddressChecker localAddressChecker;

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
            PeerClientPrivateInterface peerClientPrivateInterface,
            ConnectedPeers connectedPeers,
            PeerID ownPeerID,
            int port,
            PeerServerData peerServerData,
            PeerRelations peerRelations) {
        this.peerClientPrivateInterface = peerClientPrivateInterface;

        clientsWishForConnection = ClientsWishForConnection.NEGATIVE;
        wishedConnectionInformation = new ConnectionInformation(LocalAddressChecker.detectLocalAddress(), peerServerData, port);

        stateDaemon = new Daemon(this);

        localAddressChecker = new LocalAddressChecker(this);

        peerServerManager = new PeerServerManager(ownPeerID, peerClientPrivateInterface, wishedConnectionInformation);
        friendConnectionManager = new FriendConnectionManager(ownPeerID, peerServerManager, connectedPeers, peerClientPrivateInterface, peerRelations);
        localServerManager = new LocalServerManager(friendConnectionManager, peerClientPrivateInterface, wishedConnectionInformation);
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
            if (wishedConnectionInformation.getLocalInetAddress() == null) {
                peerClientPrivateInterface.undefinedOwnInetAddress();
                clientsWishForConnection = ClientsWishForConnection.NEGATIVE;
            } else {
                clientsWishForConnection = ClientsWishForConnection.POSITIVE;
            }
        } else {
            clientsWishForConnection = ClientsWishForConnection.NEGATIVE;
        }
        updatedState();
    }

    /**
     * Disconnects and stops all resources and blocks until all resources are freed
     */
    public void stop() {
        setWishForConnection(false);
        localAddressChecker.stop();
        peerServerManager.stop();
        localServerManager.stop();
        friendConnectionManager.stop();
    }

    /**
     * Retrieves the currently set peer server data
     *
     * @return the last peer server data set
     */
    public synchronized PeerServerData getPeerServerData() {
        return wishedConnectionInformation.getPeerServerData();
    }

    /**
     * Sets the data about the peer server to which we must connect to. This value can be set at any time, even when we are already connected to
     * another server. In this case the manager will disconnect and then connect to the new server
     *
     * @param peerServerData data about the server to connect to
     */
    public synchronized void setPeerServerData(PeerServerData peerServerData) {
        wishedConnectionInformation.setPeerServerData(peerServerData);
        peerServerManager.updatedState();
        updatedState();
    }

    public synchronized int getListeningPort() {
        return wishedConnectionInformation.getListeningPort();
    }

    /**
     * Sets the port at which we must listen to incoming friend connections. Can be set at any time
     *
     * @param port port to listen to
     */
    public synchronized void setListeningPort(int port) {
        if (wishedConnectionInformation.setListeningPort(port)) {
            peerClientPrivateInterface.listeningPortModified(port);
        }
        peerServerManager.updatedState();
        localServerManager.updatedState();
        updatedState();
    }

    /**
     * Sets the local address assigned to our machine (null if not detectable)
     *
     * @param inetAddress detected local address
     */
    synchronized void updateLocalAddress(InetAddress inetAddress) {
        if (inetAddress == null || !inetAddress.equals(wishedConnectionInformation.getLocalInetAddress())) {
            if (inetAddress == null) {
                setWishForConnection(false);
                peerClientPrivateInterface.undefinedOwnInetAddress();
            }
            wishedConnectionInformation.setLocalInetAddress(inetAddress);
            peerServerManager.updatedState();
            updatedState();
        }
    }

    private void updatedState() {
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
        if (clientsWishForConnection == ClientsWishForConnection.POSITIVE) {
            // first open the server for listening to incoming peer connections, then connect to the peer server
            localServerManager.setWishForConnect(true);
            if (!localServerManager.isInWishedState()) {
                localServerManager.updatedState();
                shortDelay();
                return false;
            }

            // second, connect to the peer server
            peerServerManager.setWishForConnect(true);
            if (!peerServerManager.isInWishedState()) {
                peerServerManager.updatedState();
                longDelay();
                return false;
            }

            // third, activate the friend search
            friendConnectionManager.setWishForFriendSearch(true);

        } else {
            // first deactivate friend search
            friendConnectionManager.setWishForFriendSearch(false);

            // second, disconnect from the peer server
            peerServerManager.setWishForConnect(false);
            if (!peerServerManager.isInWishedState()) {
                peerServerManager.updatedState();
                longDelay();
                return false;
            }

            // third, close our server for accepting peer connections and kick all connected peers
            localServerManager.setWishForConnect(false);
            if (!localServerManager.isInWishedState()) {
                shortDelay();
                return false;
            }

            // fourth, disconnect all remaining peers
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
