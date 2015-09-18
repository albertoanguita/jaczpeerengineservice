package jacz.peerengineservice.test.customfsm;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.test.SimplePeerClientActionImpl;
import jacz.peerengineservice.util.ConnectionStatus;

/**
 * Simple peer action for custom fsm
 */
public class SimplePeerClientActionImplCustom extends SimplePeerClientActionImpl {

    @Override
    public void newPeerConnected(PeerID peerID, ConnectionStatus status) {
        super.newPeerConnected(peerID, status);
        if (equalsPeerID(1)) {
            try {
                client.getPeerClient().registerTimedCustomFSM(peerID, new AskFilesFSM(), ProvideFilesFSM.SERVER_FSM, 1000);
            } catch (UnavailablePeerException e) {
                System.out.println("unavailable peer");
            }
        }
    }
}
