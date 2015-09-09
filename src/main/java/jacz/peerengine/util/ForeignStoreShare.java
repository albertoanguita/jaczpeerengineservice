package jacz.peerengine.util;

import jacz.util.event.notification.NotificationEmitter;
import jacz.util.event.notification.NotificationProcessor;
import jacz.util.event.notification.NotificationReceiver;
import jacz.util.identifier.UniqueIdentifier;
import jacz.peerengine.PeerID;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
 */
public class ForeignStoreShare implements NotificationEmitter {

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
    public ForeignStoreShare() {
        remoteResources = new HashMap<String, Set<PeerID>>();
        notificationProcessor = new NotificationProcessor();
    }

    /**
     * Adds one peer as provider of a resource
     *
     * @param resourceID identifier of the resource
     * @param peerID     peer providing the resource
     */
    public synchronized void addResourceProvider(String resourceID, PeerID peerID) {
        if (!remoteResources.containsKey(resourceID)) {
            Set<PeerID> providers = new HashSet<PeerID>();
            remoteResources.put(resourceID, providers);
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
        for (String resourceID : remoteResources.keySet()) {
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
            return new HashSet<PeerID>(remoteResources.get(resourceID));
        } else {
            return new HashSet<PeerID>();
        }
    }

    @Override
    public UniqueIdentifier subscribe(UniqueIdentifier receiverID, NotificationReceiver notificationReceiver, boolean groupEvents) throws IllegalArgumentException {
        return notificationProcessor.subscribeReceiver(receiverID, notificationReceiver, groupEvents);
    }

    @Override
    public UniqueIdentifier subscribe(UniqueIdentifier receiverID, NotificationReceiver notificationReceiver, boolean groupEvents, long millis, double timeFactorAtEachEvent, int limit) throws IllegalArgumentException {
        return notificationProcessor.subscribeReceiver(receiverID, notificationReceiver, groupEvents, millis, timeFactorAtEachEvent, limit);
    }

    @Override
    public void unsubscribe(UniqueIdentifier receiverID) {
        notificationProcessor.unsubscribeReceiver(receiverID);
    }
}
