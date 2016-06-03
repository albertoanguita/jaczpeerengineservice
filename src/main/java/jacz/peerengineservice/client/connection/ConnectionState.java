package jacz.peerengineservice.client.connection;

/**
 * This class stores the PeerEngine connection status, formed by several different attributes. We send objects of
 * this class within periodic notifications to the client
 */
public class ConnectionState {

    /**
     * ConnectionState of detection of the local network topology
     */
    public enum NetworkTopologyState {
        NO_DATA,
        FETCHING_DATA,
        WAITING_FOR_NEXT_LOCAL_ADDRESS_FETCH,
        LOCAL_ADDRESS_FETCHED,
        WAITING_FOR_NEXT_EXTERNAL_ADDRESS_FETCH,
        ALL_FETCHED;

        public static NetworkTopologyState init() {
            return NO_DATA;
        }
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
        WAITING_FOR_NEXT_CONNECTION_TRY;

        public static ConnectionToServerState init() {
            return DISCONNECTED;
        }

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
        CLOSING;

        public static LocalServerConnectionsState init() {
            return CLOSED;
        }
    }

    private final boolean wishForConnection;

    private final NetworkTopologyState networkTopologyState;

    private final LocalServerConnectionsState localServerConnectionsState;

    private final ConnectionToServerState connectionToServerState;

    /**
     * Internal port at which we listen to connections from other peers, if the local server is open (-1 otherwise)
     */
    private final int localPort;

    private final int externalPort;

    private final String localAddress;

    private final String externalAddress;

    private final boolean hasGateway;


    public ConnectionState(boolean wishForConnection, NetworkTopologyState networkTopologyState, ConnectionToServerState connectionToServerState, LocalServerConnectionsState localServerConnectionsState, int localPort, int externalPort, String localAddress, String externalAddress, boolean hasGateway) {
        this.wishForConnection = wishForConnection;
        this.networkTopologyState = networkTopologyState;
        this.localServerConnectionsState = localServerConnectionsState;
        this.connectionToServerState = connectionToServerState;
        this.localPort = localPort;
        this.externalPort = externalPort;
        this.localAddress = localAddress;
        this.externalAddress = externalAddress;
        this.hasGateway = hasGateway;
    }

    public boolean isWishForConnection() {
        return wishForConnection;
    }

    public NetworkTopologyState getNetworkTopologyState() {
        return networkTopologyState;
    }

    public LocalServerConnectionsState getLocalServerConnectionsState() {
        return localServerConnectionsState;
    }

    public ConnectionToServerState getConnectionToServerState() {
        return connectionToServerState;
    }

    public int getLocalPort() {
        return localPort;
    }

    public int getExternalPort() {
        return externalPort;
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public String getExternalAddress() {
        return externalAddress;
    }

    public boolean isHasGateway() {
        return hasGateway;
    }

    @Override
    public String toString() {
        return "ConnectionState{" +
                "wishForConnection=" + wishForConnection +
                ", networkTopologyState=" + networkTopologyState +
                ", localServerConnectionsState=" + localServerConnectionsState +
                ", connectionToServerState=" + connectionToServerState +
                ", internalPort=" + localPort +
                ", externalPort=" + externalPort +
                ", localAddress=" + localAddress +
                ", externalAddress=" + externalAddress +
                ", hasGateway=" + hasGateway +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConnectionState connectionState = (ConnectionState) o;

        if (localPort != connectionState.localPort) return false;
        if (externalPort != connectionState.externalPort) return false;
        if (connectionToServerState != connectionState.connectionToServerState) return false;
        if (localServerConnectionsState != connectionState.localServerConnectionsState) return false;

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
