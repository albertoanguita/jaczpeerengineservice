package jacz.peerengineservice.test.list_synch;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.test.SimplePeerClientActionImpl;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.util.concurrency.ThreadUtil;

/**
 * Created with IntelliJ IDEA.
 * User: Alberto
 * Date: 31/05/14
 * Time: 16:36
 * To change this template use File | Settings | File Templates.
 */
public class SimplePeerClientActionSynch extends SimplePeerClientActionImpl {

    private final DataAccessor dataAccessor;

    public SimplePeerClientActionSynch(DataAccessor dataAccessor) {
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
