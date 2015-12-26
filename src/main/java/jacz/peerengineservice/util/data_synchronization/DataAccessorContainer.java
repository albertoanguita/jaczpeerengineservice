package jacz.peerengineservice.util.data_synchronization;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.UnavailablePeerException;

/**
 * Container of data accessors
 */
public interface DataAccessorContainer {

    void peerConnected(PeerID peerID);

    void peerDisconnected(PeerID peerID);

    DataAccessor getAccessorForTransmitting(PeerID peerID, String dataAccessorName) throws AccessorNotFoundException, ServerBusyException;

//    DataAccessor getAccessorForReceiving(PeerID peerID, String dataAccessorName) throws AccessorNotFoundException;
}
