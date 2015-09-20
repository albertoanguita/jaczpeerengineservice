package jacz.peerengineservice.client;

import jacz.peerengineservice.PeerID;

/**
 * Data about own client and other peers handled by the PeerClient. This data includes:
 * - own id
 * - port for receiving peer connections
 * - known servers and their last known state
 * <p/>
 * The data in this class is initialized by a class external to the PeerClient, and given to the PeerClient in this
 * form. The PeerClient will take care of updating it according the external events (connected friends, modified
 * personal data, etc). This class implements the event submitter interface to report about these modifications.
 * <p/>
 * It was decided to include the peer data of friends in this class, although it was not useful for the PeerClient.
 * It is useful for the client to store it here and retrieve it from here, since he can initially add the last
 * known values. When some connected peer modifies its data, the PeerClient will store here the new values and notify
 * the client as well
 */
public class PeerClientData {

    /**
     * Our own peer ID
     */
    private final PeerID ownPeerID;

    /**
     * Port for listening to incoming connections from other peers
     */
    private final int port;

    private final PeerServerData peerServerData;

    public PeerClientData(
            PeerID ownPeerID,
            int port,
            PeerServerData peerServerData) {
        this.ownPeerID = ownPeerID;
        this.port = port;
        this.peerServerData = peerServerData;
    }

    public PeerID getOwnPeerID() {
        return ownPeerID;
    }

    public synchronized int getPort() {
        return port;
    }

    public PeerServerData getPeerServerData() {
        return peerServerData;
    }
}
