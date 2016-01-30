package jacz.peerengineservice.util.data_synchronization;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.client.PeerClient;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.notification.ProgressNotificationWithError;
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
            PeerID serverPeerID,
            DataAccessor dataAccessor,
            long timeout) throws UnavailablePeerException {
        return synchronizeData(serverPeerID, dataAccessor, timeout, null);
    }

    public synchronized boolean synchronizeData(
            PeerID serverPeerID,
            DataAccessor dataAccessor,
            long timeout,
            final ProgressNotificationWithError<Integer, SynchError> progress) throws UnavailablePeerException {
//        DataAccessor dataAccessor = dataAccessorContainer.getAccessorForReceiving(serverPeerID, dataAccessorName);
        // same for server FSM
        DataSynchClientFSM dataSynchClientFSM = new DataSynchClientFSM(dataAccessor, serverPeerID, progress);
        UniqueIdentifier fsmID = peerClient.registerTimedCustomFSM(
                serverPeerID,
                dataSynchClientFSM,
                DataSynchServerFSM.CUSTOM_FSM_NAME,
                timeout
        );
        if (fsmID != null) {
            logger.info("CLIENT SYNCH REQUEST INITIATED. serverPeer: " + serverPeerID + ". dataAccessorName: " + dataAccessor.getName() + ". timeout: " + timeout + ". fsmID: " + fsmID);
            return true;
        } else {
            logger.info("CLIENT SYNCH REQUEST FAILED TO INITIATE (peer client busy). serverPeer: " + serverPeerID + ". dataAccessorName: " + dataAccessor.getName() + ". timeout: " + timeout);
            return false;
        }
    }

    public DataAccessorContainer getDataAccessorContainer() {
        return dataAccessorContainer;
    }
}
