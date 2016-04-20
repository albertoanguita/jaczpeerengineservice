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

    void newPeerNick(PeerId peerId, String nick, PeerInfo peerInfo);

    void peerDisconnected(PeerId peerId, PeerInfo peerInfo, CommError error);
}
