package jacz.peerengineservice.util.data_synchronization;

import jacz.peerengineservice.client.PeerFSMAction;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.util.ConnectionStatus;

/**
 * Created by Alberto on 17/09/2015.
 */
public class DataSynchServerFSMFactory implements PeerFSMFactory {

    private final DataAccessorContainer dataAccessorContainer;

    public DataSynchServerFSMFactory(DataSynchronizer dataSynchronizer) {
        this.dataAccessorContainer = dataSynchronizer.getDataAccessorContainer();
    }

    @Override
    public PeerFSMAction buildPeerFSMAction(ConnectionStatus requestingPeerStatus) {
        // todo check peer status here, not after returning the FSM!!!
        return new DataSynchServerFSM(requestingPeerStatus, dataAccessorContainer);
    }

    @Override
    public Long getTimeoutMillis() {
        return DataSynchronizer.SERVER_FSM_TIMEOUT;
    }
}
