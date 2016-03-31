package jacz.peerengineservice.client.connection;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.clientserver.server.ServerAction;
import jacz.commengine.clientserver.server.ServerModule;
import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.connection.peers.PeerConnectionManager;
import jacz.peerengineservice.client.connection.peers.kb.PeerAddress;
import jacz.util.AI.evolve.EvolvingState;
import jacz.util.AI.evolve.EvolvingStateController;
import jacz.util.network.IP4Port;
import jacz.util.numeric.NumericUtil;
import org.bitlet.weupnp.GatewayDevice;

import java.io.IOException;

/**
 * This class implements an evolving state in charge of opening and maintaining the local server that allows other
 * peers to connect with us
 */
public class LocalServerManager {

    /**
     * Used by the ServerModule of the PeerClientConnectionManager, to listen to new connections from other peers. This
     * class also handles every event related to used ChannelConnectionPoints of peers currently connected to use
     * (received messages, freed channels, etc). Received messages are ignored since every communication with other
     * peers is carried out through FSMs, not through direct messaging. The freeing of a channel does have to be notified
     * to the PeerClient.
     */
    private static class PeerClientServerActionImpl implements ServerAction {

        private final PeerConnectionManager peerConnectionManager;

        private final ConnectionEventsBridge connectionEvents;


        /**
         * Class constructor
         *
         * @param peerConnectionManager the peerConnectionManager that handles friends events
         */
        public PeerClientServerActionImpl(
                PeerConnectionManager peerConnectionManager,
                ConnectionEventsBridge connectionEvents) {
            this.peerConnectionManager = peerConnectionManager;
            this.connectionEvents = connectionEvents;
        }

        @Override
        public void newClientConnection(String clientID, ChannelConnectionPoint ccp, IP4Port ip4Port) {
            peerConnectionManager.reportClientConnectedToOurPeerServer(ccp);
        }

        @Override
        public void newMessage(String clientID, ChannelConnectionPoint ccp, byte channel, Object message) {
            // ignore
        }

        @Override
        public void newMessage(String clientID, ChannelConnectionPoint ccp, byte channel, byte[] data) {
            // ignore
        }

        @Override
        public void channelFreed(String clientID, ChannelConnectionPoint ccp, byte channel) {
            peerConnectionManager.channelFreed(ccp, channel);
        }

        @Override
        public void clientDisconnected(String clientID, ChannelConnectionPoint ccp, boolean expected) {
            peerConnectionManager.peerDisconnected(ccp);
        }

