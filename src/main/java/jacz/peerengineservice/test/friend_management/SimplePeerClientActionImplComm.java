package jacz.peerengineservice.test.friend_management;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.test.SimplePeerClientActionImpl;
import jacz.peerengineservice.util.ConnectionStatus;

/**
 *
 */
public class SimplePeerClientActionImplComm extends SimplePeerClientActionImpl {

    @Override
    public void newPeerConnected(PeerID peerID, ConnectionStatus status) {
        super.newPeerConnected(peerID, status);
        if (equalsPeerID(1)) {
            client.getPeerClient().sendObjectMessage(peerID, "Hola 2, soy 1");

            client.getPeerClient().sendObjectMessage(peerID, "jajaja");
            client.getPeerClient().sendObjectMessage(peerID, "mierda");
            client.getPeerClient().sendObjectMessage(peerID, "joder");
        } else if (equalsPeerID(2)) {
            client.getPeerClient().sendObjectMessage(peerID, "Object: Hola 1, soy 2");
            client.getPeerClient().sendObjectMessage(peerID, "uffff");
            client.getPeerClient().sendObjectMessage(peerID, "puta");
            client.getPeerClient().sendObjectMessage(peerID, "lalelo");
        }
    }
}
