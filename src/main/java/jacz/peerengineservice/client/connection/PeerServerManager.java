package jacz.peerengineservice.client.connection;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClientPrivateInterface;
import jacz.peerengineservice.server.PeerServer;
import jacz.peerengineservice.server.RequestFromPeerToServer;
import jacz.peerengineservice.server.ServerAPI;
import jacz.peerengineservice.server.ServerAccessException;
import jacz.peerengineservice.util.ChannelConstants;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.concurrency.daemon.Daemon;
import jacz.util.concurrency.daemon.DaemonAction;
import jacz.util.concurrency.timer.SimpleTimerAction;
import jacz.util.concurrency.timer.Timer;
import jacz.util.sets.availableelements.AvailableElementsByte;

import java.io.IOException;
import java.util.Set;

/**
 * Manager class for handling connection to server
 */
public class PeerServerManager implements DaemonAction {

    /**
     * This class ensures that the connection with the server (if any) is fine (pings the server and checks server timeout)
     */
    private static class ServerConnectionMaintainer implements SimpleTimerAction {

        private final static long PING_SERVER_TIMER = PeerServer.TIMEOUT_MILLIS * 3 / 4;

        private final static long PING_SERVER_TIMEOUT_TIMER = PeerServer.TIMEOUT_MILLIS;

        private final PeerServerManager peerServerManager;

        /**
         * Timer for periodically pinging the server so the connection does not die
         */
        private Timer pingServerTimer;

        /**
         * Timer for controlling that the connection with the server does not time out. If this timer goes off, we must disconnect from the server
         * due to timeout
         */
        private Timer peerServerTimeoutTimer;


        private ServerConnectionMaintainer(PeerServerManager peerServerManager) {
            this.peerServerManager = peerServerManager;
            pingServerTimer = new Timer(PING_SERVER_TIMER, this, "ServerConnectionMaintainer/PingServerTimer");
            peerServerTimeoutTimer = new Timer(PING_SERVER_TIMEOUT_TIMER, this, "ServerConnectionMaintainer/PeerServerTimeoutTimer");
        }

        @Override
        public Long wakeUp(Timer timer) {
            if (timer == pingServerTimer) {
                peerServerManager.pingServer();
            } else if (timer == peerServerTimeoutTimer) {
                peerServerManager.connectionToServerTimedOut();
            }
            return null;
        }

        public void connectionToServerEstablished() {
            peerServerTimeoutTimer.reset();
        }

        public void pingFromServerReceived() {
            peerServerTimeoutTimer.reset();
        }

        public void stop() {
            pingServerTimer.kill();
            peerServerTimeoutTimer.kill();
        }
    }

    private static class RetryConnectionReminder implements SimpleTimerAction {

        private static final long RETRY_CONNECTION_DELAY = 25000L;

        private final PeerServerManager peerServerManager;

        private final Timer timer;

        private RetryConnectionReminder(PeerServerManager peerServerManager) {
            this.peerServerManager = peerServerManager;
            timer = new Timer(RETRY_CONNECTION_DELAY, this, false, "RetryConnectionReminder");
        }

        synchronized void mustRetryConnection() {
            timer.reset(RETRY_CONNECTION_DELAY);
        }

        synchronized void stop() {
            timer.kill();
        }

        @Override
        public Long wakeUp(Timer timer) {
            peerServerManager.finishWaitForConnectionRetry();
            return 0L;
        }
    }

    /**
     * Time that the connection process to the server can last without timeout (only for connection process)
     */
    private static final long CONNECTION_TO_PEER_SERVER_TIMEOUT = 7000L;

    /**
     * Our own ID. Cannot be modified after construction time
     */
    private PeerID ownPeerID;

    /**
     * Actions to invoke by the PeerClientConnectionManager in order to communicate with the PeerClient which owns us
     */
    private final PeerClientPrivateInterface peerClientPrivateInterface;

    /**
     * ChannelConnectionPoint used to communicate with the peer server
     */
    private ChannelConnectionPoint peerServerCCP;

    private String peerServerSessionID;

    /**
     * Available channels for communication with the peer server
     */
    private final AvailableElementsByte availableChannels;

