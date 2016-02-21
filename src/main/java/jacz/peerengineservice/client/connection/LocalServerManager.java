package jacz.peerengineservice.client.connection;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.clientserver.server.ServerAction;
import jacz.commengine.clientserver.server.ServerModule;
import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerID;
import jacz.util.concurrency.daemon.Daemon;
import jacz.util.concurrency.daemon.DaemonAction;
import jacz.util.concurrency.timer.SimpleTimerAction;
import jacz.util.concurrency.timer.Timer;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.network.IP4Port;
import org.bitlet.weupnp.GatewayDevice;

import java.io.IOException;

/**
 * This class manages the server for listening to other peers connecting to us
 */
public class LocalServerManager implements DaemonAction {

    /**
     * Used by the ServerModule of the PeerClientConnectionManager, to listen to new connections from other peers. This
     * class also handles every event related to used ChannelConnectionPoints of peers currently connected to use
     * (received messages, freed channels, etc). Received messages are ignored since every communication with other
     * peers is carried out through FSMs, not through direct messaging. The freeing of a channel does have to be notified
     * to the PeerClient.
     */
    private static class PeerClientServerActionImpl implements ServerAction {

        private final FriendConnectionManager friendConnectionManager;

        private final ConnectionEventsBridge connectionEvents;


        /**
         * Class constructor
         *
         * @param friendConnectionManager the FriendConnectionManager that handles friends events
         */
        public PeerClientServerActionImpl(
                FriendConnectionManager friendConnectionManager,
                ConnectionEventsBridge connectionEvents) {
            this.friendConnectionManager = friendConnectionManager;
            this.connectionEvents = connectionEvents;
        }

        @Override
        public void newClientConnection(UniqueIdentifier clientID, ChannelConnectionPoint ccp, IP4Port ip4Port) {
            friendConnectionManager.reportClientConnectedToOurPeerServer(ccp);
        }

        @Override
        public void newMessage(UniqueIdentifier clientID, ChannelConnectionPoint ccp, byte channel, Object message) {
            // ignore
        }

        @Override
        public void newMessage(UniqueIdentifier clientID, ChannelConnectionPoint ccp, byte channel, byte[] data) {
            // ignore
        }

        @Override
        public void channelFreed(UniqueIdentifier clientID, ChannelConnectionPoint ccp, byte channel) {
            friendConnectionManager.channelFreed(ccp, channel);
        }

        @Override
        public void clientDisconnected(UniqueIdentifier clientID, ChannelConnectionPoint ccp, boolean expected) {
            friendConnectionManager.peerDisconnected(ccp);
        }

        @Override
        public void clientError(UniqueIdentifier clientID, ChannelConnectionPoint ccp, CommError e) {
            friendConnectionManager.peerError(ccp, e);
        }

        @Override
        public void newConnectionError(Exception e, IP4Port ip4Port) {
            connectionEvents.peerCouldNotConnectToUs(e, ip4Port);
        }

        @Override
        public void TCPServerError(Exception e) {
            connectionEvents.localServerError(e);
        }
    }

    private static class RetryReminder implements SimpleTimerAction {

        private static final long RETRY_CONNECTION_DELAY = 25000L;

        private final LocalServerManager localServerManager;

        private final Timer timer;

        private RetryReminder(LocalServerManager localServerManager) {
            this.localServerManager = localServerManager;
            timer = new Timer(RETRY_CONNECTION_DELAY, this, false, "LocalServerManager.RetryReminder");
        }

        synchronized void mustRetryConnection() {
            timer.reset();
        }

        synchronized void stop() {
            timer.kill();
        }

        @Override
        public Long wakeUp(Timer timer) {
            localServerManager.finishWaitForConnectionRetry();
            return 0L;
        }
    }


    private static final String NAT_RULE_DESCRIPTION_INIT = "JCZ_";

    private static final int NAT_RULE_CHARACTER_COUNT = 6;


    private final PeerID ownPeerID;

    private final int defaultExternalPort;

    private int externalPort;

    private final NetworkTopologyManager networkTopologyManager;

    private final FriendConnectionManager friendConnectionManager;

    /**
     * Actions to invoke upon certain events
     */
    private final ConnectionEventsBridge connectionEvents;

    /**
     * ServerModule object employed to receive connections from friend peers
     */
    private ServerModule serverModule;

    private boolean wishForConnect;

    /**
     * Daemon thread that helps keep the state in its desired values
     */
    private final Daemon stateDaemon;

