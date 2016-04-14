package jacz.peerengineservice.client.connection;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.clientserver.server.ServerAction;
import jacz.commengine.clientserver.server.ServerModule;
import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.connection.peers.PeerConnectionManager;
import jacz.util.AI.evolve.EvolvingState;
import jacz.util.AI.evolve.EvolvingStateController;
import jacz.util.network.IP4Port;
import jacz.util.numeric.NumericUtil;
import org.bitlet.weupnp.GatewayDevice;

import java.io.IOException;

/**
 * This class implements an evolving state in charge of opening and maintaining the local server that allows other
 * peers to connect with us. It also ensures that the server is accessible from the world, creating NAT rules if needed.
 * <p/>
 * The code receives a NetworkConfiguration containing the user choice for the local port and the external port.
 * This is how this class handles these two attributes:
 * - localPort: if zero, then a random local port will be assigned by the underlying server implementation. The actual
 * chosen port will then be figured out, and will be available to the client for checking
 * - externalPort: this value is used as a "default" value for the actual external port. However, it is often not used
 * by this code. It is only used when:
 * -- there is a router in the network, and the previous value of local port was set to zero (random). In this case
 * the external port behaves like a default external port for creating the NAT rule (the actual NAT controller will
 * decide the final port value)
 * -- there is a router in the network, and the local port was a fixed value. In this case, the code assumes that the
 * user has previously set a NAT rule manually (this is not checked). The given external port will be notified to other
 * codes requiring its value as the actual external port
 * <p/>
 * If there is no router in the network, the external port value is ignored (the local port will be reported as
 * external port to outside services)
 * <p/>
 * Upon changes in any value of the network configuration, the code disconnects the server and restarts the
 * connection process. The PeerClientConnectionManager is responsible for reporting these changes (we do not detect
 * them)
 */
public class LocalServerManager {

    /**
     * Used by the ServerModule of the PeerClientConnectionManager, to listen to new connections from other peers. This
     * class also handles every event related to used ChannelConnectionPoints of peers currently connected to use
     * (received messages, freed channels, etc). Received messages are ignored since every communication with other
     * peers is carried out through FSMs, not through direct messaging. The freeing of a channel does have to be
     * notified to the PeerClient.
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

//    private final int defaultExternalPort;

    private int actualExternalPort;

    /**
     * Indicates if a gateway NAT rule has been manually created. Used for destroying NAT rule upon closing
     */
    private boolean gatewayRuleCreated;

    private final PeerClientConnectionManager peerClientConnectionManager;

    private final NetworkConfiguration networkConfiguration;

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
//    private ConnectionInformation wishedConnectionInformation;

    /**
     * Collection of all information related to connection to the server (what we are currently using)
     */
    private int listeningPort;

    private final EvolvingState<State.LocalServerConnectionsState, Boolean> dynamicState;

