package jacz.peerengineservice.util.data_synchronization;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.client.PeerClient;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.notification.ProgressNotificationWithError;

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

    /**
     * The PeerClient for which this DataSynchronizer works
     */
    private final PeerClient peerClient;

    private final DataSynchEventsBridge dataSynchEventsBridge;

    /**
     * List container provided by the client, with the lists that can be synched
     */
    private DataAccessorContainer dataAccessorContainer;


    public DataSynchronizer(PeerClient peerClient, DataSynchEvents dataSynchEvents, DataAccessorContainer dataAccessorContainer) {
        this.peerClient = peerClient;
        this.dataSynchEventsBridge = new DataSynchEventsBridge(dataSynchEvents);
        this.dataAccessorContainer = dataAccessorContainer;
    }

    public synchronized SynchRequestResult synchronizeData(PeerID serverPeerID, String dataAccessorName, long timeout) {
        return synchronizeData(serverPeerID, dataAccessorName, timeout, null);
    }

    public synchronized SynchRequestResult synchronizeData(PeerID serverPeerID, final String dataAccessorName, long timeout, final ProgressNotificationWithError<Integer, SynchError> progress) {
        try {
            DataAccessor dataAccessor = dataAccessorContainer.getAccessorForReceiving(serverPeerID, dataAccessorName);
            // same for server FSM
            DataSynchClientFSM dataSynchClientFSM = new DataSynchClientFSM(dataSynchEventsBridge, dataAccessor, dataAccessorName, serverPeerID, progress);
            UniqueIdentifier fsmID = peerClient.registerTimedCustomFSM(
                    serverPeerID,
                    dataSynchClientFSM,
                    DataSynchServerFSM.CUSTOM_FSM_NAME,
                    timeout
            );
            if (fsmID != null) {
                dataSynchEventsBridge.clientSynchRequestInitiated(serverPeerID, dataAccessorName, timeout, fsmID);
                return SynchRequestResult.OK;
            } else {
                dataSynchEventsBridge.clientSynchRequestFailedToInitiate(serverPeerID, dataAccessorName, timeout, SynchRequestResult.PEER_CLIENT_BUSY);
                return SynchRequestResult.PEER_CLIENT_BUSY;
            }
        } catch (UnavailablePeerException e) {
            dataSynchEventsBridge.clientSynchRequestFailedToInitiate(serverPeerID, dataAccessorName, timeout, SynchRequestResult.DISCONNECTED);
            return SynchRequestResult.DISCONNECTED;
        } catch (AccessorNotFoundException e) {
            dataSynchEventsBridge.clientSynchRequestFailedToInitiate(serverPeerID, dataAccessorName, timeout, SynchRequestResult.UNKNOWN_ACCESSOR);
            return SynchRequestResult.UNKNOWN_ACCESSOR;
        }
    }

    DataAccessorContainer getDataAccessorContainer() {
        return dataAccessorContainer;
    }

    DataSynchEventsBridge getDataSynchEventsBridge() {
        return dataSynchEventsBridge;
    }

    public synchronized void stop() {
        dataSynchEventsBridge.stop();
    }
}
