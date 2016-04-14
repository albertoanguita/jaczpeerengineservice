package jacz.peerengineservice.client.customfsm;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerFSMAction;
import jacz.peerengineservice.client.PeerFSMFactory;

/**
 *
 */
public class ProvideFilesFSMFactory implements PeerFSMFactory {

    private ProvideFilesFSM provideFilesFSM;

    public ProvideFilesFSM getProvideFilesFSM() {
        return provideFilesFSM;
    }

    @Override
    public PeerFSMAction<?> buildPeerFSMAction(PeerId clientPeer) {
        provideFilesFSM = new ProvideFilesFSM();
        return provideFilesFSM;
    }

    @Override
    public Long getTimeoutMillis() {
        return 5000L;
    }
}
