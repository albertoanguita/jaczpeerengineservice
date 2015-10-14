package jacz.peerengineservice.util.data_synchronization;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerFSMAction;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.util.ConnectionStatus;

/**
 * Created by Alberto on 17/09/2015.
 */
public class DataSynchServerFSMFactory implements PeerFSMFactory {

    private final DataAccessorContainer dataAccessorContainer;

    private final DataSynchEventsBridge dataSynchEventsBridge;

    public DataSynchServerFSMFactory(DataSynchronizer dataSynchronizer) {
        this.dataAccessorContainer = dataSynchronizer.getDataAccessorContainer();
        this.dataSynchEventsBridge = dataSynchronizer.getDataSynchEventsBridge();
    }

    @Override
    public PeerFSMAction buildPeerFSMAction(PeerID clientPeerID, ConnectionStatus requestingPeerStatus) {
        if (requestingPeerStatus.isFriend()) {
            return new DataSynchServerFSM(dataSynchEventsBridge, clientPeerID, dataAccessorContainer);
        } else {
            dataSynchEventsBridge.serverSynchRequestDenied(clientPeerID, null, null, new SynchError(SynchError.Type.NO_PERMISSION, null));
            return null;
        }
    }

    @Override
    public Long getTimeoutMillis() {
        return DataSynchronizer.SERVER_FSM_TIMEOUT;
    }
}
