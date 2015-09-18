package jacz.peerengineservice.client.connection;

import jacz.peerengineservice.client.PeerServerData;

import java.net.InetAddress;

/**
 * Information fields related to connection to server and peers. Thread safe
 */
class ConnectionInformation {

    private InetAddress localInetAddress;

    private PeerServerData peerServerData;

    private int listeningPort;

    ConnectionInformation() {
        clear();
    }

    ConnectionInformation(InetAddress localInetAddress, PeerServerData peerServerData, int listeningPort) {
        this.localInetAddress = localInetAddress;
        this.peerServerData = new PeerServerData(peerServerData);
        this.listeningPort = listeningPort;
    }

    synchronized void clear() {
        setLocalInetAddress(null);
        setPeerServerData(null);
        setListeningPort(-1);
    }

    synchronized InetAddress getLocalInetAddress() {
        return localInetAddress;
    }

    synchronized void setLocalInetAddress(InetAddress localInetAddress) {
        this.localInetAddress = localInetAddress;
    }

    synchronized PeerServerData getPeerServerData() {
        return peerServerData;
    }

    synchronized void setPeerServerData(PeerServerData peerServerData) {
        this.peerServerData = peerServerData;
    }

    synchronized int getListeningPort() {
        return listeningPort;
    }

    synchronized boolean setListeningPort(int listeningPort) {
        if (this.listeningPort != listeningPort) {
            this.listeningPort = listeningPort;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public synchronized boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConnectionInformation that = (ConnectionInformation) o;

        if (listeningPort != that.listeningPort) return false;
        if (!localInetAddress.equals(that.localInetAddress)) return false;
        //noinspection RedundantIfStatement
        if (!peerServerData.equals(that.peerServerData)) return false;

        return true;
    }

    @Override
    public synchronized int hashCode() {
        int result = localInetAddress.hashCode();
        result = 31 * result + peerServerData.hashCode();
        result = 31 * result + listeningPort;
        return result;
    }
}
