package jacz.peerengineservice.util.data_synchronization;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerFSMAction;
import jacz.peerengineservice.client.PeerFSMFactory;

/**
 * Routes synch requests to corresponding FSMs
 */
public class DataSynchServerFSMFactory implements PeerFSMFactory {

    private final DataAccessorContainer dataAccessorContainer;

    public DataSynchServerFSMFactory(DataSynchronizer dataSynchronizer) {
        this.dataAccessorContainer = dataSynchronizer.getDataAccessorContainer();
    }

    @Override
    public PeerFSMAction buildPeerFSMAction(PeerId clientPeerId) {
        return new DataSynchServerFSM(clientPeerId, dataAccessorContainer);
    }

    @Override
    public Long getTimeoutMillis() {
        return DataSynchronizer.SERVER_FSM_TIMEOUT;
    }
}