        @Override
        public void clientError(String clientID, ChannelConnectionPoint ccp, CommError e) {
            peerConnectionManager.peerError(ccp, e);
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


    private static final String NAT_RULE_DESCRIPTION_INIT = "JCZ_";

    private static final int NAT_RULE_CHARACTER_COUNT = 6;

    private static final long RETRY_CONNECTION_DELAY = 25000L;

    private final static long GENERAL_REMINDER = NumericUtil.max(RETRY_CONNECTION_DELAY) + 5000L;


    private final PeerId ownPeerId;

    private final int defaultExternalPort;

    private int externalPort;

    private final NetworkTopologyManager networkTopologyManager;

    private final PeerClientConnectionManager peerClientConnectionManager;

    /**
     * Actions to invoke upon certain events
     */
    private final ConnectionEventsBridge connectionEvents;

    /**
     * ServerModule object employed to receive connections from friend peers
     */
    private ServerModule serverModule;

    /**
     * Collection of all information related to connection to the server, as provided by the user
     */
    private ConnectionInformation wishedConnectionInformation;

    /**
     * Collection of all information related to connection to the server (what we are currently using)
     */
    private int listeningPort;

    private final EvolvingState<State.LocalServerConnectionsState, Boolean> dynamicState;

    public LocalServerManager(
            PeerId ownPeerId,
            int defaultExternalPort,
            final NetworkTopologyManager networkTopologyManager,
            PeerClientConnectionManager peerClientConnectionManager,
            ConnectionInformation wishedConnectionInformation,
            final ConnectionEventsBridge connectionEvents) {
        this.ownPeerId = ownPeerId;
        this.defaultExternalPort = defaultExternalPort;
        externalPort = -1;
        this.networkTopologyManager = networkTopologyManager;
        this.peerClientConnectionManager = peerClientConnectionManager;
        this.connectionEvents = connectionEvents;
        serverModule = null;
        this.wishedConnectionInformation = wishedConnectionInformation;
        listeningPort = -1;
        dynamicState = new EvolvingState<>(State.LocalServerConnectionsState.CLOSED, false, new EvolvingState.Transitions<State.LocalServerConnectionsState, Boolean>() {
            @Override
            public boolean runTransition(State.LocalServerConnectionsState state, Boolean goal, EvolvingStateController<State.LocalServerConnectionsState, Boolean> controller) {
                if (!goal) {
                    switch (state) {
                        case LISTENING:
                            // we must close our server and kick all connected clients
                            if (externalPort != -1) {
                                destroyGatewayForwardingRule(externalPort, controller);
                                externalPort = -1;
                            }
                            return false;

                        case OPEN:
                            // we must close our server and kick all connected clients
                            closePeerConnectionsServer(controller);
                            return false;
                    }
                } else {
                    switch (state) {
                        case CLOSED:
                        case WAITING_FOR_OPENING_TRY:
                            // open the server for listening connections from other peers
                            return openPeerConnectionsServer(controller);

                        case OPEN:
                        case WAITING_FOR_NAT_RULE_TRY:
                            // check gateway
                            if (listeningPort == 0 && LocalServerManager.this.networkTopologyManager.hasGateway()) {
                                // we must create a NAT rule in the gateway for connection to be able to reach us
                                externalPort = createGatewayForwardingRule(LocalServerManager.this.defaultExternalPort, controller);
                                if (LocalServerManager.this.externalPort != -1) {
                                    // the nat rule was created ok, evolve
                                    return false;
                                }
                                // if not, we will retry shortly
                            } else {
                                // fixed port (rely on user to appropriately forward ports) or no gateway,
                                // we can move to LISTENING state
                                controller.setState(State.LocalServerConnectionsState.LISTENING);
                                externalPort = LocalServerManager.this.wishedConnectionInformation.getLocalPort();
                                connectionEvents.listeningConnectionsWithoutNATRule(externalPort, listeningPort, State.LocalServerConnectionsState.LISTENING);
                                return false;
                            }
                            break;

                        case LISTENING:
                            // check the correct listening port is being used
                            if (!isCorrectConnectionInformation()) {
                                destroyGatewayForwardingRule(externalPort, controller);
                                closePeerConnectionsServer(controller);
                                return false;
                            }
                            // otherwise, everything ok
                            break;
                    }
                }
                // else, everything ok
                return true;
            }

            @Override
            public boolean hasReachedGoal(State.LocalServerConnectionsState state, Boolean goal) {
                return goal && state == State.LocalServerConnectionsState.LISTENING || !goal && state == State.LocalServerConnectionsState.CLOSED;
            }
        });
        dynamicState.setEvolveStateTimer(State.LocalServerConnectionsState.WAITING_FOR_OPENING_TRY, RETRY_CONNECTION_DELAY);
        dynamicState.setEvolveStateTimer(State.LocalServerConnectionsState.WAITING_FOR_NAT_RULE_TRY, RETRY_CONNECTION_DELAY);
        dynamicState.setEvolveStateTimer(state -> true, GENERAL_REMINDER);
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
        return dynamicState.state() == State.LocalServerConnectionsState.LISTENING ? externalPort : null;
    }

    public synchronized PeerAddress getPeerAddress() {
        // todo
        return null;
    }

    void setWishForConnect(boolean wishForConnect) {
        dynamicState.setGoal(wishForConnect, true);
    }

    boolean isInWishedState() {
        return dynamicState.hasReachedGoal();
    }

    private boolean isCorrectConnectionInformation() {
        return listeningPort == wishedConnectionInformation.getLocalPort();
    }

    void stop() {
        setWishForConnect(false);
        dynamicState.blockUntilGoalReached(500);
        dynamicState.stop();
    }

    synchronized void updateState() {
        dynamicState.evolve();
    }


    private boolean openPeerConnectionsServer(EvolvingStateController<State.LocalServerConnectionsState, Boolean> controller) {
        try {
            connectionEvents.tryingToOpenLocalServer(wishedConnectionInformation.getLocalPort(), State.LocalServerConnectionsState.OPENING);
            listeningPort = wishedConnectionInformation.getLocalPort();
            serverModule = new ServerModule(listeningPort, new PeerClientServerActionImpl(peerClientConnectionManager.getPeerConnectionManager(), connectionEvents), PeerClientConnectionManager.generateConcurrentChannelSets());
            serverModule.startListeningConnections();
            controller.setState(State.LocalServerConnectionsState.OPEN);
            connectionEvents.localServerOpen(getActualListeningPort(), State.LocalServerConnectionsState.OPEN);
            return false;
        } catch (IOException e) {
            // the server could not be opened
            connectionEvents.couldNotOpenLocalServer(wishedConnectionInformation.getLocalPort(), State.LocalServerConnectionsState.WAITING_FOR_OPENING_TRY);
            controller.setState(State.LocalServerConnectionsState.WAITING_FOR_OPENING_TRY);
            return true;
        }
    }

    private int createGatewayForwardingRule(int defaultExternalPort, EvolvingStateController<State.LocalServerConnectionsState, Boolean> controller) {
        try {
            connectionEvents.tryingToCreateNATRule(defaultExternalPort, getActualListeningPort(), State.LocalServerConnectionsState.CREATING_NAT_RULE);
            GatewayDevice gatewayDevice = UpnpAPI.fetchGatewayDevice(networkTopologyManager.getExternalAddress());
            int externalPort = UpnpAPI.mapPortFrom(gatewayDevice, generateNATRuleDescription(), defaultExternalPort, getActualListeningPort(), true);
            controller.setState(State.LocalServerConnectionsState.LISTENING);
            connectionEvents.NATRuleCreated(externalPort, getActualListeningPort(), State.LocalServerConnectionsState.LISTENING);
            return externalPort;
        } catch (UpnpAPI.NoGatewayException e) {
            controller.setState(State.LocalServerConnectionsState.WAITING_FOR_NAT_RULE_TRY);
            connectionEvents.couldNotFetchUPNPGateway(externalPort, getActualListeningPort(), State.LocalServerConnectionsState.WAITING_FOR_NAT_RULE_TRY);
            return -1;
        } catch (UpnpAPI.UpnpException e) {
            controller.setState(State.LocalServerConnectionsState.WAITING_FOR_NAT_RULE_TRY);
            connectionEvents.errorCreatingNATRule(externalPort, getActualListeningPort(), State.LocalServerConnectionsState.WAITING_FOR_NAT_RULE_TRY);
            return -1;
        }
    }

    private void destroyGatewayForwardingRule(int externalPort, EvolvingStateController<State.LocalServerConnectionsState, Boolean> controller) {
        connectionEvents.tryingToDestroyNATRule(externalPort, getActualListeningPort(), State.LocalServerConnectionsState.DESTROYING_NAT_RULE);
        try {
            // in both cases (success or error) the resulting state will be OPEN
            controller.setState(State.LocalServerConnectionsState.OPEN);
            GatewayDevice gatewayDevice = UpnpAPI.fetchGatewayDevice(networkTopologyManager.getExternalAddress());
            UpnpAPI.unmapPort(gatewayDevice, externalPort);
            connectionEvents.NATRuleDestroyed(externalPort, getActualListeningPort(), State.LocalServerConnectionsState.OPEN);
        } catch (UpnpAPI.NoGatewayException | UpnpAPI.UpnpException e) {
            connectionEvents.couldNotDestroyNATRule(externalPort, getActualListeningPort(), State.LocalServerConnectionsState.OPEN);
        }
    }

    private String generateNATRuleDescription() {
        // use the latest characters from the peerID value
        return NAT_RULE_DESCRIPTION_INIT + ownPeerId.toString().substring(ownPeerId.toString().length() - NAT_RULE_CHARACTER_COUNT);
    }

    private void closePeerConnectionsServer(EvolvingStateController<State.LocalServerConnectionsState, Boolean> controller) {
        if (serverModule != null) {
            connectionEvents.tryingToCloseLocalServer(getActualListeningPort(), State.LocalServerConnectionsState.CLOSING);
            serverModule.stopListeningConnections();
        }
        controller.setState(State.LocalServerConnectionsState.CLOSED);
        connectionEvents.localServerClosed(listeningPort, State.LocalServerConnectionsState.CLOSED);
    }
}
