package jacz.peerengineservice.client.connection;

import org.aanguita.jacuzzi.io.serialization.localstorage.Updater;
import org.aanguita.jacuzzi.io.serialization.localstorage.VersionedLocalStorage;

import java.io.IOException;

/**
 * Read-only network configuration given by the client
 */
public class NetworkConfiguration implements Updater {

    private static final String VERSION_0_1_0 = "0.1.0";

    private static final String CURRENT_VERSION = VERSION_0_1_0;

    /**
     * Port for listening to incoming connections from other peers. 0 means that the API chooses a local port
     * (recommended). Otherwise it should be between 1024 adn 65535
     */
    private static final String LOCAL_PORT = "localPort";

    /**
     * External port, for gateway configuration. The system will try to open a NAT rule in this port in the
     * gateway device. Must be always between 1024 and 65535
     *
     * If there is no gateway, it is not used at all
     */
    private static final String EXTERNAL_PORT = "externalPort";

    private final VersionedLocalStorage localStorage;

    public NetworkConfiguration(String localStoragePath, int localPort, int externalPort) throws IOException {
        localStorage = VersionedLocalStorage.createNew(localStoragePath, CURRENT_VERSION);
        setLocalPort(localPort);
        setExternalPort(externalPort);
    }

    public NetworkConfiguration(String localStoragePath) throws IOException {
        localStorage = new VersionedLocalStorage(localStoragePath, this, CURRENT_VERSION);
    }

    public synchronized int getLocalPort() {
        return localStorage.getInteger(LOCAL_PORT);
    }

    public synchronized boolean setLocalPort(int port) {
        return localStorage.setInteger(LOCAL_PORT, port);
    }

    public synchronized int getExternalPort() {
        return localStorage.getInteger(EXTERNAL_PORT);
    }

    public synchronized boolean setExternalPort(int port) {
        return localStorage.setInteger(EXTERNAL_PORT, port);
    }

    @Override
    public String update(VersionedLocalStorage versionedLocalStorage, String storedVersion) {
        // no versions yet, cannot be invoked
        return null;
    }
}
