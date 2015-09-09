package jacz.peerengine.test.personal_data_lists;

import jacz.peerengine.PeerID;
import jacz.peerengine.util.data_synchronization.premade_lists.old.ForeignPeerDataAction;

/**
 *
 */
public class ForeignPeerDataActionImpl implements ForeignPeerDataAction {

    @Override
    public void newPeerNick(PeerID peerID, String nick) {
        System.out.println("Peer " + peerID + " changed his nick to " + nick);
    }

//    @Override
//    public void newPeerState(PeerID peerID, SimplePersonalData.State state) {
//        System.out.println("Peer " + peerID + " changed his state to " + state.name());
//    }
//
//    @Override
//    public void newPeerMessage(PeerID peerID, String message) {
//        System.out.println("Peer " + peerID + " changed his message to " + message);
//    }
//
}
