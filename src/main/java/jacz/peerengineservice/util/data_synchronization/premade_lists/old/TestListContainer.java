package jacz.peerengineservice.util.data_synchronization.premade_lists.old;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.old.ListAccessor;
import jacz.peerengineservice.util.data_synchronization.old.ListContainer;
import jacz.peerengineservice.util.data_synchronization.old.ListNotFoundException;

import java.util.Map;

/**
 * A list container for tests
 */
public class TestListContainer implements ListContainer {

    private final Map<String, ListAccessor> transmittingLists;

    private final Map<String, ListAccessor> receivingLists;

    public TestListContainer(Map<String, ListAccessor> transmittingLists, Map<String, ListAccessor> receivingLists) {
        this.transmittingLists = transmittingLists;
        this.receivingLists = receivingLists;
    }

    @Override
    public ListAccessor getListForTransmitting(PeerID peerID, String list) throws ListNotFoundException {
        return transmittingLists.get(list);
    }

    @Override
    public ListAccessor getListForReceiving(PeerID peerID, String list) throws ListNotFoundException {
        return receivingLists.get(list);
    }
}
