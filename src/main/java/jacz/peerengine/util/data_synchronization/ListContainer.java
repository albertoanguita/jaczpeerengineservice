package jacz.peerengine.util.data_synchronization;

import jacz.peerengine.PeerID;
import jacz.peerengine.util.data_synchronization.old.ListNotFoundException;

/**
 * This interface contains the methods to retrieve individual lists (both for reading and writing)
 * <p/>
 * All lists are stored in a list contained. Each time a list is needed for reading or writing, it is
 * requested to the list contained.
 * <p/>
 * There is one and only one list container, and it is responsibility of the user to correctly
 * provide the lists
 */
public interface ListContainer {

    public ListAccessor getListForTransmitting(PeerID peerID, String list) throws ListNotFoundException;

    public ListAccessor getListForReceiving(PeerID peerID, String list) throws ListNotFoundException;
}
