package jacz.peerengineservice.test;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.util.data_synchronization.AccessorNotFoundException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.DataAccessorContainer;

import java.util.Map;

/**
 * Created by Alberto on 18/09/2015.
 */
public class TestListContainer implements DataAccessorContainer {

    private final Map<String, DataAccessor> transmittingAccessors;

    private final Map<String, DataAccessor> receivingAccessors;

    public TestListContainer(Map<String, DataAccessor> transmittingAccessors, Map<String, DataAccessor> receivingAccessors) {
        this.transmittingAccessors = transmittingAccessors;
        this.receivingAccessors = receivingAccessors;
    }

    @Override
    public DataAccessor getAccessorForTransmitting(PeerID peerID, String dataAccessorName) throws UnavailablePeerException, AccessorNotFoundException {
        return transmittingAccessors.get(dataAccessorName);
    }

    @Override
    public DataAccessor getAccessorForReceiving(PeerID peerID, String dataAccessorName) throws UnavailablePeerException, AccessorNotFoundException {
        return receivingAccessors.get(dataAccessorName);
    }
}
