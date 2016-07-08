package jacz.peerengineservice.util.data_synchronization;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.client.PeerClient;
import org.aanguita.jacuzzi.notification.ProgressNotificationWithError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data Synchronizer
 */
public class DataSynchronizer {

    static final long SERVER_FSM_TIMEOUT = 15000;

    public static final int PROGRESS_MAX = 100;

    static final Logger logger = LoggerFactory.getLogger(DataSynchronizer.class);

    /**
     * The PeerClient for which this DataSynchronizer works
     */
    private final PeerClient peerClient;

    /**
     * List container provided by the client, with the lists that can be synched
     */
    private DataAccessorContainer dataAccessorContainer;


    public DataSynchronizer(PeerClient peerClient, DataAccessorContainer dataAccessorContainer) {
        this.peerClient = peerClient;
        this.dataAccessorContainer = dataAccessorContainer;
    }

    public synchronized boolean synchronizeData(
            PeerId serverPeerId,
            DataAccessor dataAccessor,
            long timeout) throws UnavailablePeerException {
        return synchronizeData(serverPeerId, dataAccessor, timeout, null);
    }

    public synchronized boolean synchronizeData(
            PeerId serverPeerId,
            DataAccessor dataAccessor,
            long timeout,
            final ProgressNotificationWithError<Integer, SynchError> progress) throws UnavailablePeerException {
        // same for server FSM
        DataSynchClientFSM dataSynchClientFSM = new DataSynchClientFSM(dataAccessor, serverPeerId, progress);
        String fsmID = peerClient.registerTimedCustomFSM(
                serverPeerId,
                dataSynchClientFSM,
                DataSynchServerFSM.CUSTOM_FSM_NAME,
                timeout
        );
        if (fsmID != null) {
            logger.info("CLIENT SYNCH REQUEST INITIATED. serverPeer: " + serverPeerId + ". dataAccessorName: " + dataAccessor.getName() + ". timeout: " + timeout + ". fsmID: " + fsmID);
            return true;
        } else {
            logger.info("CLIENT SYNCH REQUEST FAILED TO INITIATE (peer client busy). serverPeer: " + serverPeerId + ". dataAccessorName: " + dataAccessor.getName() + ". timeout: " + timeout);
            return false;
        }
    }

    public DataAccessorContainer getDataAccessorContainer() {
        return dataAccessorContainer;
    }
}
