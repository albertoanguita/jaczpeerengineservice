package jacz.peerengineservice.test.customfsm;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerFSMAction;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.util.ConnectionStatus;

/**
 *
 */
public class ProvideFilesFSMFactory implements PeerFSMFactory {

    @Override
    public PeerFSMAction<?> buildPeerFSMAction(PeerID clientPeer, ConnectionStatus requestingPeerStatus) {
        return new ProvideFilesFSM();
    }

    @Override
    public Long getTimeoutMillis() {
        return 5000l;
    }
}
