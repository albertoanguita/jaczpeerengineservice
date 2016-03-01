package jacz.peerengineservice.util.data_synchronization;

import jacz.peerengineservice.PeerId;

/**
 * Container of data accessors
 */
public interface DataAccessorContainer {

    void peerConnected(PeerId peerId);

    void peerDisconnected(PeerId peerId);

    DataAccessor getAccessorForTransmitting(PeerId peerId, String dataAccessorName) throws AccessorNotFoundException, ServerBusyException;

//    DataAccessor getAccessorForReceiving(PeerId peerID, String dataAccessorName) throws AccessorNotFoundException;
}
