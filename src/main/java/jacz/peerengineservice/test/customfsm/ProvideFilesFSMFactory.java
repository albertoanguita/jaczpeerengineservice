package jacz.peerengineservice.test.customfsm;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerFSMAction;
import jacz.peerengineservice.client.PeerFSMFactory;

/**
 *
 */
public class ProvideFilesFSMFactory implements PeerFSMFactory {

    @Override
    public PeerFSMAction<?> buildPeerFSMAction(PeerId clientPeer) {
        return new ProvideFilesFSM();
    }

    @Override
    public Long getTimeoutMillis() {
        return 5000l;
    }
}