    /**
     * Periodically maintains the connection with server
     */
    private final ServerConnectionMaintainer serverConnectionMaintainer;

    /**
     * Collection of all information related to connection to the server (what we are currently using)
     */
    private final ConnectionInformation connectionInformation;

    /**
     * Collection of all information related to connection to the server, as provided by the user
     */
    private final ConnectionInformation wishedConnectionInformation;

    /**
     * Status of the connection to the peer server
     */
    private State.ConnectionToServerState connectionToServerStatus;

    private boolean wishForConnect;

    /**
     * Daemon thread that helps keep the state in its desired values
     */
    private final Daemon stateDaemon;

    private final RetryConnectionReminder retryConnectionReminder;


    public PeerServerManager(
            PeerID ownPeerID,
            PeerClientPrivateInterface peerClientPrivateInterface,
            ConnectionInformation wishedConnectionInformation) {
        this.ownPeerID = ownPeerID;
        this.peerClientPrivateInterface = peerClientPrivateInterface;

        peerServerCCP = null;
        peerServerSessionID = null;
        availableChannels = new AvailableElementsByte(ChannelConstants.PEER_SERVER_CONNECTION_CHANNEL, ChannelConstants.REQUESTS_CHANNEL);
        serverConnectionMaintainer = new ServerConnectionMaintainer(this);

        connectionInformation = new ConnectionInformation();
        this.wishedConnectionInformation = wishedConnectionInformation;

        connectionToServerStatus = State.ConnectionToServerState.DISCONNECTED;
        wishForConnect = false;
        stateDaemon = new Daemon(this);
        retryConnectionReminder = new RetryConnectionReminder(this);
    }


    synchronized void setWishForConnect(boolean wishForConnect) {
        boolean mustUpdateState = this.wishForConnect != wishForConnect;
        this.wishForConnect = wishForConnect;
        if (mustUpdateState) {
            updatedState();
        }
    }

    synchronized boolean isInWishedState() {
        if (wishForConnect) {
            return connectionToServerStatus == State.ConnectionToServerState.CONNECTED && isCorrectConnectionInformation();
        } else {
            return connectionToServerStatus == State.ConnectionToServerState.DISCONNECTED;
        }
    }

    private boolean isCorrectConnectionInformation() {
        return connectionInformation.equals(wishedConnectionInformation);
    }

    void stop() {
        serverConnectionMaintainer.stop();
        retryConnectionReminder.stop();
        setWishForConnect(false);
        // actively wait until the server is closed
        boolean mustWait;
        synchronized (this) {
            mustWait = connectionToServerStatus != State.ConnectionToServerState.DISCONNECTED;
        }
        while (mustWait) {
            ThreadUtil.safeSleep(100L);
            synchronized (this) {
                mustWait = connectionToServerStatus != State.ConnectionToServerState.DISCONNECTED;
            }
        }
    }

    synchronized void updatedState() {
        stateDaemon.stateChange();
        stateDaemon.interrupt();
    }

    /**
     * Executes one step towards fulfilling the desired state
     *
     * @return true if the state is now the desired one, false if more steps are required
     */
    @Override
    public synchronized boolean solveState() {
        if (wishForConnect) {
            if (connectionToServerStatus == State.ConnectionToServerState.CONNECTED) {
                // check that we are connected to the right peer server and that the local address is the correct one (in any case, disconnect)
                if (!connectionInformation.equals(wishedConnectionInformation)) {
                    disconnectFromPeerServer();
                    longDelay();
                    return false;
                }
            } else if (connectionToServerStatus == State.ConnectionToServerState.UNREGISTERED) {
                registerWithPeerServer();
                return false;
            } else if (connectionToServerStatus == State.ConnectionToServerState.DISCONNECTED) {
                // client wants us to connect to the server
                connectToPeerServer();
                return false;
            } else if (connectionToServerStatus == State.ConnectionToServerState.WAITING_FOR_NEXT_CONNECTION_TRY) {
                return true;
            }
        } else {
            // disconnect from the peer server
            if (connectionToServerStatus == State.ConnectionToServerState.ONGOING_CONNECTION) {
                // wait until connection process is complete, then we will disconnect. Nevertheless, return false to indicate that we should revisit the
                // state rather soon
                shortDelay();
                return false;
            } else if (connectionToServerStatus == State.ConnectionToServerState.CONNECTED) {
                disconnectFromPeerServer();
                return false;
            } else if (connectionToServerStatus == State.ConnectionToServerState.WAITING_FOR_NEXT_CONNECTION_TRY) {
                // we don't want to connect anymore, stop the RetryConnectionReminder
                retryConnectionReminder.stop();
                connectionToServerStatus = State.ConnectionToServerState.DISCONNECTED;
                return false;
            }
        }
        // all connection status is ok
        return true;
    }

//    @Override
//    public synchronized Long wakeUp(Timer timer) {
//        connectionToServerStatus = State.ConnectionToServerState.DISCONNECTED;
//        updatedState();
//        return 0L;
//    }


