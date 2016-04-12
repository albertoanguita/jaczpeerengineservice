package jacz.peerengineservice.client;

import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.PeerRelationship;

/**
 * General events (peers, personal data)
 */
public interface GeneralEvents {

    void peerAddedAsFriend(PeerId peerId);

    void peerRemovedAsFriend(PeerId peerId);

    void peerAddedAsBlocked(PeerId peerId);

    void peerRemovedAsBlocked(PeerId peerId);

    void newPeerConnected(PeerId peerId, PeerRelationship peerRelationship);

    void modifiedPeerRelationship(PeerId peerId, PeerRelationship peerRelationship, boolean connected);

    void newObjectMessage(PeerId peerId, Object message);

    void newPeerNick(PeerId peerId, String nick);

    void peerDisconnected(PeerId peerId, CommError error);

    void stop();

}
