package jacz.peerengineservice.test.list_synch;

import jacz.peerengineservice.PeerId;
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
    public void newPeerConnected(PeerId peerId, ConnectionStatus status) {
        super.newPeerConnected(peerId, status);
        try {
            client.getPeerClient().getDataSynchronizer().synchronizeData(peerId, dataAccessor, 10000, new ListSynchProgress(peerId, "list0", true, dataAccessor));
            ThreadUtil.safeSleep(5000);
            client.getPeerClient().getDataSynchronizer().synchronizeData(peerId, dataAccessor, 10000, new ListSynchProgress(peerId, "list0", true, dataAccessor));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