    public LocalServerManager(
            PeerId ownPeerId,
            PeerClientConnectionManager peerClientConnectionManager,
            NetworkConfiguration networkConfiguration,
            final ConnectionEventsBridge connectionEvents) {
        this.ownPeerId = ownPeerId;
//        this.defaultExternalPort = defaultExternalPort;
        actualExternalPort = -1;
        gatewayRuleCreated = false;
        this.peerClientConnectionManager = peerClientConnectionManager;
        this.networkConfiguration = networkConfiguration;
        this.connectionEvents = connectionEvents;
        serverModule = null;
//        this.wishedConnectionInformation = wishedConnectionInformation;
        listeningPort = -1;
        dynamicState = new EvolvingState<>(State.LocalServerConnectionsState.CLOSED, false, new EvolvingState.Transitions<State.LocalServerConnectionsState, Boolean>() {
            @Override
            public boolean runTransition(State.LocalServerConnectionsState state, Boolean goal, EvolvingStateController<State.LocalServerConnectionsState, Boolean> controller) {
                if (!goal) {
                    switch (state) {
                        case LISTENING:
                            tryToDestroyNatRule(controller);
                            return false;

                        case OPEN:
                            // we must close our server and kick all connected clients
                            tryToCloseServer(controller);
                            return false;
                    }
                } else {
                    switch (state) {
                        case CLOSED:
                        case WAITING_FOR_OPENING_TRY:
                            // open the server for listening connections from other peers
                            try {
                                connectionEvents.tryingToOpenLocalServer(networkConfiguration.getLocalPort(), State.LocalServerConnectionsState.OPENING);
                                openPeerConnectionsServer();
                                controller.setState(State.LocalServerConnectionsState.OPEN);
                                connectionEvents.localServerOpen(getActualLocalPort(), State.LocalServerConnectionsState.OPEN);
                                return false;
                            } catch (IOException e) {
                                // the server could not be opened
                                connectionEvents.couldNotOpenLocalServer(networkConfiguration.getLocalPort(), State.LocalServerConnectionsState.WAITING_FOR_OPENING_TRY);
                                controller.setState(State.LocalServerConnectionsState.WAITING_FOR_OPENING_TRY);
                                return true;
                            }

                        case OPEN:
                        case WAITING_FOR_NAT_RULE_TRY:
                            // check gateway
                            if (LocalServerManager.this.peerClientConnectionManager.getNetworkTopologyManager().hasGateway()) {
                                // there is a gateway in the network. Both local port and external port are used
                                if (networkConfiguration.getLocalPort() == 0) {
                                    // local port is random -> we must create a NAT rule in the gateway
                                    try {
                                        connectionEvents.tryingToCreateNATRule(networkConfiguration.getExternalPort(), getActualLocalPort(), State.LocalServerConnectionsState.CREATING_NAT_RULE);
                                        actualExternalPort = createGatewayForwardingRule(networkConfiguration.getExternalPort());
                                        controller.setState(State.LocalServerConnectionsState.LISTENING);
                                        connectionEvents.NATRuleCreated(actualExternalPort, getActualLocalPort(), State.LocalServerConnectionsState.LISTENING);
                                        gatewayRuleCreated = true;
                                    } catch (UpnpAPI.NoGatewayException e) {
                                        controller.setState(State.LocalServerConnectionsState.WAITING_FOR_NAT_RULE_TRY);
                                        connectionEvents.couldNotFetchUPNPGateway(actualExternalPort, getActualLocalPort(), State.LocalServerConnectionsState.WAITING_FOR_NAT_RULE_TRY);
                                        actualExternalPort = -1;
                                    } catch (UpnpAPI.UpnpException e) {
                                        controller.setState(State.LocalServerConnectionsState.WAITING_FOR_NAT_RULE_TRY);
                                        connectionEvents.errorCreatingNATRule(actualExternalPort, getActualLocalPort(), State.LocalServerConnectionsState.WAITING_FOR_NAT_RULE_TRY);
                                        actualExternalPort = -1;
                                    }


                                    if (actualExternalPort != -1) {
                                        // the nat rule was created ok, evolve
                                        gatewayRuleCreated = true;
                                        return false;
                                    }
                                    // if not, we will retry shortly
                                } else {
                                    // we assume that there is a correct NAT rule that routes traffic from the
                                    // gateway's external port to the machine local port
                                    // we can move to LISTENING state
                                    actualExternalPort = networkConfiguration.getExternalPort();
                                    controller.setState(State.LocalServerConnectionsState.LISTENING);
                                    connectionEvents.listeningConnectionsWithoutNATRule(actualExternalPort, listeningPort, State.LocalServerConnectionsState.LISTENING);
                                    return false;
                                }

                            } else {
                                // no gateway -> external port takes value from local port (rely on user to
                                // appropriately forward ports)
                                // we can move to LISTENING state
                                actualExternalPort = networkConfiguration.getLocalPort();
                                controller.setState(State.LocalServerConnectionsState.LISTENING);
                                connectionEvents.listeningConnectionsWithoutNATRule(actualExternalPort, listeningPort, State.LocalServerConnectionsState.LISTENING);
                                return false;
                            }
                            break;

                        case LISTENING:
                            // check the correct listening port is being used
                            if (!isCorrectConnectionInformation()) {
                                tryToDestroyNatRule(controller);
                                tryToCloseServer(controller);
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

            /////////////////////// auxiliary methods

            private void tryToDestroyNatRule(EvolvingStateController<State.LocalServerConnectionsState, Boolean> controller) {
                if (gatewayRuleCreated) {
                    // we must close our server and kick all connected clients
                    connectionEvents.tryingToDestroyNATRule(actualExternalPort, getActualLocalPort(), State.LocalServerConnectionsState.DESTROYING_NAT_RULE);
                    try {
                        destroyGatewayForwardingRule(actualExternalPort);
                        connectionEvents.NATRuleDestroyed(actualExternalPort, getActualLocalPort(), State.LocalServerConnectionsState.OPEN);
                    } catch (Exception e) {
                        connectionEvents.couldNotDestroyNATRule(actualExternalPort, getActualLocalPort(), State.LocalServerConnectionsState.OPEN);
                    } finally {
                        gatewayRuleCreated = false;
                    }
                }
                controller.setState(State.LocalServerConnectionsState.OPEN);
            }

            private void tryToCloseServer(EvolvingStateController<State.LocalServerConnectionsState, Boolean> controller) {
                if (serverModule != null) {
                    connectionEvents.tryingToCloseLocalServer(getActualLocalPort(), State.LocalServerConnectionsState.CLOSING);
                    closePeerConnectionsServer();
                }
                controller.setState(State.LocalServerConnectionsState.CLOSED);
                connectionEvents.localServerClosed(listeningPort, State.LocalServerConnectionsState.CLOSED);
            }
        }, "LocalServerManager");
        dynamicState.setEvolveStateTimer(State.LocalServerConnectionsState.WAITING_FOR_OPENING_TRY, RETRY_CONNECTION_DELAY);
        dynamicState.setEvolveStateTimer(State.LocalServerConnectionsState.WAITING_FOR_NAT_RULE_TRY, RETRY_CONNECTION_DELAY);
        dynamicState.setEvolveStateTimer(state -> true, GENERAL_REMINDER);
    }


    /**
     * When the local server is open, it returns the port at which this server listens for new connections
     *
     * @return the local listening port
     */
    synchronized Integer getActualLocalPort() {
        return serverModule != null ? serverModule.getActualListeningPort() : null;
    }

    synchronized Integer getActualExternalPort() {
        return dynamicState.state() == State.LocalServerConnectionsState.LISTENING ? actualExternalPort : null;
    }

    void setWishForConnect(boolean wishForConnect) {
        dynamicState.setGoal(wishForConnect, true);
    }

    boolean isInWishedState() {
        return dynamicState.hasReachedGoal();
    }

    private boolean isCorrectConnectionInformation() {
        return listeningPort == networkConfiguration.getLocalPort();
    }

    void stop() {
        setWishForConnect(false);
        dynamicState.blockUntilGoalReached(500);
        dynamicState.stop();
    }

    synchronized void updateState() {
        dynamicState.evolve();
    }


    private void openPeerConnectionsServer() throws IOException {
        listeningPort = networkConfiguration.getLocalPort();
        serverModule = new ServerModule(listeningPort, new PeerClientServerActionImpl(peerClientConnectionManager.getPeerConnectionManager(), connectionEvents), PeerClientConnectionManager.generateConcurrentChannelSets());
        serverModule.startListeningConnections();
    }

    private int createGatewayForwardingRule(int defaultExternalPort) throws UpnpAPI.NoGatewayException, UpnpAPI.UpnpException {
        GatewayDevice gatewayDevice = UpnpAPI.fetchGatewayDevice(peerClientConnectionManager.getNetworkTopologyManager().getExternalAddress());
        return UpnpAPI.mapPortFrom(gatewayDevice, generateNATRuleDescription(), defaultExternalPort, getActualLocalPort(), true);
    }

    private void destroyGatewayForwardingRule(int externalPort) throws UpnpAPI.NoGatewayException, UpnpAPI.UpnpException {
        GatewayDevice gatewayDevice = UpnpAPI.fetchGatewayDevice(peerClientConnectionManager.getNetworkTopologyManager().getExternalAddress());
        UpnpAPI.unmapPort(gatewayDevice, externalPort);
    }

    private String generateNATRuleDescription() {
        // use the latest characters from the peerID value
        return NAT_RULE_DESCRIPTION_INIT + ownPeerId.toString().substring(ownPeerId.toString().length() - NAT_RULE_CHARACTER_COUNT);
    }

    private void closePeerConnectionsServer() {
        serverModule.stopListeningConnections();
    }
}
