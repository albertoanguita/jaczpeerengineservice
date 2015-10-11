package jacz.peerengineservice.util.data_synchronization;

import jacz.peerengineservice.PeerID;
import jacz.util.concurrency.task_executor.SequentialTaskExecutor;
import jacz.util.identifier.UniqueIdentifier;

/**
 * This class acts as a bypass of the client's provided DataSynchEvents implementation, logging all activity
 *
 * In addition, it takes care of generating a thread for each method call, so invoker does not have to worry about it
 */
public class DataSynchEventsBridge implements DataSynchEvents {

    private final DataSynchEvents dataSynchEvents;

    private final SequentialTaskExecutor sequentialTaskExecutor;

    public DataSynchEventsBridge(DataSynchEvents dataSynchEvents) {
        this.dataSynchEvents = dataSynchEvents;
        sequentialTaskExecutor = new SequentialTaskExecutor();
    }

    @Override
    public void clientSynchRequestInitiated(PeerID serverPeer, String dataAccessorName, long timeout, UniqueIdentifier fsmID) {

    }

    @Override
    public void clientSynchRequestFailedToInitiate(PeerID serverPeer, String dataAccessorName, long timeout, SynchError synchError) {

    }

    @Override
    public void clientSynchSuccess(PeerID serverPeer, String dataAccessorName, UniqueIdentifier fsmID) {

    }

    @Override
    public void clientSynchError(PeerID serverPeer, String dataAccessorName, UniqueIdentifier fsmID, SynchError synchError) {

    }

    @Override
    public void clientSynchTimeout(PeerID serverPeer, String dataAccessorName, UniqueIdentifier fsmID) {

    }

    @Override
    public void serverSynchRequestAccepted(PeerID clientPeer, String dataAccessorName, Integer lastTimestamp, UniqueIdentifier fsmID) {

    }

    @Override
    public void serverSynchRequestDenied(PeerID clientPeer, String dataAccessorName, Integer lastTimestamp, UniqueIdentifier fsmID, DataSynchServerFSM.SynchRequestAnswer synchRequestAnswer) {

    }

    @Override
    public void serverSynchSuccess(PeerID clientPeer, String dataAccessorName, UniqueIdentifier fsmID) {

    }

    @Override
    public void serverSynchError(PeerID clientPeer, String dataAccessorName, UniqueIdentifier fsmID, SynchError synchError) {

    }

    @Override
    public void serverSynchTimeout(PeerID clientPeer, String dataAccessorName, UniqueIdentifier fsmID) {

    }

    void stop() {
        // todo use
        sequentialTaskExecutor.stopAndWaitForFinalization();
    }
}
