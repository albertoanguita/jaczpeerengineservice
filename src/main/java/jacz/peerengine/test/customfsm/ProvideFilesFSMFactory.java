package jacz.peerengine.test.customfsm;

import jacz.peerengine.client.PeerFSMAction;
import jacz.peerengine.client.PeerFSMFactory;
import jacz.peerengine.util.ConnectionStatus;

/**
 *
 */
public class ProvideFilesFSMFactory implements PeerFSMFactory {

    @Override
    public PeerFSMAction<?> buildPeerFSMAction(ConnectionStatus connectionStatus) {
        return new ProvideFilesFSM();
    }

    @Override
    public Long getTimeoutMillis() {
        return 5000l;
    }
}