    private void shortDelay() {
        ThreadUtil.safeSleep(500);
    }

    private void longDelay() {
        ThreadUtil.safeSleep(1000);
    }

    private void finishWaitForConnectionRetry() {
        if (connectionToServerStatus == State.ConnectionToServerState.WAITING_FOR_NEXT_CONNECTION_TRY) {
            connectionToServerStatus = State.ConnectionToServerState.DISCONNECTED;
            updatedState();
        }
    }

    private void registerWithPeerServer() {
        try {
            ServerAPI.RegistrationResponse registrationResponse =
                    ServerAPI.register(new ServerAPI.RegistrationRequest(ownPeerID));
            switch (registrationResponse) {

                case OK:
                    connectionToServerStatus = State.ConnectionToServerState.DISCONNECTED;
                    // todo notify
                    break;
                case ALREADY_REGISTERED:
                    connectionToServerStatus = State.ConnectionToServerState.DISCONNECTED;
                    // todo notify
                    break;
                default:
                    connectionToServerStatus = State.ConnectionToServerState.UNREGISTERED;
                    // todo notify
                    break;
            }
        } catch (IOException | ServerAccessException e) {
            // failed to connect to server or error in the request -> try again later
            connectionToServerStatus = State.ConnectionToServerState.UNREGISTERED;
            peerClientPrivateInterface.unableToConnectToServer(connectionToServerStatus);
            retryConnectionReminder.mustRetryConnection();
            connectionInformation.clear();
        } catch (IllegalArgumentException e) {
            // response from server not understandable
            connectionToServerStatus = State.ConnectionToServerState.UNREGISTERED;
            // todo notify
            setWishForConnect(false);
        }
    }

