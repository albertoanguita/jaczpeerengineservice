package jacz.peerengineservice.client.connection;

/**
 * Information fields related to connection to server and peers. Thread safe
 */
class ConnectionInformation {

    private int localPort;

    ConnectionInformation() {
        this.localPort = -1;
    }


    synchronized int getLocalPort() {
        return localPort;
    }

    /**
     *
     * @param localPort 0 for random port
     * @return true if the port has changed
     */
    synchronized boolean setListeningPort(int localPort) {
        if (this.localPort != localPort) {
            this.localPort = localPort;
            return true;
        } else {
            return false;
        }
    }
}
