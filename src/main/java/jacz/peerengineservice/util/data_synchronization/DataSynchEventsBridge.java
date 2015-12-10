package jacz.peerengineservice.util.data_synchronization;

import jacz.peerengineservice.PeerID;
import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.concurrency.task_executor.SequentialTaskExecutor;
import jacz.util.identifier.UniqueIdentifier;
import org.apache.log4j.Logger;

/**
 * This class acts as a bypass of the client's provided DataSynchEvents implementation, logging all activity
 *
 * In addition, it takes care of generating a thread for each method call, so invoker does not have to worry about it
 */
public class DataSynchEventsBridge implements DataSynchEvents {

    final static Logger logger = Logger.getLogger(DataSynchEvents.class);

    private final DataSynchEvents dataSynchEvents;

    private final SequentialTaskExecutor sequentialTaskExecutor;

    public DataSynchEventsBridge(DataSynchEvents dataSynchEvents) {
        this.dataSynchEvents = dataSynchEvents;
        sequentialTaskExecutor = new SequentialTaskExecutor();
    }

    @Override
    public void clientSynchRequestInitiated(final PeerID serverPeer, final String dataAccessorName, final long timeout, final UniqueIdentifier fsmID) {
        logger.info("CLIENT SYNCH REQUEST INITIATED. serverPeer: " + serverPeer + ". dataAccessorName: " + dataAccessorName + ". timeout: " + timeout + ". fsmID: " + fsmID);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                dataSynchEvents.clientSynchRequestInitiated(serverPeer, dataAccessorName, timeout, fsmID);
            }
        });
    }

    @Override
    public void clientSynchRequestFailedToInitiate(final PeerID serverPeer, final String dataAccessorName, final long timeout, final DataSynchronizer.SynchRequestResult synchRequestResult) {
        logger.info("CLIENT SYNCH REQUEST FAILED TO INITIATE. serverPeer: " + serverPeer + ". dataAccessorName: " + dataAccessorName + ". timeout: " + timeout + ". synchError: " + synchRequestResult);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                dataSynchEvents.clientSynchRequestFailedToInitiate(serverPeer, dataAccessorName, timeout, synchRequestResult);
            }
        });
    }

    @Override
    public void clientSynchRequestDenied(final PeerID serverPeer, final String dataAccessorName, final UniqueIdentifier fsmID, final SynchError synchError) {
        logger.info("CLIENT SYNCH REQUEST DENIED. serverPeer: " + serverPeer + ". dataAccessorName: " + dataAccessorName + ". fsmID: " + fsmID + ". synchError: " + synchError);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                dataSynchEvents.clientSynchRequestDenied(serverPeer, dataAccessorName, fsmID, synchError);
            }
        });
    }

    @Override
    public void clientSynchSuccess(final PeerID serverPeer, final String dataAccessorName, final UniqueIdentifier fsmID) {
        logger.info("CLIENT SYNCH SUCCESS. serverPeer: " + serverPeer + ". dataAccessorName: " + dataAccessorName + ". fsmID: " + fsmID);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                dataSynchEvents.clientSynchSuccess(serverPeer, dataAccessorName, fsmID);
            }
        });
    }

    @Override
    public void clientSynchError(final PeerID serverPeer, final String dataAccessorName, final UniqueIdentifier fsmID, final SynchError synchError) {
        logger.info("CLIENT SYNCH ERROR. serverPeer: " + serverPeer + ". dataAccessorName: " + dataAccessorName + ". fsmID: " + fsmID + ". synchError: " + synchError);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                dataSynchEvents.clientSynchError(serverPeer, dataAccessorName, fsmID, synchError);
            }
        });
    }

    @Override
    public void clientSynchTimeout(final PeerID serverPeer, final String dataAccessorName, final UniqueIdentifier fsmID) {
        logger.info("CLIENT SYNCH TIMEOUT. serverPeer: " + serverPeer + ". dataAccessorName: " + dataAccessorName + ". fsmID: " + fsmID);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                dataSynchEvents.clientSynchTimeout(serverPeer, dataAccessorName, fsmID);
            }
        });
    }

    @Override
    public void serverSynchRequestAccepted(final PeerID clientPeer, final String dataAccessorName, final UniqueIdentifier fsmID) {
        logger.info("SERVER SYNCH REQUEST ACCEPTED. clientPeer: " + clientPeer + ". dataAccessorName: " + dataAccessorName + ". fsmID: " + fsmID);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                dataSynchEvents.serverSynchRequestAccepted(clientPeer, dataAccessorName, fsmID);
            }
        });
    }

    @Override
    public void serverSynchRequestDenied(final PeerID clientPeer, final String dataAccessorName, final UniqueIdentifier fsmID, final SynchError synchError) {
        logger.info("SERVER SYNCH REQUEST DENIED. clientPeer: " + clientPeer + ". dataAccessorName: " + dataAccessorName + ". fsmID: " + fsmID + ". synchError: " + synchError);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                dataSynchEvents.serverSynchRequestDenied(clientPeer, dataAccessorName, fsmID, synchError);
            }
        });
    }

    @Override
    public void serverSynchSuccess(final PeerID clientPeer, final String dataAccessorName, final UniqueIdentifier fsmID) {
        logger.info("SERVER SYNCH SUCCESS. clientPeer: " + clientPeer + ". dataAccessorName: " + dataAccessorName + ". fsmID: " + fsmID);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                dataSynchEvents.serverSynchSuccess(clientPeer, dataAccessorName, fsmID);
            }
        });
    }

    @Override
    public void serverSynchError(final PeerID clientPeer, final String dataAccessorName, final UniqueIdentifier fsmID, final SynchError synchError) {
        logger.info("SERVER SYNCH ERROR. clientPeer: " + clientPeer + ". dataAccessorName: " + dataAccessorName + ". fsmID: " + fsmID + ". synchError: " + synchError);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                dataSynchEvents.serverSynchError(clientPeer, dataAccessorName, fsmID, synchError);
            }
        });
    }

    @Override
    public void serverSynchTimeout(final PeerID clientPeer, final String dataAccessorName, final UniqueIdentifier fsmID) {
        logger.info("SERVER SYNCH TIMEOUT. clientPeer: " + clientPeer + ". dataAccessorName: " + dataAccessorName + ". fsmID: " + fsmID);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                dataSynchEvents.serverSynchTimeout(clientPeer, dataAccessorName, fsmID);
            }
        });
    }

    void stop() {
        sequentialTaskExecutor.stopAndWaitForFinalization();
    }
}
