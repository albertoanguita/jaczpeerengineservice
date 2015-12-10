package jacz.peerengineservice.test.list_synch;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.test.GeneralEventsImpl;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.util.concurrency.ThreadUtil;

/**
 * Created by Alberto on 10/12/2015.
 */
public class GeneralEventsSynch extends GeneralEventsImpl {

    private final DataAccessor dataAccessor;

    public GeneralEventsSynch(DataAccessor dataAccessor) {
        this.dataAccessor = dataAccessor;
    }

    @Override
    public void newPeerConnected(PeerID peerID, ConnectionStatus status) {
        super.newPeerConnected(peerID, status);
        client.getPeerClient().getDataSynchronizer().synchronizeData(peerID, "list0", 10000, new ListSynchProgress(peerID, "list0", true, dataAccessor));
        ThreadUtil.safeSleep(5000);
        client.getPeerClient().getDataSynchronizer().synchronizeData(peerID, "list0", 10000, new ListSynchProgress(peerID, "list0", true, dataAccessor));
    }
}
