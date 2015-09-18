package jacz.peerengineservice.util.datatransfer;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is in charge of managing the different resource stores handled by the ResourceStreamingManager. Resource stores
 * are different allocations for shared resources (resources that can be accessed by other peers). The user can
 * dynamically create new stores (he can for example have one store for general sharing, and different stores for
 * specific peer sharing). This eases the handling of resource access from the outside.
 * <p/>
 * This class provides two non-exclusive ways of managing local stores:
 * 1) Registering each local store
 * 2) Setting a general resource store which will receive requests for all stores not registered
 * <p/>
 * User can use either method, or both of them, as he finds more convenient. Any request will be first tried with the registered stores. In case
 * no matching store is found, a second attempt with the general store will be made
 * <p/>
 * Stores are identified by a String value. Stores created by users cannot begin by the prefix
 * PeerClient.OWN_CUSTOM_PREFIX, to avoid clashes with internal packages.
 * <p/>
 * The operations contained in this class are appropriately synchronized, so there is no risk of messing up due to
 * concurrency issues
 */
class LocalShareManager {

    /**
     * Managers for the requests to each individual resource store
     */
    private Map<String, ResourceStore> registeredStores;

    /**
     * Manager for the requests to global resource stores
     */
    private GeneralResourceStore generalResourceStore;

    /**
     * Class constructor
     */
    LocalShareManager() {
        registeredStores = new HashMap<String, ResourceStore>();
        generalResourceStore = null;
    }

    /**
     * Registers a new resource store
     *
     * @param name  name of the new store
     * @param store the resource store
     */
    synchronized void addStore(String name, ResourceStore store) {
        registeredStores.put(name, store);
    }

    /**
     * Sets a new general resource store
     *
     * @param generalResourceStore the new general resource store (null if not used)
     */
    synchronized void setGeneralStore(GeneralResourceStore generalResourceStore) {
        this.generalResourceStore = generalResourceStore;
    }

    /**
     * Retrieves a registered store
     *
     * @param name name of the store
     * @return the requested store, or null if no store with the given name is found
     */
    synchronized ResourceStore getStore(String name) {
        if (registeredStores.containsKey(name)) {
            return registeredStores.get(name);
        } else {
            return null;
        }
    }

    /**
     * Retrieves the general resource store
     *
     * @return the general resource store (or null if it is not set)
     */
    synchronized GeneralResourceStore getGeneralResourceStore() {
        return generalResourceStore;
    }

    /**
     * Removes a registered resource store
     *
     * @param name name of the store to remove
     */
    synchronized void removeStore(String name) {
        registeredStores.remove(name);
    }
}