    /**
     * Status of the server for listening to incoming peer connections
     */
    private State.LocalServerConnectionsState localServerConnectionsState;

    /**
     * Collection of all information related to connection to the server, as provided by the user
     */
    private ConnectionInformation wishedConnectionInformation;

    /**
     * Collection of all information related to connection to the server (what we are currently using)
     */
    private int listeningPort;

    private RetryReminder retryReminder;


    public LocalServerManager(
            PeerID ownPeerID,
            int defaultExternalPort,
            NetworkTopologyManager networkTopologyManager,
            FriendConnectionManager friendConnectionManager,
            ConnectionInformation wishedConnectionInformation,
            ConnectionEventsBridge connectionEvents) {
        this.ownPeerID = ownPeerID;
        this.defaultExternalPort = defaultExternalPort;
        externalPort = -1;
        this.networkTopologyManager = networkTopologyManager;
        this.friendConnectionManager = friendConnectionManager;
        this.connectionEvents = connectionEvents;
        serverModule = null;
        wishForConnect = false;
        stateDaemon = new Daemon(this);
        localServerConnectionsState = State.LocalServerConnectionsState.CLOSED;
        this.wishedConnectionInformation = wishedConnectionInformation;
        listeningPort = -1;
        this.retryReminder = new RetryReminder(this);
    }

    public int getDefaultExternalPort() {
        return defaultExternalPort;
    }

    /**
     * When the local server is open, it returns the port at which this server listens for new connections
     *
     * @return the local listening port
     */
    synchronized Integer getActualListeningPort() {
        return serverModule != null ? serverModule.getActualListeningPort() : null;
    }

    synchronized Integer getExternalListeningPort() {
        return localServerConnectionsState == State.LocalServerConnectionsState.LISTENING ? externalPort : null;
    }

    synchronized void setWishForConnect(boolean wishForConnect) {
        boolean mustUpdateState = this.wishForConnect != wishForConnect;
        this.wishForConnect = wishForConnect;
        if (mustUpdateState) {
            updateState();
        }
    }

    synchronized boolean isInWishedState() {
        if (wishForConnect) {
            return localServerConnectionsState == State.LocalServerConnectionsState.LISTENING;
        } else {
            return localServerConnectionsState == State.LocalServerConnectionsState.CLOSED;
        }
    }

    private boolean isCorrectConnectionInformation() {
        return listeningPort == wishedConnectionInformation.getLocalPort();
    }

    void stop() {
        setWishForConnect(false);
        retryReminder.stop();
        stateDaemon.blockUntilStateIsSolved();
    }

    synchronized void updateState() {
        stateDaemon.stateChange();
        stateDaemon.interrupt();
    }

    @Override
    public synchronized boolean solveState() {
        if (wishForConnect) {
            // first open the server for listening to incoming peer connections, then connect to the peer server
            switch (localServerConnectionsState) {

                case CLOSED:
                    // open the server for listening connections from other peers
                    openPeerConnectionsServer();
                    return false;
                case OPEN:
                    // check gateway
                    if (listeningPort == 0 && networkTopologyManager.hasGateway()) {
                        // we must create a NAT rule in the gateway for connection to be able to reach us
                        externalPort = createGatewayForwardingRule(defaultExternalPort);
                        // finish if we could not create the NAT rule (timer is set to try again later)
                        return externalPort == -1;
                    } else {
                        // fixed port (rely on user to appropriately forward ports) or no gateway,
                        // we can move to LISTENING state
                        localServerConnectionsState = State.LocalServerConnectionsState.LISTENING;
                        externalPort = wishedConnectionInformation.getLocalPort();
                        connectionEvents.listeningConnectionsWithoutNATRule(externalPort, listeningPort, localServerConnectionsState);
                        return false;
                    }
                case WAITING_FOR_OPENING_TRY:
                    return true;
                case LISTENING:
                    // check the correct listening port is being used
                    if (!isCorrectConnectionInformation()) {
                        destroyGatewayForwardingRule(externalPort);
                        closePeerConnectionsServer();
                        return false;
                    } else {
                        return true;
                    }
                case WAITING_FOR_NAT_RULE_TRY:
                    return true;
            }
        } else {
            switch (localServerConnectionsState) {
                case LISTENING:
                    // we must close our server and kick all connected clients
                    if (externalPort != -1) {
                        destroyGatewayForwardingRule(externalPort);
                        externalPort = -1;
                    }
                    localServerConnectionsState = State.LocalServerConnectionsState.OPEN;
                    return false;
                case OPEN:
                    // we must close our server and kick all connected clients
                    closePeerConnectionsServer();
                    return false;
            }
        }
        return true;
    }

