package jacz.peerengine.util.data_synchronization.premade_lists.old;

import jacz.peerengine.PeerID;

/**
 * Changes in the personal data of other peers
 */
public interface ForeignPeerDataAction {

    void newPeerNick(PeerID peerID, String nick);
}
