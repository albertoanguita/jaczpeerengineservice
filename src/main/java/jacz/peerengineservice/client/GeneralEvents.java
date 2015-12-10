package jacz.peerengineservice.client;

import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.peerengineservice.util.datatransfer.DownloadsManager;
import jacz.peerengineservice.util.datatransfer.UploadsManager;

/**
 * General events (peers, personal data)
 */
public interface GeneralEvents {

    void peerAddedAsFriend(PeerID peerID, PeerRelations peerRelations);

    void peerRemovedAsFriend(PeerID peerID, PeerRelations peerRelations);

    void peerAddedAsBlocked(PeerID peerID, PeerRelations peerRelations);

    void peerRemovedAsBlocked(PeerID peerID, PeerRelations peerRelations);

    void newPeerConnected(PeerID peerID, ConnectionStatus status);

    void newObjectMessage(PeerID peerID, Object message);

    void newPeerNick(PeerID peerID, String nick);

    void peerValidatedUs(PeerID peerID);

    void peerDisconnected(PeerID peerID, CommError error);

    void stop();

}
