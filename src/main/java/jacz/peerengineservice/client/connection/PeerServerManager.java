package jacz.peerengineservice.client.connection;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClientPrivateInterface;
import jacz.peerengineservice.server.ServerAPI;
import jacz.peerengineservice.server.ServerAccessException;
import jacz.util.concurrency.ThreadUtil;
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




    /**
     * Our own ID. Cannot be modified after construction time
     */
    private PeerID ownPeerID;

    /**
     * The PeerClientConnectionManager that created us
     */
    private PeerClientConnectionManager peerClientConnectionManager;

    /**
     * Actions to invoke by the PeerClientConnectionManager in order to communicate with the PeerClient which owns us
     */
    private final PeerClientPrivateInterface peerClientPrivateInterface;

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
            PeerClientConnectionManager peerClientConnectionManager,
            PeerClientPrivateInterface peerClientPrivateInterface,
            ConnectionInformation wishedConnectionInformation) {
        this.ownPeerID = ownPeerID;
        this.peerClientConnectionManager = peerClientConnectionManager;
        this.peerClientPrivateInterface = peerClientPrivateInterface;

        peerServerSessionID = "";
        serverConnectionMaintainer = new ServerConnectionMaintainer(this);

        connectionInformation = new ConnectionInformation();
        this.wishedConnectionInformation = wishedConnectionInformation;

        connectionToServerStatus = State.ConnectionToServerState.DISCONNECTED;
        wishForConnect = false;
        stateDaemon = new Daemon(this);
        retryConnectionReminder = new RetryConnectionReminder(this);
    }

    public State.ConnectionToServerState getConnectionToServerStatus() {
        return connectionToServerStatus;
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
            return connectionToServerStatus == State.ConnectionToServerState.DISCONNECTED ||
                    connectionToServerStatus == State.ConnectionToServerState.UNREGISTERED;
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
            updatedState();
        }
    }

    private void registerWithPeerServer() {
        peerClientPrivateInterface.tryingToRegisterWithServer(connectionToServerStatus);
        try {
            ServerAPI.RegistrationResponse registrationResponse =
                    ServerAPI.register(new ServerAPI.RegistrationRequest(ownPeerID));
            switch (registrationResponse) {

                case OK:
                    connectionToServerStatus = State.ConnectionToServerState.DISCONNECTED;
                    peerClientPrivateInterface.registrationSuccessful(connectionToServerStatus);
                    break;
                case ALREADY_REGISTERED:
                    connectionToServerStatus = State.ConnectionToServerState.DISCONNECTED;
                    peerClientPrivateInterface.alreadyRegistered(connectionToServerStatus);
                    break;
                default:
                    unrecognizedServerMessage();
                    break;
            }
        } catch (IOException | ServerAccessException e) {
            // failed to connect to server or error in the request -> try again later
            connectionToServerStatus = State.ConnectionToServerState.UNREGISTERED;
            peerClientPrivateInterface.unableToConnectToServer(connectionToServerStatus);
            retryConnectionReminder.mustRetryConnection();
        } catch (IllegalArgumentException e) {
            // response from server not understandable
            unrecognizedServerMessage();
        }
    }

    private void connectToPeerServer() {
        connectionInformation.setLocalInetAddress(wishedConnectionInformation.getLocalInetAddress());
        connectionInformation.setListeningPort(wishedConnectionInformation.getListeningPort());
        peerClientPrivateInterface.tryingToConnectToServer(connectionToServerStatus);
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
                    connectionToServerStatus = State.ConnectionToServerState.UNREGISTERED;
                    peerClientPrivateInterface.registrationRequired(connectionToServerStatus);
                    break;
                case PEER_MAIN_SERVER_UNREACHABLE:
                    peerClientPrivateInterface.localServerUnreachable(connectionToServerStatus);
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
            peerClientPrivateInterface.unableToConnectToServer(connectionToServerStatus);
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
            peerClientPrivateInterface.disconnectedFromServer(connectionToServerStatus);
        }
    }

    synchronized boolean refreshConnection() {
        if (connectionToServerStatus == State.ConnectionToServerState.CONNECTED) {
            try {
                ServerAPI.RefreshResponse refreshResponse =
                        ServerAPI.refresh(new ServerAPI.UpdateRequest(peerServerSessionID));
                switch (refreshResponse) {

                    case OK:
                        return true;
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
        peerClientPrivateInterface.failedToRefreshServerConnection(connectionToServerStatus);
        updatedState();
    }
}
