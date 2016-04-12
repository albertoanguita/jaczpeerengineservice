package jacz.peerengineservice.client.connection;

import jacz.util.io.serialization.localstorage.LocalStorage;

import java.io.IOException;

/**
 * Read-only network configuration given by the client
 */
public class NetworkConfiguration {

    /**
     * Port for listening to incoming connections from other peers. 0 means that the API chooses a local port
     * (recommended)
     */
    private static final String LOCAL_PORT = "localPort";

    private static final String EXTERNAL_PORT = "externalPort";

    private final LocalStorage localStorage;

    public NetworkConfiguration(String localStoragePath, int localPort, int externalPort) throws IOException {
        localStorage = LocalStorage.createNew(localStoragePath);
        localStorage.setInteger(LOCAL_PORT, localPort);
        localStorage.setInteger(EXTERNAL_PORT, externalPort);
    }

    public NetworkConfiguration(String localStoragePath) throws IOException {
        localStorage = new LocalStorage(localStoragePath);
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
}
