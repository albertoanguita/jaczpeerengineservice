package jacz.peerengineservice.client.listsynch;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerIdGenerator;
import jacz.peerengineservice.client.PeersEventsImpl;
import jacz.peerengineservice.client.connection.peers.PeerInfo;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;

/**
 * Created by Alberto on 10/12/2015.
 */
public class PeersEventsSynch extends PeersEventsImpl {

    private final DataAccessor dataAccessor;

    private ListSynchProgress clientProgress;

    public PeersEventsSynch(DataAccessor dataAccessor) {
        this.dataAccessor = dataAccessor;
    }

    public ListSynchProgress getClientProgress() {
        return clientProgress;
    }

    @Override
    public void newPeerConnected(PeerId peerId, PeerInfo peerInfo) {
        super.newPeerConnected(peerId, peerInfo);
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
