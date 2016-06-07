package jacz.peerengineservice.client.connection.peers;

import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.PeerRelationship;

/**
 * Peer-related events
 */
public interface PeersEvents {

    void newPeerConnected(PeerId peerId, PeerInfo peerInfo);

    void modifiedPeerRelationship(PeerId peerId, PeerInfo peerInfo);

    void modifiedMainCountry(PeerId peerId, PeerInfo peerInfo);

    void modifiedAffinity(PeerId peerId, PeerInfo peerInfo);

    void newPeerNick(PeerId peerId, PeerInfo peerInfo);

    void peerDisconnected(PeerId peerId, PeerInfo peerInfo, CommError error);
}
