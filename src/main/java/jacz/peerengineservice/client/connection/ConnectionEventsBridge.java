package jacz.peerengineservice.client.connection;

import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.concurrency.task_executor.SequentialTaskExecutor;
import jacz.util.network.IP4Port;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridge for connection events
 */
public class ConnectionEventsBridge {

    final static Logger logger = LoggerFactory.getLogger(ConnectionEvents.class);

    private final ConnectionEvents connectionEvents;

    private final PeerClientConnectionManager peerClientConnectionManager;

    private final SequentialTaskExecutor sequentialTaskExecutor;

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
        sequentialTaskExecutor = new SequentialTaskExecutor();
    }

    private synchronized State buildState() {
        return new State(networkTopologyState, connectionToServerState, localServerConnectionsState, localPort, externalPort);
    }

    public synchronized State getState() {
        return buildState();
    }

    private void updateNetworkTopologyState(State.NetworkTopologyState networkTopologyState) {
        this.networkTopologyState = networkTopologyState;
    }

    private void updateConnectionToServerInfo(State.ConnectionToServerState connectionToServerStatus) {
        this.connectionToServerState = connectionToServerStatus;
    }

    private void updateLocalServerInfo(State.LocalServerConnectionsState localServerConnectionsState, int localPort, int externalPort) {
        this.localServerConnectionsState = localServerConnectionsState;
        this.localPort = localPort;
        this.externalPort = externalPort;
    }

    public synchronized void listeningPortModified(final int port) {
        logger.info("LISTENING PORT MODIFIED. Port: " + port);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.listeningPortModified(port);
            }
        });
    }

    public synchronized void initializingConnection() {
        logger.info("INITIALIZING CONNECTION");
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.initializingConnection();
            }
        });
    }

    public synchronized void localAddressFetched(final String localAddress, final State.NetworkTopologyState networkTopologyState) {
        updateNetworkTopologyState(networkTopologyState);
        logger.info("LOCAL ADDRESS FETCHED. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.localAddressFetched(localAddress, buildState());
            }
        });
    }

    public synchronized void couldNotFetchLocalAddress(final State.NetworkTopologyState networkTopologyState) {
        updateNetworkTopologyState(networkTopologyState);
        logger.info("COULD NOT FETCH LOCAL ADDRESS. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.couldNotFetchLocalAddress(buildState());
            }
        });
    }

    public synchronized void tryingToFetchExternalAddress(final State.NetworkTopologyState networkTopologyState) {
        updateNetworkTopologyState(networkTopologyState);
        logger.info("TRYING TO FETCH EXTERNAL ADDRESS. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.tryingToFetchExternalAddress(buildState());
            }
        });
    }


    public synchronized void externalAddressFetched(final String externalAddress, final boolean hasGateway, final State.NetworkTopologyState networkTopologyState) {
        updateNetworkTopologyState(networkTopologyState);
        logger.info("EXTERNAL ADDRESS FETCHED. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.externalAddressFetched(externalAddress, hasGateway, buildState());
            }
        });
    }

    public synchronized void couldNotFetchExternalAddress(final State.NetworkTopologyState networkTopologyState) {
        updateNetworkTopologyState(networkTopologyState);
        logger.info("COULD NOT FETCH EXTERNAL ADDRESS. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.couldNotFetchExternalAddress(buildState());
            }
        });
    }

    public synchronized void unrecognizedMessageFromServer(final State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("UNRECOGNIZED MESSAGE FROM SERVER. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.unrecognizedMessageFromServer(buildState());
            }
        });
    }

    public synchronized void tryingToConnectToServer(final State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("TRYING TO CONNECT TO SERVER. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.tryingToConnectToServer(buildState());
            }
        });
    }

    public synchronized void connectionToServerEstablished(final State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("CONNECTION TO SERVER ESTABLISHED. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.connectionToServerEstablished(buildState());
            }
        });
    }

    public synchronized void registrationRequired(final State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("REGISTRATION WITH SERVER REQUIRED. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.registrationRequired(buildState());
            }
        });
    }

    public synchronized void localServerUnreachable(final State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("LOCAL SERVER UNREACHABLE. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.localServerUnreachable(buildState());
            }
        });
    }

    public synchronized void unableToConnectToServer(final State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("UNABLE TO CONNECT TO SERVER. Server: \" + peerServerData + \". State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.unableToConnectToServer(buildState());
            }
        });
    }

    public synchronized void disconnectedFromServer(final State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("DISCONNECTED FROM SERVER. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.disconnectedFromServer(buildState());
            }
        });
    }

    public synchronized void failedToRefreshServerConnection(final State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("FAILED TO REFRESH SERVER CONNECTION. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.failedToRefreshServerConnection(buildState());
            }
        });
    }

    public synchronized void tryingToRegisterWithServer(final State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("TRYING TO REGISTER WITH SERVER. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.tryingToRegisterWithServer(buildState());
            }
        });
    }

    public synchronized void registrationSuccessful(final State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("REGISTRATION SUCCESSFUL. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.registrationSuccessful(buildState());
            }
        });
    }

    public synchronized void alreadyRegistered(final State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        logger.info("ALREADY REGISTERED. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.alreadyRegistered(buildState());
            }
        });
    }

    public synchronized void tryingToOpenLocalServer(final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, -1);
        logger.info("TRYING TO OPEN LOCAL SERVER. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.tryingToOpenLocalServer(buildState());
            }
        });
    }

    public synchronized void localServerOpen(final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, -1);
        logger.info("LOCAL SERVER OPEN. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.localServerOpen(buildState());
            }
        });
    }

    public synchronized void couldNotOpenLocalServer(final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, -1);
        logger.info("COULD NOT OPEN LOCAL SERVER. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.couldNotOpenLocalServer(buildState());
            }
        });
    }

    public synchronized void tryingToCloseLocalServer(final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("TRYING TO CLOSE LOCAL SERVER. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.tryingToCloseLocalServer(buildState());
            }
        });
    }

    public synchronized void localServerClosed(final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("LOCAL SERVER CLOSED. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.localServerClosed(buildState());
            }
        });
    }

    public synchronized void tryingToCreateNATRule(final int externalPort, final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("TRYING TO CREATE NAT RULE. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.tryingToCreateNATRule(buildState());
            }
        });
    }

    public synchronized void NATRuleCreated(final int externalPort, final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("NAT RULE CREATED. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.NATRuleCreated(buildState());
            }
        });
    }

    public synchronized void couldNotFetchUPNPGateway(final int externalPort, final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("COULD NOT FETCH UPNP GATEWAY. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.couldNotFetchUPNPGateway(buildState());
            }
        });
    }

    public synchronized void errorCreatingNATRule(final int externalPort, final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("ERROR CREATING NAT RULE. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.errorCreatingNATRule(buildState());
            }
        });
    }

    public synchronized void tryingToDestroyNATRule(final int externalPort, final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("TRYING TO DESTROY NAT RULE. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.tryingToDestroyNATRule(buildState());
            }
        });
    }

    public synchronized void NATRuleDestroyed(final int externalPort, final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("NAT RULE DESTROYED. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.NATRuleDestroyed(buildState());
            }
        });
    }

    public synchronized void couldNotDestroyNATRule(final int externalPort, final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("COULD NOT DESTROY NAT RULE. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.couldNotDestroyNATRule(buildState());
            }
        });
    }

    public synchronized void listeningConnectionsWithoutNATRule(final int externalPort, final int localPort, final State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, localPort, externalPort);
        logger.info("LISTENING CONNECTIONS WITHOUT NAT RULE. State: " + buildState());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.listeningConnectionsWithoutNATRule(buildState());
            }
        });
    }

    public void peerCouldNotConnectToUs(final Exception e, final IP4Port ip4Port) {
        logger.info("PEER COULD NOT CONNECT TO US. Exception: " + e.getMessage() + ". ip4Port: " + ip4Port);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.peerCouldNotConnectToUs(e, ip4Port);
            }
        });
    }

    public void localServerError(final Exception e) {
        logger.info("LOCAL SERVER ERROR. Exception: " + e.getMessage());
        peerClientConnectionManager.setWishForConnection(false);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                connectionEvents.localServerError(e);
            }
        });
    }

}
