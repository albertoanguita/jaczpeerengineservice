package jacz.peerengineservice.client.connection;

import jacz.peerengineservice.client.PeerServerData;

/**
 * This class stores the PeerEngine connection status, formed by several different attributes. We send objects of
 * this class within periodic notifications to the client
 */
public class State {

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
        OPEN,
        CLOSED
    }

    private final ConnectionToServerState connectionToServerState;

    private final LocalServerConnectionsState localServerConnectionsState;

    /**
     * Port at which we listen to connections from other peers, if the local server is open (-1 otherwise)
     */
    private final int port;

    public State() {
        connectionToServerState = ConnectionToServerState.DISCONNECTED;
        localServerConnectionsState = LocalServerConnectionsState.CLOSED;
        port = -1;
    }

    public State(ConnectionToServerState connectionToServerState, LocalServerConnectionsState localServerConnectionsState, int port) {
        this.connectionToServerState = connectionToServerState;
        this.localServerConnectionsState = localServerConnectionsState;
        this.port = port;
    }

    public ConnectionToServerState getConnectionToServerState() {
        return connectionToServerState;
    }

    public LocalServerConnectionsState getLocalServerConnectionsState() {
        return localServerConnectionsState;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "State{Peer server: " + connectionToServerState + ", Local server: " + localServerConnectionsState + " [" + port + "]}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        State state = (State) o;

        if (port != state.port) return false;
        if (connectionToServerState != state.connectionToServerState) return false;
        if (localServerConnectionsState != state.localServerConnectionsState) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = connectionToServerState.hashCode();
        result = 31 * result + localServerConnectionsState.hashCode();
        result = 31 * result + port;
        return result;
    }
}
