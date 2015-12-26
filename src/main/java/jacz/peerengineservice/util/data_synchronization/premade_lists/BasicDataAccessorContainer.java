package jacz.peerengineservice.util.data_synchronization.premade_lists;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.AccessorNotFoundException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.DataAccessorContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple implementation of the DataAccessorContainer interface. Peers are added or removed dynamically,
 * but accessors must be given at peer addition. Some transmitting accessors can be given at construction,
 * so every peer has them always.
 * todo remove
 */
//public class BasicDataAccessorContainer implements DataAccessorContainer {
//
//    /**
//     * Named-indexed set of accessors for a single peer
//     */
//    private class PeerAccessors {
//
//        private PeerAccessors(Map<String, DataAccessor> transmittingAccessors, Map<String, DataAccessor> receivingAccessors) {
//            this.transmittingAccessors = transmittingAccessors;
//            this.receivingAccessors = receivingAccessors;
//        }
//
//        private Map<String, DataAccessor> transmittingAccessors;
//
//        private Map<String, DataAccessor> receivingAccessors;
//    }
//
//    public interface PeerAccessorsFactory {
//
//        PeerAccessors buildPeerAccessors(PeerID peerID);
//    }
//
//    private final PeerAccessorsFactory peerAccessorsFactory;
//
//    /**
//     * Peer-indexed accessors
//     */
//    private Map<PeerID, PeerAccessors> dataAccessors;
//
//    /**
//     * Transmitting accessors shared by all peers (must be given at construction time)
//     */
//    private final Map<String, DataAccessor> sharedTransmittingAccessors;
//
//    /**
//     * Class constructor. No shared transmitting accessors will be used
//     */
//    public BasicDataAccessorContainer(PeerAccessorsFactory peerAccessorsFactory) {
//        this(peerAccessorsFactory, new HashMap<String, DataAccessor>());
//    }
//
//    /**
//     * Class constructor with shared transmitting accessors
//     */
//    public BasicDataAccessorContainer(PeerAccessorsFactory peerAccessorsFactory, Map<String, DataAccessor> sharedTransmittingAccessors) {
//        this.peerAccessorsFactory = peerAccessorsFactory;
//        dataAccessors = new HashMap<>();
//        this.sharedTransmittingAccessors = new HashMap<>(sharedTransmittingAccessors);
//    }
//
//    /**
//     * Adds a new peer
//     *
//     * @param peerID                new peer
//     * @param transmittingAccessors transmitting accessors for the new peer
//     * @param receivingAccessors    receiving accessors for the new peer
//     */
//    private synchronized void addPeer(PeerID peerID, Map<String, DataAccessor> transmittingAccessors, Map<String, DataAccessor> receivingAccessors) {
//        PeerAccessors peerAccessors = new PeerAccessors(transmittingAccessors, receivingAccessors);
//        peerAccessors.transmittingAccessors.putAll(sharedTransmittingAccessors);
//        dataAccessors.put(peerID, peerAccessors);
//    }
//
//    @Override
//    public synchronized void peerConnected(PeerID peerID) {
//        PeerAccessors peerAccessors = peerAccessorsFactory.buildPeerAccessors(peerID);
//        addPeer(peerID, peerAccessors.transmittingAccessors, peerAccessors.receivingAccessors);
//    }
//
//    @Override
//    public synchronized void peerDisconnected(PeerID peerID) {
//        dataAccessors.remove(peerID);
//    }
//
//    @Override
//    public synchronized DataAccessor getAccessorForTransmitting(PeerID peerID, String accessor) throws AccessorNotFoundException {
//        PeerAccessors peerAccessors = getPeerAccessors(peerID);
//        try {
//            return peerAccessors.transmittingAccessors.get(accessor);
//        } catch (NullPointerException e) {
//            throw new AccessorNotFoundException();
//        }
//    }
//
//    @Override
//    public synchronized DataAccessor getAccessorForReceiving(PeerID peerID, String accessor) throws AccessorNotFoundException {
//        PeerAccessors peerAccessors = getPeerAccessors(peerID);
//        try {
//            return peerAccessors.receivingAccessors.get(accessor);
//        } catch (NullPointerException e) {
//            throw new AccessorNotFoundException();
//        }
//    }
//
//    private PeerAccessors getPeerAccessors(PeerID peerID) throws AccessorNotFoundException {
//        if (!dataAccessors.containsKey(peerID)) {
//            return dataAccessors.get(peerID);
//        } else {
//            throw new AccessorNotFoundException();
//        }
//    }
//}
