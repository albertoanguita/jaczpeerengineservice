package jacz.peerengine.test.list_synch;

import jacz.peerengine.PeerID;
import jacz.peerengine.test.SimplePeerClientActionImpl;
import jacz.peerengine.util.ConnectionStatus;
import jacz.peerengine.util.data_synchronization.old.ListAccessor;

/**
 * Created with IntelliJ IDEA.
 * User: Alberto
 * Date: 31/05/14
 * Time: 16:36
 * To change this template use File | Settings | File Templates.
 */
public class SimplePeerClientActionSynch extends SimplePeerClientActionImpl {

    private final ListAccessor listAccessor;

    public SimplePeerClientActionSynch(ListAccessor listAccessor) {
        this.listAccessor = listAccessor;
    }

    @Override
    public void newPeerConnected(PeerID peerID, ConnectionStatus status) {
        super.newPeerConnected(peerID, status);
        client.getPeerClient().getListSynchronizer().synchronizeList(peerID, "list0", 2, 10000, new ListSynchProgress(client.getPeerClientData().getOwnPeerID(), peerID, "list0", true, listAccessor));
//        client.getPeerClient().getListSynchronizer().synchronizeElement(peerID, "list0", "a", 3, 10000, new ListSynchProgress(client.getPeerClientData().getOwnPeerID(), peerID, "list0", true, listAccessor));
    }
}
