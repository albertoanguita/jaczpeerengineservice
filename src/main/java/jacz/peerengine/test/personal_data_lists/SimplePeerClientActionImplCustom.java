package jacz.peerengine.test.personal_data_lists;

import jacz.peerengine.test.SimplePeerClientActionImpl;

/**
 * Simple peer action for custom fsm
 */
public class SimplePeerClientActionImplCustom extends SimplePeerClientActionImpl {

//    private BasicListContainer basicListContainer;
//
//    private ListSynchronizer listSynchronizer;
//
//    private ForeignPeerDataAction foreignPeerDataAction;
//
//    public SimplePeerClientActionImplCustom(ForeignPeerDataAction foreignPeerDataAction) {
//        this.foreignPeerDataAction = foreignPeerDataAction;
//    }
//
//    public void setBasicListContainer(BasicListContainer basicListContainer) {
//        this.basicListContainer = basicListContainer;
//    }
//
//    public void setListSynchronizer(ListSynchronizer listSynchronizer) {
//        this.listSynchronizer = listSynchronizer;
//    }
//
//    @Override
//    public void newPeerConnected(PeerID peerID, ConnectionStatus status) {
//        super.newPeerConnected(peerID, status);
//        Map<String, ListAccessor> readingLists = new HashMap<String, ListAccessor>(0);
//        Map<String, ListAccessor> writingLists = new HashMap<String, ListAccessor>(1);
//        writingLists.put(SimplePersonalData.getListName(), new SimplePersonalData(peerID, "", SimplePersonalData.State.UNDEFINED, "", foreignPeerDataAction));
//        basicListContainer.addPeer(peerID, readingLists, writingLists);
//        listSynchronizer.synchronizeList(peerID, SimplePersonalData.getListName(), 0, 2, 5000);
//    }
//
//    @Override
//    public void newObjectMessage(PeerID peerID, Object message) {
//        if (message instanceof ModifiedPersonalDataNotification) {
//            listSynchronizer.synchronizeList(peerID, SimplePersonalData.getListName(), 0, 2, 5000);
//        } else {
//            super.newObjectMessage(peerID, message);
//        }
//    }
}
