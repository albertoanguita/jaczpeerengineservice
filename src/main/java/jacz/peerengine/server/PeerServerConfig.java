package jacz.peerengine.server;

/**
 * This class contains the configuration options for a peer server. These include the port of the server, and the
 * maximum amount of clients allowed. These values can only be set at construction time, and cannot be modified later.
 */
public class PeerServerConfig {

    private int port;

    private int maxClients;

    public PeerServerConfig(int port, int maxClients) {
        this.port = port;
        this.maxClients = maxClients;
    }

    public int getPort() {
        return port;
    }

    public int getMaxClients() {
        return maxClients;
    }
}
