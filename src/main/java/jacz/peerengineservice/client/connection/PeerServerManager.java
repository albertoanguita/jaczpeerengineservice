package jacz.peerengineservice.client.connection;

import jacz.peerengineservice.PeerID;
import jacz.util.concurrency.daemon.Daemon;
import jacz.util.concurrency.daemon.DaemonAction;
import jacz.util.concurrency.timer.SimpleTimerAction;
import jacz.util.concurrency.timer.Timer;

import java.io.IOException;

/**
 * Manager class for handling connection to server
 */
public class PeerServerManager implements DaemonAction {

    /**
     * This class ensures that the connection with the server (if any) is fine (pings the server and checks server timeout)
     */
    private static class ServerConnectionMaintainer implements SimpleTimerAction {

        private final PeerServerManager peerServerManager;

        /**
         * Timer for periodically pinging the server so the connection does not die
         */
        private Timer pingServerTimer;

        private ServerConnectionMaintainer(PeerServerManager peerServerManager) {
            this.peerServerManager = peerServerManager;
            pingServerTimer = null;
        }

        @Override
        public Long wakeUp(Timer timer) {
            if (peerServerManager.refreshConnection()) {
                // we are still connected -> keep refreshing
                return null;
            } else {
                // we lost connection -> stop refreshing
                return 0L;
            }
        }

        public void connectionToServerEstablished(long minReminderTime, long maxReminderTime) {
            stop();
            pingServerTimer = new Timer((minReminderTime + maxReminderTime) / 2, this, "ServerConnectionMaintainer/PingServerTimer");
        }

        public void disconnectedFromServer() {
            stop();
        }

        public void stop() {
            if (pingServerTimer != null) {
                pingServerTimer.kill();
            }
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

    private static final class ActualConnectionData {
        String localAddress;
        String externalAddress;
        int localPort;
        int externalPort;

        public ActualConnectionData() {
            localAddress = "";
            externalAddress = "";
            localPort = -1;
            externalPort = -1;
        }
    }



    /**
     * Our own ID. Cannot be modified after construction time
     */
    private PeerID ownPeerID;

    /**
     * The PeerClientConnectionManager that created us
     */
    private PeerClientConnectionManager peerClientConnectionManager;

    /**
     * Actions to invoke upon certain events
     */
    private final ConnectionEventsBridge connectionEvents;

    /**
     * Server session ID when we are connected. Used to refresh the connection to avoid timeout
     */
    private String peerServerSessionID;

    /**
     * Periodically maintains the connection with server
     */
    private final ServerConnectionMaintainer serverConnectionMaintainer;

    /**
     * Collection of all information related to connection to the server (what we are currently using)
     */
    private final ActualConnectionData actualConnectionData;

    private NetworkTopologyManager networkTopologyManager;

    private LocalServerManager localServerManager;

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
            PeerClientConnectionManager peerClientConnectionManager,
            NetworkTopologyManager networkTopologyManager,
            ConnectionEventsBridge connectionEvents) {
        this.ownPeerID = ownPeerID;
        this.peerClientConnectionManager = peerClientConnectionManager;
        this.connectionEvents = connectionEvents;

        peerServerSessionID = "";
        serverConnectionMaintainer = new ServerConnectionMaintainer(this);

//        connectionInformation = new ConnectionInformation();
        actualConnectionData = new ActualConnectionData();
        this.networkTopologyManager = networkTopologyManager;

        connectionToServerStatus = State.ConnectionToServerState.DISCONNECTED;
        wishForConnect = false;
        stateDaemon = new Daemon(this);
        retryConnectionReminder = new RetryConnectionReminder(this);
    }

    public void setLocalServerManager(LocalServerManager localServerManager) {
        this.localServerManager = localServerManager;
    }

    public State.ConnectionToServerState getConnectionToServerStatus() {
        return connectionToServerStatus;
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
            return connectionToServerStatus == State.ConnectionToServerState.CONNECTED && isCorrectConnectionInformation();
        } else {
            return connectionToServerStatus == State.ConnectionToServerState.DISCONNECTED ||
                    connectionToServerStatus == State.ConnectionToServerState.UNREGISTERED;
        }
    }

    private boolean isCorrectConnectionInformation() {
//        return connectionInformation.equals(wishedConnectionInformation);
        return actualConnectionData.localAddress.equals(networkTopologyManager.getLocalAddress()) &&
                actualConnectionData.externalAddress.equals(networkTopologyManager.getExternalAddress()) &&
                actualConnectionData.localPort == localServerManager.getActualListeningPort() &&
                actualConnectionData.externalPort == localServerManager.getExternalListeningPort();
    }

    void stop() {
        serverConnectionMaintainer.stop();
        retryConnectionReminder.stop();
        setWishForConnect(false);
        stateDaemon.blockUntilStateIsSolved();
    }

