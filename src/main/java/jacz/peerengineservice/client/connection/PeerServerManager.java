package jacz.peerengineservice.client.connection;

import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineservice.PeerId;
import jacz.util.AI.evolve.EvolvingState;
import jacz.util.AI.evolve.EvolvingStateController;

import java.io.IOException;

/**
 * This class defines an evolving state for handling connection with the central server
 */
public class PeerServerManager {

    private static final class ActualConnectionData {
        String localAddress;
        int localPort;
        int externalPort;

        public ActualConnectionData() {
            localAddress = "";
            localPort = -1;
            externalPort = -1;
        }
    }

    private static final long RETRY_CONNECTION_DELAY = 25000L;

    /**
     * Our own ID. Cannot be modified after construction time
     */
    private final PeerId ownPeerId;

    /**
     * URL (including version) of the server
     */
    private final String serverURL;

    /**
     * The PeerClientConnectionManager that created us
     */
    private final PeerClientConnectionManager peerClientConnectionManager;

    /**
     * Actions to invoke upon certain events
     */
    private final ConnectionEventsBridge connectionEvents;

    /**
     * Server session ID when we are connected. Used to refresh the connection to avoid timeout
     */
    private String peerServerSessionID;

    /**
     * Collection of all information related to connection to the server (what we are currently using)
     */
    private final ActualConnectionData actualConnectionData;

    private final EvolvingState<State.ConnectionToServerState, Boolean> dynamicState;

    public PeerServerManager(
            PeerId ownPeerId,
            String serverURL,
            PeerClientConnectionManager peerClientConnectionManager,
            ConnectionEventsBridge connectionEvents) {
        this.ownPeerId = ownPeerId;
        this.serverURL = serverURL;
        this.peerClientConnectionManager = peerClientConnectionManager;
        this.connectionEvents = connectionEvents;

        peerServerSessionID = "";

        actualConnectionData = new ActualConnectionData();

        dynamicState = new EvolvingState<>(State.ConnectionToServerState.DISCONNECTED, false, new EvolvingState.Transitions<State.ConnectionToServerState, Boolean>() {
            @Override
            public boolean runTransition(State.ConnectionToServerState state, Boolean goal, EvolvingStateController<State.ConnectionToServerState, Boolean> controller) {
                if (goal) {
                    switch (state) {

                        case CONNECTED:
                            // check that we are connected to the right peer server and that the local address is the correct one (in any case, disconnect)
                            if (!isCorrectConnectionInformation()) {
                                disconnectFromPeerServer(controller);
                                return false;
                            }
                            break;

                        case UNREGISTERED:
                            registerWithPeerServer(controller);
                            return false;

                        case DISCONNECTED:
                        case WAITING_FOR_NEXT_CONNECTION_TRY:
                            // client wants us to connect to the server
                            return connectToPeerServer(state, controller);
                    }
                } else {
                    // disconnect from the peer server
                    if (state == State.ConnectionToServerState.CONNECTED) {
                        disconnectFromPeerServer(controller);
                        return false;
                    } else if (state == State.ConnectionToServerState.WAITING_FOR_NEXT_CONNECTION_TRY) {
                        // we don't want to connect anymore, stop the RetryConnectionReminder
                        controller.setState(State.ConnectionToServerState.DISCONNECTED);
                        return false;
                    }
                }
                // else, everything ok
                return true;
            }

            @Override
            public boolean hasReachedGoal(State.ConnectionToServerState state, Boolean goal) {
                return goal && state == State.ConnectionToServerState.CONNECTED && isCorrectConnectionInformation()
                        || !goal && (state == State.ConnectionToServerState.DISCONNECTED || state == State.ConnectionToServerState.UNREGISTERED);
            }
        });
        dynamicState.setEvolveStateTimer(State.ConnectionToServerState.UNREGISTERED, RETRY_CONNECTION_DELAY);
        dynamicState.setEvolveStateTimer(State.ConnectionToServerState.DISCONNECTED, RETRY_CONNECTION_DELAY);
        dynamicState.setEvolveStateTimer(State.ConnectionToServerState.WAITING_FOR_NEXT_CONNECTION_TRY, RETRY_CONNECTION_DELAY);
        // we do not set a general timer because we want to set the timer for the connected state for refreshing the connection,
        // and we do not know the millis for that timer at compile time
    }

