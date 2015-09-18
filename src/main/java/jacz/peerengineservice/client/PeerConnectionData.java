package jacz.peerengineservice.client;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.util.sets.availableelements.AvailableElementsByte;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.datatransfer.ResourceStreamingManager;
import jacz.peerengineservice.util.datatransfer.resource_accession.PeerResourceProvider;

/**
 * Connection-related data we have about a peer to which we are currently connected (connection status, ccp and
 * available channels).
 * <p/>
 * Available channels should be initialized with all available except one, which is reserved for handling new requests
 * (usually channel 0)
 *
 * todo not used
 */
class PeerConnectionData {

    /**
     * ID of this peer
     */
    PeerID peerID;

    /**
     * The connection status of this peer with respect to us (for internal purpose)
     */
    ConnectionStatus status;

    /**
     * ChannelConnectionPoint used to reach this client
     */
    ChannelConnectionPoint ccp;

    /**
     * Available channels of this client
     */
    AvailableElementsByte availableChannels;

    /**
     * Resource provider for requesting resources to this peer
     */
    PeerResourceProvider peerResourceProvider;

    public PeerConnectionData(ConnectionStatus status, ChannelConnectionPoint ccp, PeerID ownPeerID, PeerID peerID, ResourceStreamingManager resourceStreamingManager, Byte... occupiedChannels) {
        this.peerID = peerID;
        this.status = status;
        this.ccp = ccp;
        availableChannels = new AvailableElementsByte(occupiedChannels);
        peerResourceProvider = new PeerResourceProvider(ownPeerID, peerID, resourceStreamingManager);
    }
}
