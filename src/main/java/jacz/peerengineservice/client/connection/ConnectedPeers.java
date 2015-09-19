package jacz.peerengineservice.client.connection;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.util.sets.availableelements.AvailableElementsByte;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class stores the relation of connected peers. Information can be modified and retrieved in a thread-safe manner
 */
public class ConnectedPeers {

    /**
     * Connection-related data we have about a peer to which we are currently connected (connection status, ccp and
     * available channels).
     * <p/>
     * Available channels should be initialized with all available except one, which is reserved for handling new requests
     * (usually channel 0)
     */
    private class PeerConnectionData {

        /**
         * ID of this peer
         */
        private final PeerID peerID;

        /**
         * The connection status of this peer with respect to us (for internal authorization purposes)
         */
        private ConnectionStatus status;

        /**
         * ChannelConnectionPoint used to reach this client
         */
        private final ChannelConnectionPoint ccp;

        /**
         * Available channels of this client
         */
        private final AvailableElementsByte availableChannels;

        public PeerConnectionData(PeerID peerID, ConnectionStatus status, ChannelConnectionPoint ccp, Byte... occupiedChannels) {
            this.peerID = peerID;
            this.status = status;
            this.ccp = ccp;
            availableChannels = new AvailableElementsByte(occupiedChannels);
        }
    }

    /**
     * List of connected peers, with their corresponding connection information (only connected peers are stored here)
     */
    private final Map<PeerID, PeerConnectionData> connectedPeers;

    /**
     * Table for relating ChannelConnectionPoint objects to their corresponding PeerIDs. This table is necessary
     * because some messages received from the connection sub-package (such as freeChannels, disconnection or error)
     * do not carry the PeerID information (since it is unknown at that level), and can only provide the
     * ChannelConnectionPoint object corresponding to a connected peer
     */
    private final Map<ChannelConnectionPoint, PeerID> ccpToPeerID;

    private final Byte[] occupiedChannels;


    public ConnectedPeers(Byte... occupiedChannels) {
        connectedPeers = new HashMap<>();
        ccpToPeerID = new HashMap<>();
        this.occupiedChannels = occupiedChannels;
    }

    public synchronized int connectedPeersCount() {
        return connectedPeers.size();
    }

    /**
     * This method updates the internal attributes for reflecting a new connection to a peer
     *
     * @param peerID ID of the peer to which we just connected
     * @param ccp    ChannelConnectionPoint object of the connected peer
     * @param status the connection status to give to this new peer connection
     */
    public synchronized void setConnectedPeer(PeerID peerID, ChannelConnectionPoint ccp, ConnectionStatus status) {
        // the initially occupied channels are the channel for the RequestDispatcher and the channel for the connection
        // process (the latter will be released shortly)
        PeerConnectionData pcd = new PeerConnectionData(peerID, status, ccp, occupiedChannels);
        connectedPeers.put(peerID, pcd);
        ccpToPeerID.put(ccp, peerID);
    }

    /**
     * This method indicates if we are already connected to a given peer
     *
     * @param peerID the ID of the peer to test
     * @return true if we have an active connection with the given peer, false otherwise
     */
    public synchronized boolean isConnectedPeer(PeerID peerID) {
        return connectedPeers.containsKey(peerID);
    }

    /**
     * This method retrieves the set of connected peers at a given time
     *
     * @return the set of the peerIDs corresponding to the currently connected peers
     */
    public synchronized Set<PeerID> getConnectedPeers() {
        return new HashSet<>(connectedPeers.keySet());
    }

    /**
     * Retrieves the connection status from a connected peer. The connection status indicates if this peer is a friend of us and we are a friend
     * of him (CORRECT), he is not a friend of us but we are a friend of him (UNVALIDATED) or he is our friend but we are waiting for
     * him to make us his friend (WAITING_FOR_REMOTE_VALIDATION)
     *
     * @param peerID peer whose connection status we want to retrieve
     * @return the connection status of the given peer, or null if we are not connected to this peer
     */
    public synchronized ConnectionStatus getPeerConnectionStatus(PeerID peerID) {
        if (isConnectedPeer(peerID)) {
            return connectedPeers.get(peerID).status;
        } else {
            return null;
        }
    }

    /**
     * Retrieves the connection status from a connected peer. The connection status indicates if this peer is a friend of us and we are a friend
     * of him (CORRECT), he is not a friend of us but we are a friend of him (UNVALIDATED) or he is our friend but we are waiting for
     * him to make us his friend (WAITING_FOR_REMOTE_VALIDATION)
     *
     * @param peerID           peer whose connection status we want to retrieve
     * @param connectionStatus new status
     */
    public synchronized void setPeerConnectionStatus(PeerID peerID, ConnectionStatus connectionStatus) {
        if (isConnectedPeer(peerID)) {
            connectedPeers.get(peerID).status = connectionStatus;
        }
    }

    /**
     * This method requests an incoming channel for communicating with a connected peer. Channels are requested, for
     * example, for setting up custom peer FSMs
     *
     * @param peerID the ID of the peer for which we request a free channel
     * @return the number of the free incoming channel, or null if there are currently no free channels available
     */
    public synchronized Byte requestChannel(PeerID peerID) {
        if (isConnectedPeer(peerID)) {
            return connectedPeers.get(peerID).availableChannels.requestElement();
        } else {
            return null;
        }
    }

    /**
     * The PeerClientConnectionManager reports that some channels used in the ChannelConnectionPoint of a connected
     * peer have been freed. The list of available channels is here updated, so these channels can be further employed
     * (channels are freed when FSMs working on them finalize).
     *
     * @param ccp     the ChannelConnectionPoint whose channels are freed
     * @param channel the freed channel
     */
    public synchronized void channelFreed(ChannelConnectionPoint ccp, byte channel) {
        if (ccpToPeerID.containsKey(ccp)) {
            PeerID peerID = ccpToPeerID.get(ccp);
            connectedPeers.get(peerID).availableChannels.freeElement(channel);
        }
    }

    /**
     * Retrieves the channel connection point of a connected peer. This element is not intended for public use, but for resource transfer
     *
     * @param peerID peer whose channel connection point we want to retrieve
     * @return the channel connection point of the given peer, or null if we are not connected to this peer
     */
    public synchronized ChannelConnectionPoint getPeerChannelConnectionPoint(PeerID peerID) {
        if (isConnectedPeer(peerID)) {
            return connectedPeers.get(peerID).ccp;
        } else {
            return null;
        }
    }

    /**
     * This methods erases a connected peer (due to this peer disconnecting from us, or we from him, or due to an error)
     *
     * @param ccp ChannelConnectionPoint associated to the peer who got disconnected
     */
    public synchronized PeerID peerDisconnected(ChannelConnectionPoint ccp) {
        if (ccpToPeerID.containsKey(ccp)) {
            PeerID peerID = ccpToPeerID.get(ccp);
            connectedPeers.remove(peerID);
            ccpToPeerID.remove(ccp);
            return peerID;
        } else {
            return null;
        }
    }

    public synchronized void disconnectPeer(PeerID peerID) {
        if (connectedPeers.containsKey(peerID)) {
            connectedPeers.get(peerID).ccp.disconnect();
        }
    }

    public synchronized void disconnectAllPeers() {
        for (ChannelConnectionPoint ccp : ccpToPeerID.keySet()) {
            ccp.disconnect();
        }
    }
}
