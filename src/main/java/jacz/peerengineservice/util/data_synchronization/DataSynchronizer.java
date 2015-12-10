package jacz.peerengineservice.util.data_synchronization;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.client.PeerClient;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.notification.ProgressNotificationWithError;
import org.apache.log4j.Logger;

/**
 * Data Synchronizer
 */
public class DataSynchronizer {

    public enum SynchRequestResult {
        OK,
        PEER_CLIENT_BUSY,
        DISCONNECTED,
        UNKNOWN_ACCESSOR,
    }

    static final long SERVER_FSM_TIMEOUT = 15000;

    public static final int PROGRESS_MAX = 100;

    static final Logger logger = Logger.getLogger(DataSynchronizer.class);

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

    public synchronized SynchRequestResult synchronizeData(PeerID serverPeerID, String dataAccessorName, long timeout) {
        return synchronizeData(serverPeerID, dataAccessorName, timeout, null);
    }

    public synchronized SynchRequestResult synchronizeData(PeerID serverPeerID, final String dataAccessorName, long timeout, final ProgressNotificationWithError<Integer, SynchError> progress) {
        try {
            DataAccessor dataAccessor = dataAccessorContainer.getAccessorForReceiving(serverPeerID, dataAccessorName);
            // same for server FSM
            DataSynchClientFSM dataSynchClientFSM = new DataSynchClientFSM(dataAccessor, dataAccessorName, serverPeerID, progress);
            UniqueIdentifier fsmID = peerClient.registerTimedCustomFSM(
                    serverPeerID,
                    dataSynchClientFSM,
                    DataSynchServerFSM.CUSTOM_FSM_NAME,
                    timeout
            );
            if (fsmID != null) {
                logger.info("CLIENT SYNCH REQUEST INITIATED. serverPeer: " + serverPeerID + ". dataAccessorName: " + dataAccessorName + ". timeout: " + timeout + ". fsmID: " + fsmID);
                return SynchRequestResult.OK;
            } else {
                logger.info("CLIENT SYNCH REQUEST FAILED TO INITIATE. serverPeer: " + serverPeerID + ". dataAccessorName: " + dataAccessorName + ". timeout: " + timeout + ". synchError: " + SynchRequestResult.PEER_CLIENT_BUSY);
                return SynchRequestResult.PEER_CLIENT_BUSY;
            }
        } catch (UnavailablePeerException e) {
            logger.info("CLIENT SYNCH REQUEST FAILED TO INITIATE. serverPeer: " + serverPeerID + ". dataAccessorName: " + dataAccessorName + ". timeout: " + timeout + ". synchError: " + SynchRequestResult.DISCONNECTED);
            return SynchRequestResult.DISCONNECTED;
        } catch (AccessorNotFoundException e) {
            logger.info("CLIENT SYNCH REQUEST FAILED TO INITIATE. serverPeer: " + serverPeerID + ". dataAccessorName: " + dataAccessorName + ". timeout: " + timeout + ". synchError: " + SynchRequestResult.UNKNOWN_ACCESSOR);
            return SynchRequestResult.UNKNOWN_ACCESSOR;
        }
    }

    DataAccessorContainer getDataAccessorContainer() {
        return dataAccessorContainer;
    }
}
