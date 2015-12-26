package jacz.peerengineservice.client.connection;

/**
 * Read-only network configuration given by the client
 */
public class NetworkConfiguration {

    /**
     * Port for listening to incoming connections from other peers. 0 means that the API chooses a local port
     * (recommended)
     */
    private final int localPort;

    private final int externalPort;

    public NetworkConfiguration(
            int localPort,
            int externalPort) {
        this.localPort = localPort;
        this.externalPort = externalPort;
    }

    public int getLocalPort() {
        return localPort;
    }

    public int getExternalPort() {
        return externalPort;
    }
}