    public State.ConnectionToServerState getConnectionToServerStatus() {
        return dynamicState.state();
    }

    void setWishForConnect(boolean wishForConnect) {
        dynamicState.setGoal(wishForConnect, true);
    }

    boolean isInWishedState() {
        return dynamicState.hasReachedGoal();
    }

    private boolean isCorrectConnectionInformation() {
        return actualConnectionData.localAddress.equals(peerClientConnectionManager.getNetworkTopologyManager().getLocalAddress()) &&
                actualConnectionData.localPort == peerClientConnectionManager.getLocalServerManager().getActualLocalPort() &&
                actualConnectionData.externalPort == peerClientConnectionManager.getLocalServerManager().getActualExternalPort();
    }

    void stop() {
        setWishForConnect(false);
        dynamicState.blockUntilGoalReached(500);
        dynamicState.stop();
    }

    void updateState() {
        dynamicState.evolve();
    }

    private void registerWithPeerServer(EvolvingStateController<State.ConnectionToServerState, Boolean> controller) {
        connectionEvents.tryingToRegisterWithServer(State.ConnectionToServerState.REGISTERING);
        try {
            ServerAPI.RegistrationResponse registrationResponse =
                    ServerAPI.register(serverURL, new ServerAPI.RegistrationRequest(ownPeerId));
            switch (registrationResponse) {

                case OK:
                    controller.setState(State.ConnectionToServerState.DISCONNECTED);
                    connectionEvents.registrationSuccessful(State.ConnectionToServerState.DISCONNECTED);
                    break;
                case ALREADY_REGISTERED:
                    controller.setState(State.ConnectionToServerState.DISCONNECTED);
                    connectionEvents.alreadyRegistered(State.ConnectionToServerState.DISCONNECTED);
                    break;
                default:
                    unrecognizedServerMessage();
                    break;
            }
        } catch (IOException | ServerAccessException e) {
            // failed to connect to server or error in the request -> try again later
            controller.setState(State.ConnectionToServerState.UNREGISTERED);
            connectionEvents.unableToConnectToServer(State.ConnectionToServerState.UNREGISTERED);
        } catch (IllegalArgumentException e) {
            // response from server not understandable
            unrecognizedServerMessage();
        }
    }

