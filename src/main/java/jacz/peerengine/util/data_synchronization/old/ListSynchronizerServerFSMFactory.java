package jacz.peerengine.util.data_synchronization.old;

import jacz.peerengine.client.PeerFSMAction;
import jacz.peerengine.client.PeerFSMFactory;
import jacz.peerengine.util.ConnectionStatus;

/**
 * This class implements a factory of FSM for synchronizing lists at the server side
 */
public class ListSynchronizerServerFSMFactory implements PeerFSMFactory {

    private ListSynchronizer listSynchronizer;

    public ListSynchronizerServerFSMFactory(ListSynchronizer listSynchronizer) {
        this.listSynchronizer = listSynchronizer;
    }

    @Override
    public PeerFSMAction buildPeerFSMAction(ConnectionStatus connectionStatus) {
        return new ListSynchronizerServerFSM(listSynchronizer);
    }

    @Override
    public Long getTimeoutMillis() {
        return ListSynchronizer.SERVER_FSM_TIMEOUT;
    }
}
