package jacz.peerengineservice.util.data_synchronization;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.client.PeerClient;
import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.concurrency.task_executor.ParallelTaskExecutor;
import jacz.util.notification.ProgressNotificationWithError;

/**
 * Created by Alberto on 17/09/2015.
 */
public class DataSynchronizer {


    static final long SERVER_FSM_TIMEOUT = 15000;

    public static final int PROGRESS_MAX = 100;

    /**
     * The PeerClient for which this DataSynchronizer works
     */
    private final PeerClient peerClient;

    /**
     * List container provided by the client, with the lists that can be synched
     */
    private DataAccessorContainer dataAccessorContainer;

    /**
     * Owr own peer id
     */
    private PeerID ownPeerID;


    public DataSynchronizer(PeerClient peerClient, DataAccessorContainer dataAccessorContainer, PeerID ownPeerID) {
        this.peerClient = peerClient;
        this.dataAccessorContainer = dataAccessorContainer;
        this.ownPeerID = ownPeerID;
    }

    public void synchronizeData(PeerID serverPeerID, String dataAccessorName, long timeout) {
        synchronizeData(serverPeerID, dataAccessorName, timeout, null);
    }

    public void synchronizeData(PeerID serverPeerID, final String dataAccessorName, long timeout, final ProgressNotificationWithError<Integer, SynchError> progress) {
        try {
            DataAccessor dataAccessor = dataAccessorContainer.getAccessorForReceiving(serverPeerID, dataAccessorName);
            boolean correctSetup = peerClient.registerTimedCustomFSM(
                    serverPeerID,
                    new DataSynchClientFSM(dataAccessor, dataAccessorName, ownPeerID, progress),
                    DataSynchServerFSM.CUSTOM_FSM_NAME,
                    timeout
            );

            if (!correctSetup) {
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
            ParallelTaskExecutor.executeTask(new ParallelTask() {
                @Override
                public void performTask() {
                    if (progress != null) {
                        progress.error(new SynchError(SynchError.Type.DISCONNECTED, null));
                    }
                }
            });
        } catch (AccessorNotFoundException e) {
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


    PeerClient getPeerClient() {
        return peerClient;
    }

    public DataAccessorContainer getDataAccessorContainer() {
        return dataAccessorContainer;
    }
}
