package jacz.peerengineservice.util.data_synchronization;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.client.PeerClient;
import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.concurrency.task_executor.ParallelTaskExecutor;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.notification.ProgressNotificationWithError;

/**
 * Data Synchronizer
 */
public class DataSynchronizer {


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

    /**
     * Owr own peer id
     */
    private PeerID ownPeerID;


    public DataSynchronizer(PeerClient peerClient, DataSynchEvents dataSynchEvents, DataAccessorContainer dataAccessorContainer, PeerID ownPeerID) {
        this.peerClient = peerClient;
        this.dataSynchEventsBridge = new DataSynchEventsBridge(dataSynchEvents);
        this.dataAccessorContainer = dataAccessorContainer;
        this.ownPeerID = ownPeerID;
    }

    public synchronized void synchronizeData(PeerID serverPeerID, String dataAccessorName, long timeout) {
        synchronizeData(serverPeerID, dataAccessorName, timeout, null);
    }

    public synchronized void synchronizeData(PeerID serverPeerID, final String dataAccessorName, long timeout, final ProgressNotificationWithError<Integer, SynchError> progress) {
        try {
            DataAccessor dataAccessor = dataAccessorContainer.getAccessorForReceiving(serverPeerID, dataAccessorName);
            // same for server FSM
            DataSynchClientFSM dataSynchClientFSM = new DataSynchClientFSM(dataSynchEventsBridge, dataAccessor, dataAccessorName, ownPeerID, serverPeerID, progress);
            UniqueIdentifier fsmID = peerClient.registerTimedCustomFSM(
                    serverPeerID,
                    dataSynchClientFSM,
                    DataSynchServerFSM.CUSTOM_FSM_NAME,
                    timeout
            );
            if (fsmID != null) {
                dataSynchEventsBridge.clientSynchRequestInitiated(serverPeerID, dataAccessorName, timeout, fsmID);
            } else {
                dataSynchEventsBridge.clientSynchRequestFailedToInitiate(serverPeerID, dataAccessorName, timeout, new SynchError(SynchError.Type.PEER_CLIENT_BUSY, null));
                ParallelTaskExecutor.executeTask(new ParallelTask() {
                    @Override
                    public void performTask() {
                        if (progress != null) {
                            progress.error(new SynchError(SynchError.Type.PEER_CLIENT_BUSY, null));
                        }
                    }
                });
            }
        } catch (UnavailablePeerException e) {
            dataSynchEventsBridge.clientSynchRequestFailedToInitiate(serverPeerID, dataAccessorName, timeout, new SynchError(SynchError.Type.DISCONNECTED, null));
            ParallelTaskExecutor.executeTask(new ParallelTask() {
                @Override
                public void performTask() {
                    if (progress != null) {
                        progress.error(new SynchError(SynchError.Type.DISCONNECTED, null));
                    }
                }
            });
        } catch (AccessorNotFoundException e) {
            dataSynchEventsBridge.clientSynchRequestFailedToInitiate(serverPeerID, dataAccessorName, timeout, new SynchError(SynchError.Type.UNKNOWN_ACCESSOR, "Unknown accessor name: " + dataAccessorName));
            ParallelTaskExecutor.executeTask(new ParallelTask() {
                @Override
                public void performTask() {
                    if (progress != null) {
                        progress.error(new SynchError(SynchError.Type.UNKNOWN_ACCESSOR, "Unknown accessor name: " + dataAccessorName));
                    }
                }
            });
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
