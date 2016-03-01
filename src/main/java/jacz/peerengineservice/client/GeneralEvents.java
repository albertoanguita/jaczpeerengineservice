package jacz.peerengineservice.client;

import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.ConnectionStatus;

/**
 * General events (peers, personal data)
 */
public interface GeneralEvents {

    void peerAddedAsFriend(PeerId peerId, PeerRelations peerRelations);

    void peerRemovedAsFriend(PeerId peerId, PeerRelations peerRelations);

    void peerAddedAsBlocked(PeerId peerId, PeerRelations peerRelations);

    void peerRemovedAsBlocked(PeerId peerId, PeerRelations peerRelations);

    void newPeerConnected(PeerId peerId, ConnectionStatus status);

    void newObjectMessage(PeerId peerId, Object message);

    void newPeerNick(PeerId peerId, String nick);

    void peerValidatedUs(PeerId peerId);

    void peerDisconnected(PeerId peerId, CommError error);

    void stop();

}
