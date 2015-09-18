package jacz.peerengineservice.util.data_synchronization;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.UnavailablePeerException;

/**
 * Container of data accessors
 */
public interface DataAccessorContainer {

    DataAccessor getAccessorForTransmitting(PeerID peerID, String dataAccessorName) throws UnavailablePeerException, AccessorNotFoundException;

    DataAccessor getAccessorForReceiving(PeerID peerID, String dataAccessorName) throws UnavailablePeerException, AccessorNotFoundException;
}
