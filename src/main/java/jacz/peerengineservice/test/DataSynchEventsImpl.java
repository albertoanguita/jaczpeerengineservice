package jacz.peerengineservice.test;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.DataSynchEvents;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.util.identifier.UniqueIdentifier;

/**
 * Created by Alberto on 13/10/2015.
 */
public class DataSynchEventsImpl implements DataSynchEvents {

    @Override
    public void clientSynchRequestInitiated(PeerID serverPeer, String dataAccessorName, long timeout, UniqueIdentifier fsmID) {

    }

    @Override
    public void clientSynchRequestFailedToInitiate(PeerID serverPeer, String dataAccessorName, long timeout, SynchError synchError) {

    }

    @Override
    public void clientSynchRequestDenied(PeerID serverPeer, String dataAccessorName, UniqueIdentifier fsmID, SynchError synchError) {

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
    public void serverSynchRequestAccepted(PeerID clientPeer, String dataAccessorName, UniqueIdentifier fsmID) {

    }

    @Override
    public void serverSynchRequestDenied(PeerID clientPeer, String dataAccessorName, UniqueIdentifier fsmID, SynchError synchError) {

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
}