    private void connectToPeerServer() {
        connectionInformation.setLocalInetAddress(wishedConnectionInformation.getLocalInetAddress());
//        connectionInformation.setPeerServerData(wishedConnectionInformation.getPeerServerData());
        connectionInformation.setListeningPort(wishedConnectionInformation.getListeningPort());

        try {
            ServerAPI.ConnectionResponse connectionResponse =
                    ServerAPI.connect(
                            new ServerAPI.ConnectionRequest(
                                    ownPeerID,
                                    connectionInformation.getLocalInetAddress().toString(),
                                    connectionInformation.getListeningPort(),
                                    connectionInformation.getListeningPort()
                            )
                    );
            switch (connectionResponse.getResponse()) {

                case OK:
                    connectionToServerStatus = State.ConnectionToServerState.CONNECTED;
                    peerServerSessionID = connectionResponse.getSessionID();
                    serverConnectionMaintainer.connectionToServerEstablished(connectionResponse.getMinReminderTime(), connectionResponse.getMaxReminderTime());
                    peerClientPrivateInterface.connectionToServerEstablished(connectionToServerStatus);
                    break;
                case UNREGISTERED_PEER:
                    // todo notify
                    connectionToServerStatus = State.ConnectionToServerState.UNREGISTERED;
                    break;
                case PEER_MAIN_SERVER_UNREACHABLE:
                    // todo notify client
                    break;
                case PEER_REST_SERVER_UNREACHABLE:
                    // ignore
                    break;
                case WRONG_AUTHENTICATION:
                    // ignore
                    break;
            }
        } catch (IOException | ServerAccessException e) {
            // failed to connect to server or error in the request -> try again later
            connectionToServerStatus = State.ConnectionToServerState.WAITING_FOR_NEXT_CONNECTION_TRY;
            peerClientPrivateInterface.unableToConnectToServer(connectionToServerStatus);
            retryConnectionReminder.mustRetryConnection();
            connectionInformation.clear();
        } catch (IllegalArgumentException e) {
            // response from server not understandable
            connectionToServerStatus = State.ConnectionToServerState.DISCONNECTED;
            // todo notify
            setWishForConnect(false);
        }
//
//
//        String serverIP = connectionInformation.getPeerServerData().getIp4Port().getIp();
//        int serverPort = connectionInformation.getPeerServerData().getIp4Port().getPort();
//        Set<Byte> allChannels = new HashSet<>();
//        for (Byte channel = Byte.MIN_VALUE; channel < Byte.MAX_VALUE; channel++) {
//            allChannels.add(channel);
//        }
//        allChannels.add(Byte.MAX_VALUE);
//        Set<Set<Byte>> concurrentChannels = new HashSet<>();
//        concurrentChannels.add(allChannels);
//
//        ClientModule peerClientModule = new ClientModule(new IP4Port(serverIP, serverPort), new PeerClientConnectionToServerChannelActionImpl(this), concurrentChannels);
//        // with the ClientModule we obtain the peerServerCCP and register the FSM for setting up the connection
//        try {
//            connectionToServerStatus = State.ConnectionToServerState.ONGOING_CONNECTION;
//            peerClientPrivateInterface.tryingToConnectToServer(connectionInformation.getPeerServerData(), connectionToServerStatus);
//            peerServerCCP = peerClientModule.connect();
//            peerServerCCP.registerTimedFSM(new ClientConnectionToServerFSM(this, ownPeerID, connectionInformation.getLocalInetAddress(), connectionInformation.getListeningPort()), CONNECTION_TO_PEER_SERVER_TIMEOUT, "ClientConnectionToServerFSM", ChannelConstants.PEER_SERVER_CONNECTION_CHANNEL);
//            peerServerCCP.registerGenericFSM(new ServerRequestDispatcherFSM(this), "ServerRequestDispatcherFSM", ChannelConstants.REQUESTS_CHANNEL);
//            peerClientModule.start();
//        } catch (IOException e) {
//            // error connecting to the peer server
//            connectionToServerStatus = State.ConnectionToServerState.WAITING_FOR_NEXT_CONNECTION_TRY;
//            peerClientPrivateInterface.unableToConnectToServer(new PeerServerData(connectionInformation.getPeerServerData()), connectionToServerStatus);
//            retryConnectionReminder.mustRetryConnection();
//            connectionInformation.clear();
//        }
    }

    private void disconnectFromPeerServer() {
        try {
            ServerAPI.UpdateResponse updateResponse =
                    ServerAPI.disconnect(new ServerAPI.UpdateRequest(ownPeerID));
            switch (updateResponse) {

                case OK:
                    connectionToServerStatus = State.ConnectionToServerState.DISCONNECTED;
                    // todo notify
                    break;
                case ALREADY_REGISTERED:
                    connectionToServerStatus = State.ConnectionToServerState.DISCONNECTED;
                    // todo notify
                    break;
                default:
                    connectionToServerStatus = State.ConnectionToServerState.UNREGISTERED;
                    // todo notify
                    break;
            }
        } catch (IOException | ServerAccessException e) {
            // failed to connect to server or error in the request -> try again later
            connectionToServerStatus = State.ConnectionToServerState.UNREGISTERED;
            peerClientPrivateInterface.unableToConnectToServer(connectionToServerStatus);
            retryConnectionReminder.mustRetryConnection();
            connectionInformation.clear();
        } catch (IllegalArgumentException e) {
            // response from server not understandable
            connectionToServerStatus = State.ConnectionToServerState.UNREGISTERED;
            // todo notify
            setWishForConnect(false);
        }
//        if (peerServerCCP != null) {
//            peerServerCCP.disconnect();
//        }
//        peerServerCCP = null;
//        if (connectionToServerStatus == State.ConnectionToServerState.CONNECTED) {
//            peerClientPrivateInterface.disconnectedFromServer(true, connectionInformation.getPeerServerData(), State.ConnectionToServerState.DISCONNECTED);
//        }
//        connectionToServerStatus = State.ConnectionToServerState.DISCONNECTED;
    }

