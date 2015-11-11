package jacz.peerengineservice.client.connection;

/**
 * Information fields related to connection to server and peers. Thread safe
 */
class ConnectionInformation {

    private String localInetAddress;

    private Integer listeningPort;

    ConnectionInformation() {
        this.localInetAddress = null;
        this.listeningPort = null;
    }

//    ConnectionInformation(String localInetAddress, int listeningPort) {
//        this.localInetAddress = localInetAddress;
//        this.listeningPort = listeningPort;
//    }
//
//    synchronized void clear() {
//        setLocalInetAddress(null);
//        setListeningPort(-1);
//    }

    synchronized String getLocalInetAddress() {
        return localInetAddress;
    }

    synchronized void setLocalInetAddress(String localInetAddress) {
        this.localInetAddress = localInetAddress;
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
//        if (!peerServerData.equals(that.peerServerData)) return false;

        return true;
    }

    @Override
    public synchronized int hashCode() {
        int result = localInetAddress.hashCode();
//        result = 31 * result + peerServerData.hashCode();
        result = 31 * result + listeningPort;
        return result;
    }
}