    private void finishWaitForConnectionRetry() {
        if (localServerConnectionsState == State.LocalServerConnectionsState.WAITING_FOR_OPENING_TRY) {
            localServerConnectionsState = State.LocalServerConnectionsState.CLOSED;
        } else if (localServerConnectionsState == State.LocalServerConnectionsState.WAITING_FOR_NAT_RULE_TRY) {
            localServerConnectionsState = State.LocalServerConnectionsState.OPEN;
        }
        updateState();
    }


    private void openPeerConnectionsServer() {
        try {
            connectionEvents.tryingToOpenLocalServer(wishedConnectionInformation.getLocalPort(), State.LocalServerConnectionsState.OPENING);
            listeningPort = wishedConnectionInformation.getLocalPort();
            serverModule = new ServerModule(listeningPort, new PeerClientServerActionImpl(friendConnectionManager, connectionEvents), PeerClientConnectionManager.generateConcurrentChannelSets());
            serverModule.startListeningConnections();
            localServerConnectionsState = State.LocalServerConnectionsState.OPEN;
            connectionEvents.localServerOpen(getActualListeningPort(), localServerConnectionsState);
        } catch (IOException e) {
            // the server could not be opened
            connectionEvents.couldNotOpenLocalServer(wishedConnectionInformation.getLocalPort(), State.LocalServerConnectionsState.WAITING_FOR_OPENING_TRY);
            localServerConnectionsState = State.LocalServerConnectionsState.CLOSED;
            retryReminder.mustRetryConnection();
        }
    }

    private int createGatewayForwardingRule(int defaultExternalPort) {
        try {
            connectionEvents.tryingToCreateNATRule(defaultExternalPort, getActualListeningPort(), State.LocalServerConnectionsState.CREATING_NAT_RULE);
            GatewayDevice gatewayDevice = UpnpAPI.fetchGatewayDevice(networkTopologyManager.getExternalAddress());
            int externalPort = UpnpAPI.mapPortFrom(gatewayDevice, generateNATRuleDescription(), defaultExternalPort, getActualListeningPort(), true);
            localServerConnectionsState = State.LocalServerConnectionsState.LISTENING;
            connectionEvents.NATRuleCreated(externalPort, getActualListeningPort(), localServerConnectionsState);
            return externalPort;
        } catch (UpnpAPI.NoGatewayException e) {
            connectionEvents.couldNotFetchUPNPGateway(externalPort, getActualListeningPort(), localServerConnectionsState);
            retryReminder.mustRetryConnection();
            return -1;
        } catch (UpnpAPI.UpnpException e) {
            connectionEvents.errorCreatingNATRule(externalPort, getActualListeningPort(), localServerConnectionsState);
            retryReminder.mustRetryConnection();
            return -1;
        }
    }

    private void destroyGatewayForwardingRule(int externalPort) {
        connectionEvents.tryingToDestroyNATRule(externalPort, getActualListeningPort(), State.LocalServerConnectionsState.DESTROYING_NAT_RULE);
        try {
            GatewayDevice gatewayDevice = UpnpAPI.fetchGatewayDevice(networkTopologyManager.getExternalAddress());
            UpnpAPI.unmapPort(gatewayDevice, externalPort);
            connectionEvents.NATRuleDestroyed(externalPort, getActualListeningPort(), localServerConnectionsState);
        } catch (UpnpAPI.NoGatewayException | UpnpAPI.UpnpException e) {
            connectionEvents.couldNotDestroyNATRule(externalPort, getActualListeningPort(), localServerConnectionsState);
        }
    }

    private String generateNATRuleDescription() {
        // use the latest characters from the peerID value
        return NAT_RULE_DESCRIPTION_INIT + ownPeerID.toString().substring(ownPeerID.toString().length() - NAT_RULE_CHARACTER_COUNT);
    }

    private void closePeerConnectionsServer() {
        if (serverModule != null) {
            connectionEvents.tryingToCloseLocalServer(getActualListeningPort(), State.LocalServerConnectionsState.CLOSING);
            serverModule.stopListeningConnections();
        }
        localServerConnectionsState = State.LocalServerConnectionsState.CLOSED;
        connectionEvents.localServerClosed(listeningPort, localServerConnectionsState);
    }
}
