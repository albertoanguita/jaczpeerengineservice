package jacz.peerengineservice.util.datatransfer;

import jacz.peerengineservice.util.ForeignStoreShare;
import jacz.util.concurrency.task_executor.ParallelTaskExecutor;
import jacz.util.event.notification.NotificationReceiver;
import jacz.util.id.AlphaNumFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class handles all resources shared to us by other peers. Shared resources are grouped in resource
 * stores (identified by a string). If two peers share the same resource through the same store, we will be able
 * to download it from both peers simultaneously.
 * <p/>
 * This class keeps track of all resource stores created by the other peers, and all resources contained in them. It
 * employs the class ForeignStoreShare to handle the resources shared in just one resource store. It also offers
 * the methods to allow maintaining it properly updated.
 * <p/>
 * We do not need to register stores intended for individual downloads (downloads from one specific peer), since in
 * the download method itself we will specify the target peer. This is only useful for downloads in which we do not
 * specify target peer. In this case the resource streaming manager redirects to this class in order to find which
 * peers are sharing the required resource.
 * <p/>
 * The client is in charge of properly updating all the given ForeignStoreShare objects. The data contained in this
 * class is for internal use of the ResourceStreamingManager only.
 * <p/>
 * This class offers ability for subscribing for receiving updates on changes. The jacz.util event notification api is used for this
 * <p/>
 * Synchronization is done to avoid clashes between the resource streaming manager and the notification receiver handler that invokes newEvent
 */
class ForeignShareManager implements NotificationReceiver {

    /**
     * Information stored per resource store
     */
    private class ForeignPeerShareWithEmitterID {

        private final ForeignStoreShare foreignStoreShare;

        private final String emitterID;

        private ForeignPeerShareWithEmitterID(ForeignStoreShare foreignStoreShare, String emitterID) {
            this.foreignStoreShare = foreignStoreShare;
            this.emitterID = emitterID;
        }
    }

    /**
     * Base notification time delay for emitting updates on changes
     */
    private static final long RECEIVER_MILLIS = 5000;

    /**
     * Factor of notification time delay upon additional changes
     */
    private static final double RECEIVER_TIME_FACTOR = 0.5d;

    /**
     * Max allowed changes before emitting the update
     */
    private static final int RECEIVER_LIMIT = 100;

    /**
     * A table containing the shares for each registered resource stored
     */
    private final Map<String, ForeignPeerShareWithEmitterID> storeShares;

    /**
     * Table with all registered ResourceStores, indexed by their emitter id
     */
    private final Map<String, String> globalEmitterIDs;

    /**
     * Own unique identifier, for receiving event updates from ForeignPeerShares
     */
    private final String receiverID;

    /**
     * The resource streaming manager that owns this resource store manager. We need it to report provider data
     * changes (events)
     */
    private final ResourceStreamingManager resourceStreamingManager;

    /**
     * Indicates if we are still alive. We can only register stores if we are alive. Otherwise all requests are rejected
     */
    private boolean alive;

    /**
     * Class constructor
     *
     * @param resourceStreamingManager the resource streaming manager owning this foreign share manager
     */
    ForeignShareManager(ResourceStreamingManager resourceStreamingManager) {
        storeShares = new HashMap<>();
        globalEmitterIDs = new HashMap<>();
        receiverID = AlphaNumFactory.getStaticId();
        this.resourceStreamingManager = resourceStreamingManager;
        alive = true;
    }

    /**
     * Adds a resource store
     *
     * @param store             name of the resource store
     * @param foreignStoreShare foreign store share representing this store
     */
    synchronized void addStore(String store, ForeignStoreShare foreignStoreShare) {
        // the subscribe call can be synched because it can cause no clash
        if (alive) {
            String shareEmitterID = foreignStoreShare.subscribe(receiverID, this, RECEIVER_MILLIS, RECEIVER_TIME_FACTOR, RECEIVER_LIMIT);
            storeShares.put(store, new ForeignPeerShareWithEmitterID(foreignStoreShare, shareEmitterID));
            globalEmitterIDs.put(shareEmitterID, store);
        }
    }

    /**
     * Retrieves a foreign store share
     *
     * @param store name of the resource store
     * @return foreign store share associated to the given resource store name
     */
    synchronized ForeignStoreShare getResourceProviderShare(String store) {
        return storeShares.containsKey(store) ? storeShares.get(store).foreignStoreShare : null;
    }

    /**
     * Removes a resource store from the registered foreign resource stores, and stops the store share
     *
     * @param store name of the resource store to remove
     */
    void removeStore(String store) {
        // the unsubscribe call must not be synchronized, because it could clash with the newEvent call (it makes an internal synched stop call)
        ForeignStoreShare foreignStoreShare = null;
        synchronized (this) {
            if (storeShares.containsKey(store)) {
                foreignStoreShare = storeShares.get(store).foreignStoreShare;
                String emitterID = storeShares.remove(store).emitterID;
                globalEmitterIDs.remove(emitterID);
            }
        }
        if (foreignStoreShare != null) {
            foreignStoreShare.unsubscribe(receiverID);
            foreignStoreShare.stop();
        }
    }

    /**
     * Removes all registered resource stores and stops all subscriptions. No further store registrations can be made after this call
     */
    void stop() {
        synchronized (this) {
            alive = false;
        }
        for (String store : new ArrayList<>(storeShares.keySet())) {
            removeStore(store);
        }
    }

    @Override
    public void newEvent(final String emitterID, int eventCount, List<List<Object>> nonGroupedMessages, List<Object> groupedMessages) {
        // messages are grouped, so they all come in the first list instead of in individual lists
        // a timer thread invokes this sporadically, but this thread can clash with the unsubscribe method, so it is left un-synchronized
        // and the call to the ResourceStreamingManager is in parallel. Therefore, this newEvent call ALWAYS ends
        String resourceStore = null;
        synchronized (this) {
            if (globalEmitterIDs.containsKey(emitterID)) {
                resourceStore = globalEmitterIDs.get(emitterID);
            }
        }
        if (resourceStore != null) {
            final String finalResourceStore = resourceStore;
            final List<String> affectedResources = new ArrayList<>();
            for (Object message : groupedMessages) {
                affectedResources.add((String) message);
            }
            ParallelTaskExecutor.executeTask(new Runnable() {
                @Override
                public void run() {
                    resourceStreamingManager.reportProvidersShareChanges(finalResourceStore, affectedResources);
                }
            });
        }
    }
}
