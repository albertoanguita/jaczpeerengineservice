package jacz.peerengineservice.util.data_synchronization.old;

import jacz.peerengineservice.client.PeerFSMAction;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.util.ConnectionStatus;

/**
 * This class implements a factory of FSM for synchronizing single elements at the server side
 */
public class ElementSynchronizerServerFSMFactory implements PeerFSMFactory {

    private ListSynchronizer listSynchronizer;

    public ElementSynchronizerServerFSMFactory(ListSynchronizer listSynchronizer) {
        this.listSynchronizer = listSynchronizer;
    }

    @Override
    public PeerFSMAction buildPeerFSMAction(ConnectionStatus requestingPeerStatus) {
        return new ElementSynchronizerServerFSM(listSynchronizer);
    }

    @Override
    public Long getTimeoutMillis() {
        return ListSynchronizer.SERVER_FSM_TIMEOUT;
    }
}
