package jacz.peerengineservice.util.data_synchronization;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerFSMAction;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.util.ConnectionStatus;

/**
 * Routes synch requests to corresponding FSMs
 */
public class DataSynchServerFSMFactory implements PeerFSMFactory {

    private final DataAccessorContainer dataAccessorContainer;

    public DataSynchServerFSMFactory(DataSynchronizer dataSynchronizer) {
        this.dataAccessorContainer = dataSynchronizer.getDataAccessorContainer();
    }

    @Override
    public PeerFSMAction buildPeerFSMAction(PeerId clientPeerId, ConnectionStatus requestingPeerStatus) {
        if (requestingPeerStatus.isFriend()) {
            return new DataSynchServerFSM(clientPeerId, dataAccessorContainer);
        } else {
            DataSynchronizer.logger.info("SERVER SYNCH REQUEST DENIED. clientPeer: " + clientPeerId + ". dataAccessorName: " + null + ". fsmID: " + null + ". synchError: " + new SynchError(SynchError.Type.NO_PERMISSION, null));
            return null;
        }
    }

    @Override
    public Long getTimeoutMillis() {
        return DataSynchronizer.SERVER_FSM_TIMEOUT;
    }
}
