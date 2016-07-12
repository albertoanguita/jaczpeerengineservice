package jacz.peerengineservice.client.connection;

import org.aanguita.jacuzzi.network.IP4Port;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bridge for connection events
 */
public class ConnectionEventsBridge {

    final static Logger logger = LoggerFactory.getLogger(ConnectionEvents.class);

    private final ConnectionEvents connectionEvents;

    private final PeerClientConnectionManager peerClientConnectionManager;

    private final ExecutorService sequentialTaskExecutor;

    private ConnectionState.NetworkTopologyState networkTopologyState;

    private ConnectionState.ConnectionToServerState connectionToServerState;

    private ConnectionState.LocalServerConnectionsState localServerConnectionsState;

    /**
     * Port at which we listen to connections from other peers, if the local server is open (-1 otherwise)
     */
    private int localPort;

    /**
     * Port at which we listen to connections from other peers, if the local server is open (-1 otherwise)
     */
    private int externalPort;

    private String localAddress;

    private String externalAddress;

    private boolean hasGateway;

    public ConnectionEventsBridge(ConnectionEvents connectionEvents, PeerClientConnectionManager peerClientConnectionManager) {
        this.connectionEvents = connectionEvents;
        this.peerClientConnectionManager = peerClientConnectionManager;
        sequentialTaskExecutor = Executors.newSingleThreadExecutor();
        networkTopologyState = ConnectionState.NetworkTopologyState.init();
        connectionToServerState = ConnectionState.ConnectionToServerState.init();
        localServerConnectionsState = ConnectionState.LocalServerConnectionsState.init();
        localPort = 0;
        externalPort = 0;
        localAddress = "";
        externalAddress = "";
        hasGateway = false;
    }

    private ConnectionState buildState() {
        return new ConnectionState(peerClientConnectionManager.isWishForConnection(), networkTopologyState, connectionToServerState, localServerConnectionsState, localPort, externalPort, localAddress, externalAddress, hasGateway);
    }

    public ConnectionState getState() {
        return buildState();
    }

    private synchronized void updateNetworkTopologyState(ConnectionState.NetworkTopologyState networkTopologyState) {
        this.networkTopologyState = networkTopologyState;
    }

    private synchronized void updateConnectionToServerInfo(ConnectionState.ConnectionToServerState connectionToServerStatus) {
        this.connectionToServerState = connectionToServerStatus;
    }

    private synchronized void updateLocalServerInfo(ConnectionState.LocalServerConnectionsState localServerConnectionsState, int localPort, int externalPort) {
        this.localServerConnectionsState = localServerConnectionsState;
        this.localPort = localPort;
        this.externalPort = externalPort;
    }

    public void localPortModified(final int port) {
        logger.info("LOCAL PORT MODIFIED. Port: " + port);
        this.localPort = port;
        sequentialTaskExecutor.submit(() -> connectionEvents.localPortModified(buildState()));
    }

    public void externalPortModified(final int port) {
        logger.info("EXTERNAL PORT MODIFIED. Port: " + port);
        this.externalPort = port;
        sequentialTaskExecutor.submit(() -> connectionEvents.externalPortModified(buildState()));
    }