    synchronized void connectionToServerEstablished(ChannelConnectionPoint ccp) {
        if (ccp.equals(peerServerCCP)) {
            connectionToServerStatus = State.ConnectionToServerState.CONNECTED;
            serverConnectionMaintainer.connectionToServerEstablished();
            peerClientPrivateInterface.connectionToServerEstablished(connectionInformation.getPeerServerData(), connectionToServerStatus);
            updatedState();
        } else {
            // disconnect this old connection to the server
            ccp.disconnect();
        }
    }

    synchronized void connectionToServerDenied(ClientConnectionToServerFSM.ConnectionFailureReason reason, ChannelConnectionPoint ccp) {
        if (ccp.equals(peerServerCCP)) {
            disconnectFromPeerServer();
            connectionToServerStatus = State.ConnectionToServerState.WAITING_FOR_NEXT_CONNECTION_TRY;
            peerClientPrivateInterface.connectionToServerDenied(connectionInformation.getPeerServerData(), reason, connectionToServerStatus);
            connectionInformation.clear();
            retryConnectionReminder.mustRetryConnection();
            updatedState();
        } else {
            // disconnect this old connection to the server
            ccp.disconnect();
        }
    }

    synchronized void serverTookToMuchTimeToAnswerConnectionRequest(ChannelConnectionPoint ccp) {
        if (ccp.equals(peerServerCCP)) {
            disconnectFromPeerServer();
            connectionToServerStatus = State.ConnectionToServerState.WAITING_FOR_NEXT_CONNECTION_TRY;
            peerClientPrivateInterface.serverTookToMuchTimeToAnswerConnectionRequest(connectionInformation.getPeerServerData(), connectionToServerStatus);
            connectionInformation.clear();
            retryConnectionReminder.mustRetryConnection();
            updatedState();
        } else {
            // disconnect this old connection to the server
            ccp.disconnect();
        }
    }

    synchronized void disconnectedFromServer(boolean expected, ChannelConnectionPoint ccp) {
        if (ccp.equals(peerServerCCP)) {
            connectionToServerStatus = State.ConnectionToServerState.DISCONNECTED;
            peerClientPrivateInterface.disconnectedFromServer(expected, connectionInformation.getPeerServerData(), connectionToServerStatus);
            connectionInformation.clear();
            updatedState();
            if (!expected) {
                updatedState();
            }
        }
    }

    synchronized void connectionToServerTimedOut() {
        disconnectFromPeerServer();
        connectionToServerStatus = State.ConnectionToServerState.WAITING_FOR_NEXT_CONNECTION_TRY;
        peerClientPrivateInterface.connectionToServerTimedOut(connectionInformation.getPeerServerData(), connectionToServerStatus);
        connectionInformation.clear();
        retryConnectionReminder.mustRetryConnection();
        updatedState();
    }

    synchronized void peerServerPingReceived() {
        serverConnectionMaintainer.pingFromServerReceived();
    }

    synchronized void channelFreed(byte channel) {
        availableChannels.freeElement(channel);
    }

    synchronized void pingServer() {
        if (connectionToServerStatus == State.ConnectionToServerState.CONNECTED) {
            peerServerCCP.write(PeerServer.REQUESTS_CHANNEL, RequestFromPeerToServer.pingRequest(ChannelConstants.REQUESTS_CHANNEL));
        }
    }

    synchronized void registerClientFriendSearchFSM(FriendConnectionManager friendConnectionManager, Set<PeerID> disconnectedFriends, long friendSearchTimeout) {
        if (peerServerCCP != null) {
            Byte channel = availableChannels.requestElement();
            if (channel != null) {
                peerServerCCP.registerTimedFSM(new ClientFriendSearchFSM(friendConnectionManager, channel, disconnectedFriends), friendSearchTimeout, "ClientFriendSearchFSM", channel);
            }
        }
    }
}
