package jacz.peerengineservice.client.connection;

/**
 * This class stores the PeerEngine connection status, formed by several different attributes. We send objects of
 * this class within periodic notifications to the client
 */
public class State {

    /**
     * State of detection of the local network topology
     */
    public enum NetworkTopologyState {
        NO_DATA,
        WAITING_FOR_NEXT_LOCAL_ADDRESS_FETCH,
        LOCAL_ADDRESS_FETCHED,
        ALL_FETCHED
    }

    /**
     * This enum allows to keep track of the connection state with the peer server
     */
    public enum ConnectionToServerState {
        UNREGISTERED,
        REGISTERING,
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        WAITING_FOR_NEXT_CONNECTION_TRY
    }

    /**
     * This enum stores the possible states of the server for listening to incoming connections from friends
     */
    public enum LocalServerConnectionsState {
        CLOSED,
        OPENING,
        WAITING_FOR_OPENING_TRY,
        OPEN,
        CREATING_NAT_RULE,
        WAITING_FOR_NAT_RULE_TRY,
        LISTENING,
        DESTROYING_NAT_RULE,
        CLOSING
    }

    private final NetworkTopologyState networkTopologyState;

    private final ConnectionToServerState connectionToServerState;

    private final LocalServerConnectionsState localServerConnectionsState;

    /**
     * Internal port at which we listen to connections from other peers, if the local server is open (-1 otherwise)
     */
    private final int localPort;

    private final int externalPort;

    public State(NetworkTopologyState networkTopologyState, ConnectionToServerState connectionToServerState, LocalServerConnectionsState localServerConnectionsState, int localPort, int externalPort) {
        this.networkTopologyState = networkTopologyState;
        this.connectionToServerState = connectionToServerState;
        this.localServerConnectionsState = localServerConnectionsState;
        this.localPort = localPort;
        this.externalPort = externalPort;
    }

    public ConnectionToServerState getConnectionToServerState() {
        return connectionToServerState;
    }

    public LocalServerConnectionsState getLocalServerConnectionsState() {
        return localServerConnectionsState;
    }

    public int getLocalPort() {
        return localPort;
    }

    public int getExternalPort() {
        return externalPort;
    }

    @Override
    public String toString() {
        return "State{" +
                "networkTopologyState=" + networkTopologyState +
                ", connectionToServerState=" + connectionToServerState +
                ", localServerConnectionsState=" + localServerConnectionsState +
                ", internalPort=" + localPort +
                ", externalPort=" + externalPort +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        State state = (State) o;

        if (localPort != state.localPort) return false;
        if (externalPort != state.externalPort) return false;
        if (connectionToServerState != state.connectionToServerState) return false;
        if (localServerConnectionsState != state.localServerConnectionsState) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = connectionToServerState.hashCode();
        result = 31 * result + localServerConnectionsState.hashCode();
        result = 31 * result + localPort;
        result = 31 * result + externalPort;
        return result;
    }
}
