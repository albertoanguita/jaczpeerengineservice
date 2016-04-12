package jacz.peerengineservice.client.connection;

import jacz.util.network.IP4Port;
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

    private State.NetworkTopologyState networkTopologyState;

    private State.ConnectionToServerState connectionToServerState;

    private State.LocalServerConnectionsState localServerConnectionsState;

    /**
     * Port at which we listen to connections from other peers, if the local server is open (-1 otherwise)
     */
    private int localPort;

    /**
     * Port at which we listen to connections from other peers, if the local server is open (-1 otherwise)
     */
    private int externalPort;

    public ConnectionEventsBridge(ConnectionEvents connectionEvents, PeerClientConnectionManager peerClientConnectionManager) {
        this.connectionEvents = connectionEvents;
        this.peerClientConnectionManager = peerClientConnectionManager;
        sequentialTaskExecutor = Executors.newSingleThreadExecutor();
    }

    private synchronized State buildState() {
        return new State(networkTopologyState, connectionToServerState, localServerConnectionsState, localPort, externalPort);
    }

    public State getState() {
        return buildState();
    }

    private synchronized void updateNetworkTopologyState(State.NetworkTopologyState networkTopologyState) {
        this.networkTopologyState = networkTopologyState;
    }

    private synchronized void updateConnectionToServerInfo(State.ConnectionToServerState connectionToServerStatus) {
        this.connectionToServerState = connectionToServerStatus;
    }

    private synchronized void updateLocalServerInfo(State.LocalServerConnectionsState localServerConnectionsState, int localPort, int externalPort) {
        this.localServerConnectionsState = localServerConnectionsState;
        this.localPort = localPort;
        this.externalPort = externalPort;
    }

    public void listeningPortModified(final int port) {
        logger.info("LISTENING PORT MODIFIED. Port: " + port);
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.listeningPortModified(port);
            }
        });
    }

    public void initializingConnection() {
        logger.info("INITIALIZING CONNECTION");
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.initializingConnection();
            }
        });
    }

    public void localAddressFetched(final String localAddress, final State.NetworkTopologyState networkTopologyState) {
        updateNetworkTopologyState(networkTopologyState);
        logger.info("LOCAL ADDRESS FETCHED. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.localAddressFetched(localAddress, buildState());
            }
        });
    }

    public void couldNotFetchLocalAddress(final State.NetworkTopologyState networkTopologyState) {
        updateNetworkTopologyState(networkTopologyState);
        logger.info("COULD NOT FETCH LOCAL ADDRESS. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.couldNotFetchLocalAddress(buildState());
            }
        });
    }

    public void tryingToFetchExternalAddress(final State.NetworkTopologyState networkTopologyState) {
        updateNetworkTopologyState(networkTopologyState);
        logger.info("TRYING TO FETCH EXTERNAL ADDRESS. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.tryingToFetchExternalAddress(buildState());
            }
        });
    }


    public void externalAddressFetched(final String externalAddress, final boolean hasGateway, final State.NetworkTopologyState networkTopologyState) {
        updateNetworkTopologyState(networkTopologyState);
        logger.info("EXTERNAL ADDRESS FETCHED. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.externalAddressFetched(externalAddress, hasGateway, buildState());
            }
        });
    }

    public void couldNotFetchExternalAddress(final State.NetworkTopologyState networkTopologyState) {
        updateNetworkTopologyState(networkTopologyState);
        logger.info("COULD NOT FETCH EXTERNAL ADDRESS. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.couldNotFetchExternalAddress(buildState());
            }
        });
    }

    public void unrecognizedMessageFromServer(final State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("UNRECOGNIZED MESSAGE FROM SERVER. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.unrecognizedMessageFromServer(buildState());
            }
        });
    }

    public void tryingToConnectToServer(final State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("TRYING TO CONNECT TO SERVER. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.tryingToConnectToServer(buildState());
            }
        });
    }

    public void connectionToServerEstablished(final State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("CONNECTION TO SERVER ESTABLISHED. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.connectionToServerEstablished(buildState());
            }
        });
    }

    public void registrationRequired(final State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("REGISTRATION WITH SERVER REQUIRED. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.registrationRequired(buildState());
            }
        });
    }

    public void localServerUnreachable(final State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("LOCAL SERVER UNREACHABLE. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.localServerUnreachable(buildState());
            }
        });
    }

    public void unableToConnectToServer(final State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("UNABLE TO CONNECT TO SERVER. Server: \" + peerServerData + \". State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.unableToConnectToServer(buildState());
            }
        });
    }

    public void disconnectedFromServer(final State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("DISCONNECTED FROM SERVER. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.disconnectedFromServer(buildState());
            }
        });
    }

    public void failedToRefreshServerConnection(final State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("FAILED TO REFRESH SERVER CONNECTION. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.failedToRefreshServerConnection(buildState());
            }
        });
    }

    public void tryingToRegisterWithServer(final State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("TRYING TO REGISTER WITH SERVER. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.tryingToRegisterWithServer(buildState());
            }
        });
    }

    public void registrationSuccessful(final State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("REGISTRATION SUCCESSFUL. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.registrationSuccessful(buildState());
            }
        });
    }

    public void alreadyRegistered(final State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("ALREADY REGISTERED. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.alreadyRegistered(buildState());
            }
        });
    }

    public void tryingToOpenLocalServer(final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, -1);
        logger.info("TRYING TO OPEN LOCAL SERVER. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.tryingToOpenLocalServer(buildState());
            }
        });
    }

    public void localServerOpen(final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, -1);
        logger.info("LOCAL SERVER OPEN. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.localServerOpen(buildState());
            }
        });
    }

    public void couldNotOpenLocalServer(final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, -1);
        logger.info("COULD NOT OPEN LOCAL SERVER. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.couldNotOpenLocalServer(buildState());
            }
        });
    }

    public void tryingToCloseLocalServer(final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("TRYING TO CLOSE LOCAL SERVER. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.tryingToCloseLocalServer(buildState());
            }
        });
    }

    public void localServerClosed(final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("LOCAL SERVER CLOSED. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.localServerClosed(buildState());
            }
        });
    }

    public void tryingToCreateNATRule(final int externalPort, final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("TRYING TO CREATE NAT RULE. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.tryingToCreateNATRule(buildState());
            }
        });
    }

    public void NATRuleCreated(final int externalPort, final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("NAT RULE CREATED. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.NATRuleCreated(buildState());
            }
        });
    }

    public void couldNotFetchUPNPGateway(final int externalPort, final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("COULD NOT FETCH UPNP GATEWAY. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.couldNotFetchUPNPGateway(buildState());
            }
        });
    }

    public void errorCreatingNATRule(final int externalPort, final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("ERROR CREATING NAT RULE. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.errorCreatingNATRule(buildState());
            }
        });
    }

    public void tryingToDestroyNATRule(final int externalPort, final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("TRYING TO DESTROY NAT RULE. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.tryingToDestroyNATRule(buildState());
            }
        });
    }

    public void NATRuleDestroyed(final int externalPort, final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("NAT RULE DESTROYED. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.NATRuleDestroyed(buildState());
            }
        });
    }

    public void couldNotDestroyNATRule(final int externalPort, final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("COULD NOT DESTROY NAT RULE. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.couldNotDestroyNATRule(buildState());
            }
        });
    }

    public void listeningConnectionsWithoutNATRule(final int externalPort, final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("LISTENING CONNECTIONS WITHOUT NAT RULE. State: " + buildState());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.listeningConnectionsWithoutNATRule(buildState());
            }
        });
    }

    public void peerCouldNotConnectToUs(final Exception e, final IP4Port ip4Port) {
        logger.info("PEER COULD NOT CONNECT TO US. Exception: " + e.getMessage() + ". ip4Port: " + ip4Port);
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.peerCouldNotConnectToUs(e, ip4Port);
            }
        });
    }

    public void localServerError(final Exception e) {
        logger.info("LOCAL SERVER ERROR. Exception: " + e.getMessage());
        peerClientConnectionManager.setWishForConnection(false);
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                connectionEvents.localServerError(e);
            }
        });
    }

    public void stop() {
        sequentialTaskExecutor.shutdown();
    }
}
