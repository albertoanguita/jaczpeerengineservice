package jacz.peerengineservice.client.connection;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.util.event.notification.NotificationEmitter;
import jacz.util.event.notification.NotificationProcessor;
import jacz.util.event.notification.NotificationReceiver;
import jacz.util.sets.availableelements.AvailableElementsByte;

import java.util.*;

/**
 * This class stores the relation of connected peers. Information can be modified and retrieved in a thread-safe manner
 */
public class ConnectedPeers implements NotificationEmitter {

    /**
     * Connection-related data we have about a peer to which we are currently connected (connection status, ccp and
     * available channels).
     * <p/>
     * Available channels should be initialized with all available except one, which is reserved for handling new requests
     * (usually channel 0)
     */
    private class PeerConnectionData {

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

        public PeerConnectionData(ConnectionStatus status, ChannelConnectionPoint ccp, Byte... occupiedChannels) {
            this.status = status;
            this.ccp = ccp;
            availableChannels = new AvailableElementsByte(occupiedChannels);
        }
    }

    /**
     * List of connected peers, with their corresponding connection information (only connected peers are stored here)
     */
    private final Map<PeerId, PeerConnectionData> connectedPeers;

    /**
     * Table for relating ChannelConnectionPoint objects to their corresponding PeerIDs. This table is necessary
     * because some messages received from the connection sub-package (such as freeChannels, disconnection or error)
     * do not carry the PeerId information (since it is unknown at that level), and can only provide the
     * ChannelConnectionPoint object corresponding to a connected peer
     */
    private final Map<ChannelConnectionPoint, PeerId> ccpToPeerID;

    private final Byte[] occupiedChannels;

    /**
     * For submitting events to subscribers (each time a connection of a peer changes)
     */
    private final NotificationProcessor notificationProcessor;


    public ConnectedPeers(Byte... occupiedChannels) {
        connectedPeers = new HashMap<>();
        ccpToPeerID = new HashMap<>();
        this.occupiedChannels = occupiedChannels;
        notificationProcessor = new NotificationProcessor();
    }

    public synchronized int connectedPeersCount() {
        return connectedPeers.size();
    }

    /**
     * This method updates the internal attributes for reflecting a new connection to a peer
     *
     * @param peerId ID of the peer to which we just connected
     * @param ccp    ChannelConnectionPoint object of the connected peer
     * @param status the connection status to give to this new peer connection
     */
    public synchronized void setConnectedPeer(PeerId peerId, ChannelConnectionPoint ccp, ConnectionStatus status) {
        // the initially occupied channels are the channel for the RequestDispatcher and the channel for the connection
        // process (the latter will be released shortly)
        PeerConnectionData pcd = new PeerConnectionData(status, ccp, occupiedChannels);
        connectedPeers.put(peerId, pcd);
        notificationProcessor.newEvent(peerId);
        ccpToPeerID.put(ccp, peerId);
    }

    /**
     * This method indicates if we are already connected to a given peer
     *
     * @param peerId the ID of the peer to test
     * @return true if we have an active connection with the given peer, false otherwise
     */
    public synchronized boolean isConnectedPeer(PeerId peerId) {
        return connectedPeers.containsKey(peerId);
    }

    /**
     * This method retrieves the set of connected peers at a given time
     *
     * @return the set of the peerIDs corresponding to the currently connected peers
     */
    public synchronized Set<PeerId> getConnectedPeers() {
        return new HashSet<>(connectedPeers.keySet());
    }

    /**
     * Returns the next peer with respect to a given one
     *
     * @param peerId the given peer id (does not need to be connected)
     * @return the next peer with respect to peerId that is connected, or null if there are no peers connected
     */
    public synchronized PeerId getNextConnectedPeer(PeerId peerId) {
        if (peerId == null) {
            // any connected peer is a valid result, return the first one, if any
            Iterator<PeerId> it = getConnectedPeers().iterator();
            if (it.hasNext()) {
                return it.next();
            } else {
                // there are no connected peers
                return null;
            }
        } else {
            PeerId nextPeerId = null;
            PeerId lowestPeerId = null;
            for (PeerId connectedPeerId : getConnectedPeers()) {
                // update lowest peer
                if (lowestPeerId == null || connectedPeerId.compareTo(lowestPeerId) < 0) {
                    lowestPeerId = connectedPeerId;
                }
                // update next peer
                if ((nextPeerId == null && connectedPeerId.compareTo(peerId) > 0) ||
                        (nextPeerId != null && connectedPeerId.compareTo(peerId) > 0 && connectedPeerId.compareTo(nextPeerId) < 0)) {
                    nextPeerId = connectedPeerId;
                }
            }
            if (nextPeerId != null) {
                // found a next peer
                return nextPeerId;
            } else {
                // didn't find a next peer, send the lowest peer (if null, there are no peers)
                return lowestPeerId;
            }
        }
    }