    public void initializingConnection(ConnectionState.NetworkTopologyState networkTopologyState) {
        updateNetworkTopologyState(networkTopologyState);
        logger.info("INITIALIZING CONNECTION. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.initializingConnection(buildState()));
    }

    public void localAddressFetched(final String localAddress, final ConnectionState.NetworkTopologyState networkTopologyState) {
        updateNetworkTopologyState(networkTopologyState);
        this.localAddress = localAddress;
        logger.info("LOCAL ADDRESS FETCHED. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.localAddressFetched(buildState()));
    }

    public void couldNotFetchLocalAddress(final ConnectionState.NetworkTopologyState networkTopologyState) {
        updateNetworkTopologyState(networkTopologyState);
        logger.info("COULD NOT FETCH LOCAL ADDRESS. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.couldNotFetchLocalAddress(buildState()));
    }

    public void tryingToFetchExternalAddress(final ConnectionState.NetworkTopologyState networkTopologyState) {
        updateNetworkTopologyState(networkTopologyState);
        logger.info("TRYING TO FETCH EXTERNAL ADDRESS. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.tryingToFetchExternalAddress(buildState()));
    }


    public void externalAddressFetched(final String externalAddress, final boolean hasGateway, final ConnectionState.NetworkTopologyState networkTopologyState) {
        updateNetworkTopologyState(networkTopologyState);
        this.externalAddress = externalAddress;
        this.hasGateway = hasGateway;
        logger.info("EXTERNAL ADDRESS FETCHED. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.externalAddressFetched(buildState()));
    }

    public void couldNotFetchExternalAddress(final ConnectionState.NetworkTopologyState networkTopologyState) {
        updateNetworkTopologyState(networkTopologyState);
        logger.info("COULD NOT FETCH EXTERNAL ADDRESS. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.couldNotFetchExternalAddress(buildState()));
    }

    public void unrecognizedMessageFromServer(final ConnectionState.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("UNRECOGNIZED MESSAGE FROM SERVER. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.unrecognizedMessageFromServer(buildState()));
    }

    public void tryingToConnectToServer(final ConnectionState.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("TRYING TO CONNECT TO SERVER. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.tryingToConnectToServer(buildState()));
    }

    public void connectionToServerEstablished(final ConnectionState.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("CONNECTION TO SERVER ESTABLISHED. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.connectionToServerEstablished(buildState()));
    }

    public void registrationRequired(final ConnectionState.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("REGISTRATION WITH SERVER REQUIRED. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.registrationRequired(buildState()));
    }

    public void localServerUnreachable(final ConnectionState.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("LOCAL SERVER UNREACHABLE. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.localServerUnreachable(buildState()));
    }

    public void unableToConnectToServer(final ConnectionState.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("UNABLE TO CONNECT TO SERVER. Server: \" + peerServerData + \". ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.unableToConnectToServer(buildState()));
    }

    public void disconnectedFromServer(final ConnectionState.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("DISCONNECTED FROM SERVER. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.disconnectedFromServer(buildState()));
    }

    public void failedToRefreshServerConnection(final ConnectionState.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("FAILED TO REFRESH SERVER CONNECTION. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.failedToRefreshServerConnection(buildState()));
    }

    public void tryingToRegisterWithServer(final ConnectionState.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("TRYING TO REGISTER WITH SERVER. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.tryingToRegisterWithServer(buildState()));
    }

    public void registrationSuccessful(final ConnectionState.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("REGISTRATION SUCCESSFUL. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.registrationSuccessful(buildState()));
    }

    public void alreadyRegistered(final ConnectionState.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("ALREADY REGISTERED. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.alreadyRegistered(buildState()));
    }

    public void tryingToOpenLocalServer(final int localPort, final ConnectionState.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, -1);
        logger.info("TRYING TO OPEN LOCAL SERVER. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.tryingToOpenLocalServer(buildState()));
    }

    public void localServerOpen(final int localPort, final ConnectionState.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, -1);
        logger.info("LOCAL SERVER OPEN. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.localServerOpen(buildState()));
    }

    public void couldNotOpenLocalServer(final int localPort, final ConnectionState.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, -1);
        logger.info("COULD NOT OPEN LOCAL SERVER. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.couldNotOpenLocalServer(buildState()));
    }

    public void tryingToCloseLocalServer(final int localPort, final ConnectionState.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("TRYING TO CLOSE LOCAL SERVER. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.tryingToCloseLocalServer(buildState()));
    }

    public void localServerClosed(final int localPort, final ConnectionState.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("LOCAL SERVER CLOSED. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.localServerClosed(buildState()));
    }

    public void tryingToCreateNATRule(final int externalPort, final int localPort, final ConnectionState.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("TRYING TO CREATE NAT RULE. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.tryingToCreateNATRule(buildState()));
    }

    public void NATRuleCreated(final int externalPort, final int localPort, final ConnectionState.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("NAT RULE CREATED. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.NATRuleCreated(buildState()));
    }

    public void couldNotFetchUPNPGateway(final int externalPort, final int localPort, final ConnectionState.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("COULD NOT FETCH UPNP GATEWAY. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.couldNotFetchUPNPGateway(buildState()));
    }

    public void errorCreatingNATRule(final int externalPort, final int localPort, final ConnectionState.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("ERROR CREATING NAT RULE. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.errorCreatingNATRule(buildState()));
    }

    public void tryingToDestroyNATRule(final int externalPort, final int localPort, final ConnectionState.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("TRYING TO DESTROY NAT RULE. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.tryingToDestroyNATRule(buildState()));
    }

    public void NATRuleDestroyed(final int externalPort, final int localPort, final ConnectionState.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("NAT RULE DESTROYED. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.NATRuleDestroyed(buildState()));
    }

    public void couldNotDestroyNATRule(final int externalPort, final int localPort, final ConnectionState.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("COULD NOT DESTROY NAT RULE. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.couldNotDestroyNATRule(buildState()));
    }

    public void listeningConnectionsWithoutNATRule(final int externalPort, final int localPort, final ConnectionState.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("LISTENING CONNECTIONS WITHOUT NAT RULE. ConnectionState: " + buildState());
        sequentialTaskExecutor.submit(() -> connectionEvents.listeningConnectionsWithoutNATRule(buildState()));
    }

    public void peerCouldNotConnectToUs(final Exception e, final IP4Port ip4Port) {
        logger.info("PEER COULD NOT CONNECT TO US. Exception: " + e.getMessage() + ". ip4Port: " + ip4Port);
        sequentialTaskExecutor.submit(() -> connectionEvents.peerCouldNotConnectToUs(e, ip4Port));
    }

    public void localServerError(final Exception e) {
        // todo include wish for connection in connection state
        logger.info("LOCAL SERVER ERROR. Exception: " + e.getMessage());
        peerClientConnectionManager.setWishForConnection(false);
        sequentialTaskExecutor.submit(() -> connectionEvents.localServerError(buildState(), e));
    }

    public void stop() {
        sequentialTaskExecutor.shutdown();
    }
}