    synchronized void updateState() {
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
                if (!isCorrectConnectionInformation()) {
                    disconnectFromPeerServer();
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
            if (connectionToServerStatus == State.ConnectionToServerState.CONNECTED) {
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

    private void finishWaitForConnectionRetry() {
        if (connectionToServerStatus == State.ConnectionToServerState.WAITING_FOR_NEXT_CONNECTION_TRY) {
            connectionToServerStatus = State.ConnectionToServerState.DISCONNECTED;
            updateState();
        }
    }

    private void registerWithPeerServer() {
        connectionEvents.tryingToRegisterWithServer(State.ConnectionToServerState.REGISTERING);
        try {
            ServerAPI.RegistrationResponse registrationResponse =
                    ServerAPI.register(new ServerAPI.RegistrationRequest(ownPeerID));
            switch (registrationResponse) {

                case OK:
                    connectionToServerStatus = State.ConnectionToServerState.DISCONNECTED;
                    connectionEvents.registrationSuccessful(connectionToServerStatus);
                    break;
                case ALREADY_REGISTERED:
                    connectionToServerStatus = State.ConnectionToServerState.DISCONNECTED;
                    connectionEvents.alreadyRegistered(connectionToServerStatus);
                    break;
                default:
                    unrecognizedServerMessage();
                    break;
            }
        } catch (IOException | ServerAccessException e) {
            // failed to connect to server or error in the request -> try again later
            connectionToServerStatus = State.ConnectionToServerState.UNREGISTERED;
            connectionEvents.unableToConnectToServer(connectionToServerStatus);
            retryConnectionReminder.mustRetryConnection();
        } catch (IllegalArgumentException e) {
            // response from server not understandable
            unrecognizedServerMessage();
        }
    }

    private void connectToPeerServer() {
        actualConnectionData.localAddress = networkTopologyManager.getLocalAddress();
        actualConnectionData.externalAddress = networkTopologyManager.getExternalAddress();
        actualConnectionData.localPort = localServerManager.getActualListeningPort();
        actualConnectionData.externalPort = localServerManager.getExternalListeningPort();
        connectionEvents.tryingToConnectToServer(State.ConnectionToServerState.CONNECTING);
        try {
            ServerAPI.ConnectionResponse connectionResponse =
                    ServerAPI.connect(
                            new ServerAPI.ConnectionRequest(
                                    ownPeerID,
                                    actualConnectionData.localAddress,
                                    actualConnectionData.externalAddress,
                                    actualConnectionData.localPort,
                                    actualConnectionData.externalPort
                            )
                    );
            switch (connectionResponse.getResponse()) {

                case OK:
                    connectionToServerStatus = State.ConnectionToServerState.CONNECTED;
                    peerServerSessionID = connectionResponse.getSessionID();
                    serverConnectionMaintainer.connectionToServerEstablished(connectionResponse.getMinReminderTime(), connectionResponse.getMaxReminderTime());
                    connectionEvents.connectionToServerEstablished(connectionToServerStatus);
                    break;
                case PUBLIC_IP_MISMATCH:
                    connectionToServerStatus = State.ConnectionToServerState.DISCONNECTED;
                    peerClientConnectionManager.publicIPMismatch(connectionToServerStatus);
                    break;
                case UNREGISTERED_PEER:
                    connectionToServerStatus = State.ConnectionToServerState.UNREGISTERED;
                    connectionEvents.registrationRequired(connectionToServerStatus);
                    break;
                case PEER_MAIN_SERVER_UNREACHABLE:
                    connectionEvents.localServerUnreachable(connectionToServerStatus);
                    break;
                case PEER_REST_SERVER_UNREACHABLE:
                    // ignore
                    break;
                case WRONG_AUTHENTICATION:
                    // ignore
                    // todo
                    break;
                default:
                    unrecognizedServerMessage();
                    break;
            }
        } catch (IOException | ServerAccessException e) {
            // failed to connect to server or error in the request -> try again later
            connectionToServerStatus = State.ConnectionToServerState.WAITING_FOR_NEXT_CONNECTION_TRY;
            connectionEvents.unableToConnectToServer(connectionToServerStatus);
            retryConnectionReminder.mustRetryConnection();
        } catch (IllegalArgumentException e) {
            // response from server not understandable
            unrecognizedServerMessage();
        }
    }

    private void disconnectFromPeerServer() {
        try {
            ServerAPI.disconnect(new ServerAPI.UpdateRequest(peerServerSessionID));
        } catch (Exception e) {
            // ignore
        } finally {
            // either if we succeed or fail, we consider that we disconnected ok (if not, we will eventually timeout)
            connectionToServerStatus = State.ConnectionToServerState.DISCONNECTED;
            serverConnectionMaintainer.disconnectedFromServer();
            connectionEvents.disconnectedFromServer(connectionToServerStatus);
        }
    }

    private synchronized boolean refreshConnection() {
        if (connectionToServerStatus == State.ConnectionToServerState.CONNECTED) {
            try {
                ServerAPI.RefreshResponse refreshResponse =
                        ServerAPI.refresh(new ServerAPI.UpdateRequest(peerServerSessionID));
                switch (refreshResponse) {

                    case OK:
                        return true;

                    case PUBLIC_IP_MISMATCH:
                        connectionToServerStatus = State.ConnectionToServerState.DISCONNECTED;
                        peerClientConnectionManager.publicIPMismatch(connectionToServerStatus);
                        return false;

                    case UNRECOGNIZED_SESSION:
                    case TOO_SOON:
                        // refresh did not succeed, we are now disconnected
                        refreshFailed();
                        return false;

                    default:
                        unrecognizedServerMessage();
                        return false;
                }
            } catch (ServerAccessException | IOException e) {
                // refresh did not succeed, we are now disconnected
                refreshFailed();
                return false;
            } catch (IllegalArgumentException e) {
                unrecognizedServerMessage();
                return false;
            }
        } else {
            return false;
        }
    }

    private void unrecognizedServerMessage() {
        peerClientConnectionManager.unrecognizedServerMessage();
    }

    private void refreshFailed() {
        connectionToServerStatus = State.ConnectionToServerState.DISCONNECTED;
        connectionEvents.failedToRefreshServerConnection(connectionToServerStatus);
        updateState();
    }
}