    /**
     * Retrieves the connection status from a connected peer. The connection status indicates if this peer is a friend of us and we are a friend
     * of him (CORRECT), he is not a friend of us but we are a friend of him (UNVALIDATED) or he is our friend but we are waiting for
     * him to make us his friend (WAITING_FOR_REMOTE_VALIDATION)
     *
     * @param peerId peer whose connection status we want to retrieve
     * @return the connection status of the given peer (DISCONNECTED if we are not connected to this peer)
     */
    public synchronized ConnectionStatus getPeerConnectionStatus(PeerId peerId) {
        if (isConnectedPeer(peerId)) {
            return connectedPeers.get(peerId).status;
        } else {
            return ConnectionStatus.DISCONNECTED;
        }
    }

    /**
     * Retrieves the connection status from a connected peer. The connection status indicates if this peer is a friend of us and we are a friend
     * of him (CORRECT), he is not a friend of us but we are a friend of him (UNVALIDATED) or he is our friend but we are waiting for
     * him to make us his friend (WAITING_FOR_REMOTE_VALIDATION)
     *
     * @param peerId           peer whose connection status we want to retrieve
     * @param connectionStatus new status
     */
    public synchronized void setPeerConnectionStatus(PeerId peerId, ConnectionStatus connectionStatus) {
        if (isConnectedPeer(peerId)) {
            connectedPeers.get(peerId).status = connectionStatus;
            notificationProcessor.newEvent(peerId);
        }
    }

    /**
     * This method requests an incoming channel for communicating with a connected peer. Channels are requested, for
     * example, for setting up custom peer FSMs
     *
     * @param peerId the ID of the peer for which we request a free channel
     * @return the number of the free incoming channel, or null if there are currently no free channels available
     */
    public synchronized Byte requestChannel(PeerId peerId) {
        if (isConnectedPeer(peerId)) {
            return connectedPeers.get(peerId).availableChannels.requestElement();
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
            PeerId peerId = ccpToPeerID.get(ccp);
            connectedPeers.get(peerId).availableChannels.freeElement(channel);
        }
    }

    /**
     * Retrieves the channel connection point of a connected peer. This element is not intended for public use, but for resource transfer
     *
     * @param peerId peer whose channel connection point we want to retrieve
     * @return the channel connection point of the given peer, or null if we are not connected to this peer
     */
    public synchronized ChannelConnectionPoint getPeerChannelConnectionPoint(PeerId peerId) {
        if (isConnectedPeer(peerId)) {
            return connectedPeers.get(peerId).ccp;
        } else {
            return null;
        }
    }

    /**
     * This methods erases a connected peer (due to this peer disconnecting from us, or we from him, or due to an error)
     * <p/>
     * The communication is already closed, so we simply need to clear resources
     *
     * @param ccp ChannelConnectionPoint associated to the peer who got disconnected
     */
    public synchronized PeerId peerDisconnected(ChannelConnectionPoint ccp) {
        if (ccpToPeerID.containsKey(ccp)) {
            PeerId peerId = ccpToPeerID.get(ccp);
            connectedPeers.remove(peerId);
            ccpToPeerID.remove(ccp);
            notificationProcessor.newEvent(peerId);
            return peerId;
        } else {
            return null;
        }
    }

    public synchronized void disconnectPeer(PeerId peerId) {
        if (connectedPeers.containsKey(peerId)) {
            connectedPeers.get(peerId).ccp.disconnect();
        }
    }

    public synchronized void disconnectAllPeers() {
        for (ChannelConnectionPoint ccp : ccpToPeerID.keySet()) {
            ccp.disconnect();
        }
    }

    @Override
    public String subscribe(String receiverID, NotificationReceiver notificationReceiver) throws IllegalArgumentException {
        return notificationProcessor.subscribeReceiver(receiverID, notificationReceiver);
    }

    @Override
    public String subscribe(String receiverID, NotificationReceiver notificationReceiver, long millis, double timeFactorAtEachEvent, int limit) throws IllegalArgumentException {
        return notificationProcessor.subscribeReceiver(receiverID, notificationReceiver, millis, timeFactorAtEachEvent, limit);
    }

    @Override
    public void unsubscribe(String receiverID) {
        notificationProcessor.unsubscribeReceiver(receiverID);
    }

    public void stop() {
        notificationProcessor.stop();
    }
}
