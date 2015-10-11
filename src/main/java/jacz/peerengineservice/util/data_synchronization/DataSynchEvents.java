package jacz.peerengineservice.util.data_synchronization;

import jacz.peerengineservice.PeerID;
import jacz.util.identifier.UniqueIdentifier;

/**
 * Events related to data synchronization that are notified to clients
 */
public interface DataSynchEvents {

    // todo

    void clientSynchRequestInitiated(PeerID serverPeer, String dataAccessorName, long timeout, UniqueIdentifier fsmID);

    void clientSynchRequestFailedToInitiate(PeerID serverPeer, String dataAccessorName, long timeout, SynchError synchError);

    void clientSynchRequestDenied(PeerID serverPeer, String dataAccessorName, long timeout, SynchError synchError);

    void clientSynchSuccess(PeerID serverPeer, String dataAccessorName, UniqueIdentifier fsmID);

    void clientSynchError(PeerID serverPeer, String dataAccessorName, UniqueIdentifier fsmID, SynchError synchError);

    void clientSynchTimeout(PeerID serverPeer, String dataAccessorName, UniqueIdentifier fsmID);

    void serverSynchRequestAccepted(PeerID clientPeer, String dataAccessorName, Integer lastTimestamp, UniqueIdentifier fsmID);

    void serverSynchRequestDenied(PeerID clientPeer, String dataAccessorName, Integer lastTimestamp, UniqueIdentifier fsmID, DataSynchServerFSM.SynchRequestAnswer synchRequestAnswer);

    void serverSynchSuccess(PeerID clientPeer, String dataAccessorName, UniqueIdentifier fsmID);

    void serverSynchError(PeerID clientPeer, String dataAccessorName, UniqueIdentifier fsmID, SynchError synchError);

    void serverSynchTimeout(PeerID clientPeer, String dataAccessorName, UniqueIdentifier fsmID);
}
