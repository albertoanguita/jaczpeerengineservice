package jacz.peerengineservice.util;

import jacz.peerengineservice.client.PeerClient;
import jacz.util.event.notification.NotificationEmitter;
import jacz.util.event.notification.NotificationProcessor;
import jacz.util.event.notification.NotificationReceiver;
import jacz.util.identifier.UniqueIdentifier;
import jacz.peerengineservice.PeerID;
import jacz.util.identifier.UniqueIdentifierFactory;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

/**
 * Class describing the resources (files) shared by all the connected peers (friends), or any other resource provider, for ONE SINGLE resource store.
 * <p/>
 * A resource is identified by its MD5 hash code (or any other string code). The files handled in one object of ForeignStoreShare correspond
 * to one and only one resource store (but handle resources shared across all peers in that store)
 * <p/>
 * The client must keep this updated, adding peers when they connect and updating the shared files.
 * The ResourceStreamingManager will periodically read the values in here to correctly assign peers to active downloads
 * <p/>
 * No additional synchronization measures are needed at this class, as no clashed can be produced
 * <p/>
 * Strategy for not-connected peers: is it correct to add peers that are not connected, and leave them even if they
 * disconnect. Otherwise we would have to add/remove them for each connection/disconnection. This class takes into
 * account the connection status of peers when reporting for resource possession to the resource streaming manager,
 * filtering those peers that are not connected or are waiting for validation.
 * <p/>
 * Client can, if wished, remove disconnected clients, but he will be responsible of adding them again when they
 * reconnect. It is recommended to leave them here, since they are properly filtered.
 */
public class ForeignStoreShare implements NotificationEmitter, NotificationReceiver {

    /**
     * Base notification time delay for emitting updates on changes
     */
    private static final long RECEIVER_MILLIS = 1000;

    /**
     * Factor of notification time delay upon additional changes
     */
    private static final double RECEIVER_TIME_FACTOR = 0.5d;

    /**
     * Max allowed changes before emitting the update
     */
    private static final int RECEIVER_LIMIT = 100;



    private final PeerClient peerClient;

    /**
     * For each file, a list of peers offering it is maintained
     */
    private final Map<String, Set<PeerID>> remoteResources;

    /**
     * For submitting events to subscribers (each time the providers of a resource are modified)
     */
    private final NotificationProcessor notificationProcessor;

    /**
     * Class constructor
     */
    public ForeignStoreShare(PeerClient peerClient) {
        this.peerClient = peerClient;
        remoteResources = new HashMap<>();
        notificationProcessor = new NotificationProcessor();
        peerClient.subscribeToConnectedPeers(UniqueIdentifierFactory.getOneStaticIdentifier(), this, RECEIVER_MILLIS, RECEIVER_TIME_FACTOR, RECEIVER_LIMIT);
    }

    /**
     * Adds one peer as provider of a resource
     *
     * @param resourceID identifier of the resource
     * @param peerID     peer providing the resource
     */
    public synchronized void addResourceProvider(String resourceID, PeerID peerID) {
        if (!remoteResources.containsKey(resourceID)) {
            remoteResources.put(resourceID, new HashSet<PeerID>());
        }
        remoteResources.get(resourceID).add(peerID);
        notificationProcessor.newEvent(resourceID);
    }

    /**
     * A peer is no longer providing a resource
     *
     * @param resourceID identifier of the resource
     * @param peerID     peer no longer providing the resource
     */
    public synchronized void removeResourceProvider(String resourceID, PeerID peerID) {
        if (remoteResources.containsKey(resourceID) && remoteResources.get(resourceID).contains(peerID)) {
            remoteResources.get(resourceID).remove(peerID);
            if (remoteResources.get(resourceID).isEmpty()) {
                remoteResources.remove(resourceID);
            }
            notificationProcessor.newEvent(resourceID);
        }
    }

    /**
     * A peer is completely removed from this foreign store share (usually because the peer is no longer reachable)
     *
     * @param peerID peer to remove
     */
    public synchronized void removeResourceProvider(PeerID peerID) {
        for (String resourceID : new HashSet<>(remoteResources.keySet())) {
            removeResourceProvider(resourceID, peerID);
        }
    }

    /**
     * Retrieves the set of peers that share a specific resource
     *
     * @param resourceID resource id
     * @return set with the peers that provide the specified resource
     */
    public synchronized Set<PeerID> getForeignPeerShares(String resourceID) {
        if (remoteResources.containsKey(resourceID)) {
            Set<PeerID> peerShares = new HashSet<>();
            for (PeerID peerID : remoteResources.get(resourceID)) {
                if (peerClient.getPeerConnectionStatus(peerID) == ConnectionStatus.CORRECT) {
                    peerShares.add(peerID);
                }
            }
            return peerShares;
        } else {
            return new HashSet<>();
        }
    }

    @Override
    public UniqueIdentifier subscribe(UniqueIdentifier receiverID, NotificationReceiver notificationReceiver) throws IllegalArgumentException {
        return notificationProcessor.subscribeReceiver(receiverID, notificationReceiver);
    }

    @Override
    public UniqueIdentifier subscribe(UniqueIdentifier receiverID, NotificationReceiver notificationReceiver, long millis, double timeFactorAtEachEvent, int limit) throws IllegalArgumentException {
        return notificationProcessor.subscribeReceiver(receiverID, notificationReceiver, millis, timeFactorAtEachEvent, limit);
    }

    @Override
    public void unsubscribe(UniqueIdentifier receiverID) {
        notificationProcessor.unsubscribeReceiver(receiverID);
    }

    public void stop() {
        notificationProcessor.stop();
    }

    @Override
    public void newEvent(UniqueIdentifier emitterID, int eventCount, List<List<Object>> nonGroupedMessages, List<Object> groupedMessages) {
        // the connection status of a peer has changed. Notify all resources shared by that peer id
        // first, generate the set of affected peers. Messages are grouped, so they all come in the first list
        Set<PeerID> affectedPeers = new HashSet<>();
        for (Object o : groupedMessages) {
            affectedPeers.add((PeerID) o);
        }
        Map<String, Set<PeerID>> remoteResourcesCopy;
        synchronized (this) {
            remoteResourcesCopy = new HashMap<>(remoteResources);
        }
        for (Map.Entry<String, Set<PeerID>> remoteResource : remoteResourcesCopy.entrySet()) {
            if (CollectionUtils.containsAny(remoteResource.getValue(), affectedPeers)) {
                notificationProcessor.newEvent(remoteResource.getKey());
            }
        }
    }
}
