package jacz.peerengine.util.data_synchronization.old;

import jacz.peerengine.client.PeerFSMAction;
import jacz.peerengine.client.PeerFSMFactory;
import jacz.peerengine.util.ConnectionStatus;

/**
 * This class implements a factory of FSM for synchronizing single elements at the server side
 */
public class ElementSynchronizerServerFSMFactory implements PeerFSMFactory {

    private ListSynchronizer listSynchronizer;

    public ElementSynchronizerServerFSMFactory(ListSynchronizer listSynchronizer) {
        this.listSynchronizer = listSynchronizer;
    }

    @Override
    public PeerFSMAction buildPeerFSMAction(ConnectionStatus connectionStatus) {
        return new ElementSynchronizerServerFSM(listSynchronizer);
    }

    @Override
    public Long getTimeoutMillis() {
        return ListSynchronizer.SERVER_FSM_TIMEOUT;
    }
}