    private boolean connectToPeerServer(State.ConnectionToServerState state, EvolvingStateController<State.ConnectionToServerState, Boolean> controller) {
        actualConnectionData.localAddress = peerClientConnectionManager.getNetworkTopologyManager().getLocalAddress();
        actualConnectionData.localPort = peerClientConnectionManager.getLocalServerManager().getActualLocalPort();
        actualConnectionData.externalPort = peerClientConnectionManager.getLocalServerManager().getActualExternalPort();
        connectionEvents.tryingToConnectToServer(State.ConnectionToServerState.CONNECTING);
        try {
            ServerAPI.ConnectionResponse connectionResponse =
                    ServerAPI.connect(
                            serverURL,
                            new ServerAPI.ConnectionRequest(
                                    ownPeerId,
                                    actualConnectionData.localAddress,
                                    actualConnectionData.localPort,
                                    actualConnectionData.externalPort,
                                    CountryCode.AC, // todo
                                    true
                            )
                    );
            switch (connectionResponse.getResponse()) {

                case OK:
                    controller.setState(State.ConnectionToServerState.CONNECTED);
                    peerServerSessionID = connectionResponse.getSessionID();
                    // set up the timer for refreshing the connection
                    setupConnectionRefreshTimer(connectionResponse.getMinReminderTime(), connectionResponse.getMaxReminderTime());
                    connectionEvents.connectionToServerEstablished(State.ConnectionToServerState.CONNECTED);
                    return false;
                case UNREGISTERED_PEER:
                    controller.setState(State.ConnectionToServerState.UNREGISTERED);
                    connectionEvents.registrationRequired(State.ConnectionToServerState.UNREGISTERED);
                    return false;
                case PEER_MAIN_SERVER_UNREACHABLE:
                    controller.setState(State.ConnectionToServerState.WAITING_FOR_NEXT_CONNECTION_TRY);
                    connectionEvents.localServerUnreachable(state);
                    return true;
                case PEER_REST_SERVER_UNREACHABLE:
                    // ignore
                    controller.setState(State.ConnectionToServerState.WAITING_FOR_NEXT_CONNECTION_TRY);
                    return true;
                case WRONG_AUTHENTICATION:
                    // ignore
                    // todo
                    controller.setState(State.ConnectionToServerState.WAITING_FOR_NEXT_CONNECTION_TRY);
                    return true;
                default:
                    unrecognizedServerMessage();
                    return true;
            }
        } catch (IOException | ServerAccessException e) {
            // failed to connect to server or error in the request -> try again later
            controller.setState(State.ConnectionToServerState.WAITING_FOR_NEXT_CONNECTION_TRY);
            connectionEvents.unableToConnectToServer(State.ConnectionToServerState.WAITING_FOR_NEXT_CONNECTION_TRY);
            return true;
        } catch (IllegalArgumentException e) {
            // response from server not understandable
            unrecognizedServerMessage();
            return true;
        }
    }

    private void setupConnectionRefreshTimer(long minReminderTime, long maxReminderTime) {
        dynamicState.setRunnableStateTimer(State.ConnectionToServerState.CONNECTED, (minReminderTime + maxReminderTime) / 2, new Runnable() {
            @Override
            public void run() {
                refreshConnection();
            }
        });
    }

    private void disconnectFromPeerServer(EvolvingStateController<State.ConnectionToServerState, Boolean> controller) {
        try {
            ServerAPI.disconnect(serverURL, new ServerAPI.UpdateRequest(peerServerSessionID));
        } catch (Exception e) {
            // ignore
        } finally {
            // either if we succeed or fail, we consider that we disconnected ok (if not, we will eventually timeout)
            controller.setState(State.ConnectionToServerState.DISCONNECTED);
            connectionEvents.disconnectedFromServer(State.ConnectionToServerState.DISCONNECTED);
        }
    }

    private synchronized void refreshConnection() {
        if (dynamicState.state() == State.ConnectionToServerState.CONNECTED) {
            try {
                ServerAPI.RefreshResponse refreshResponse =
                        ServerAPI.refresh(serverURL, new ServerAPI.UpdateRequest(peerServerSessionID));
                switch (refreshResponse) {

                    case OK:
                        break;

                    case UNRECOGNIZED_SESSION:
                    case TOO_SOON:
                        // refresh did not succeed, we are now disconnected
                        refreshFailed();
                        break;

                    default:
                        refreshFailed();
                        unrecognizedServerMessage();
                        break;
                }
            } catch (ServerAccessException | IOException e) {
                // refresh did not succeed, we are now disconnected
                refreshFailed();
            } catch (IllegalArgumentException e) {
                unrecognizedServerMessage();
            }
        }
    }

    private void unrecognizedServerMessage() {
        peerClientConnectionManager.unrecognizedServerMessage();
    }

    private void refreshFailed() {
        dynamicState.setState(State.ConnectionToServerState.DISCONNECTED);
        connectionEvents.failedToRefreshServerConnection(State.ConnectionToServerState.DISCONNECTED);
        dynamicState.evolve();
    }
}
