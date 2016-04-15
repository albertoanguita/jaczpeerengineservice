package jacz.peerengineservice.client.listsynch;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.GeneralEventsImpl;
import jacz.peerengineservice.client.PeerIdGenerator;
import jacz.peerengineservice.util.PeerRelationship;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;

/**
 * Created by Alberto on 10/12/2015.
 */
public class GeneralEventsSynch extends GeneralEventsImpl {

    private final DataAccessor dataAccessor;

    private ListSynchProgress clientProgress;

    public GeneralEventsSynch(DataAccessor dataAccessor) {
        this.dataAccessor = dataAccessor;
    }

    public ListSynchProgress getClientProgress() {
        return clientProgress;
    }

    @Override
    public void newPeerConnected(PeerId peerId, PeerRelationship peerRelationship) {
        super.newPeerConnected(peerId, peerRelationship);
        try {
            if (peerId.equals(PeerIdGenerator.peerID(2))) {
                clientProgress = new ListSynchProgress(peerId, "list0", true);
                client.getPeerClient().getDataSynchronizer().synchronizeData(peerId, dataAccessor, 10000, clientProgress);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
