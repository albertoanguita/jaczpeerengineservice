package jacz.peerengineservice.client;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.data_synchronization.AccessorNotFoundException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.DataAccessorContainer;

import java.util.Map;

/**
 * Created by Alberto on 15/04/2016.
 */
public class ListContainer implements DataAccessorContainer {

    private final Map<String, DataAccessor> transmittingAccessors;

    private final Map<String, DataAccessor> receivingAccessors;

    public ListContainer(Map<String, DataAccessor> transmittingAccessors, Map<String, DataAccessor> receivingAccessors) {
        this.transmittingAccessors = transmittingAccessors;
        this.receivingAccessors = receivingAccessors;
    }

    @Override
    public void peerConnected(PeerId peerId) {

    }

    @Override
    public void peerDisconnected(PeerId peerId) {

    }

    @Override
    public DataAccessor getAccessorForTransmitting(PeerId peerId, String dataAccessorName) throws AccessorNotFoundException {
        return transmittingAccessors.get(dataAccessorName);
    }

//    @Override
//    public DataAccessor getAccessorForReceiving(PeerId peerID, String dataAccessorName) throws AccessorNotFoundException {
//        return receivingAccessors.get(dataAccessorName);
//    }
}
