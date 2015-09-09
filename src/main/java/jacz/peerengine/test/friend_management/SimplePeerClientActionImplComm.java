package jacz.peerengine.test.friend_management;

import jacz.peerengine.PeerID;
import jacz.peerengine.test.SimplePeerClientActionImpl;
import jacz.peerengine.util.ConnectionStatus;

/**
 *
 */
public class SimplePeerClientActionImplComm extends SimplePeerClientActionImpl {

    @Override
    public void newPeerConnected(PeerID peerID, ConnectionStatus status) {
        super.newPeerConnected(peerID, status);
        if (equalsPeerID(1)) {
            client.getPeerClient().sendChatMessage(peerID, "Hola 2, soy 1");
        } else if (equalsPeerID(2)) {
            client.getPeerClient().sendObjectMessage(peerID, "Object: Hola 1, soy 2");
        }
    }
}
