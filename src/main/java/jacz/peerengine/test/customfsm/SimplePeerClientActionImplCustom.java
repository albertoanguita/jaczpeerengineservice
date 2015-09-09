package jacz.peerengine.test.customfsm;

import jacz.peerengine.PeerID;
import jacz.peerengine.test.SimplePeerClientActionImpl;
import jacz.peerengine.util.ConnectionStatus;

/**
 * Simple peer action for custom fsm
 */
public class SimplePeerClientActionImplCustom extends SimplePeerClientActionImpl {

    @Override
    public void newPeerConnected(PeerID peerID, ConnectionStatus status) {
        super.newPeerConnected(peerID, status);
        if (equalsPeerID(1)) {
            client.getPeerClient().registerTimedCustomFSM(peerID, new AskFilesFSM(), ProvideFilesFSM.SERVER_FSM, 1000);
        }
    }
}
