package jacz.peerengineservice.client.connection;

import jacz.peerengineservice.client.PeerServerData;

/**
 * This class stores the PeerEngine connection status, formed by several different attributes
 */
public class State {

    /**
     * This enum allows to keep track of the connection state with the peer server
     */
    public static enum ConnectionToServerState {
        DISCONNECTED,
        ONGOING_CONNECTION,
        CONNECTED,
        WAITING_FOR_NEXT_CONNECTION_TRY
    }

    /**
     * This enum stores the possible states of the server for listening to incoming connections from friends
     */
    public static enum LocalServerConnectionsState {
        OPEN,
        CLOSED
    }

    private final ConnectionToServerState connectionToServerState;

    /**
     * Information about the server to which we are connected or trying to connect (null if disconnected or waiting for next connection try)
     */
    private final PeerServerData peerServerData;

    private final LocalServerConnectionsState localServerConnectionsState;

    /**
     * Port at which we listen to connections from other peers, if the local server is open (-1 otherwise)
     */
    private final int port;

    public State() {
        connectionToServerState = ConnectionToServerState.DISCONNECTED;
        peerServerData = null;
        localServerConnectionsState = LocalServerConnectionsState.CLOSED;
        port = -1;
    }

    public State(ConnectionToServerState connectionToServerState, PeerServerData peerServerData, LocalServerConnectionsState localServerConnectionsState, int port) {
        this.connectionToServerState = connectionToServerState;
        this.peerServerData = peerServerData;
        this.localServerConnectionsState = localServerConnectionsState;
        this.port = port;
    }

    public ConnectionToServerState getConnectionToServerState() {
        return connectionToServerState;
    }

    public PeerServerData getPeerServerData() {
        return peerServerData;
    }

    public LocalServerConnectionsState getLocalServerConnectionsState() {
        return localServerConnectionsState;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "State{Peer server: " + connectionToServerState + " [" + peerServerData + "], Local server: " + localServerConnectionsState + " [" + port + "]}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        State state = (State) o;

        if (port != state.port) return false;
        if (connectionToServerState != state.connectionToServerState) return false;
        if (localServerConnectionsState != state.localServerConnectionsState) return false;
        if (peerServerData != null ? !peerServerData.equals(state.peerServerData) : state.peerServerData != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = connectionToServerState.hashCode();
        result = 31 * result + (peerServerData != null ? peerServerData.hashCode() : 0);
        result = 31 * result + localServerConnectionsState.hashCode();
        result = 31 * result + port;
        return result;
    }
}
