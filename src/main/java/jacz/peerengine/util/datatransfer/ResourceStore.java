package jacz.peerengine.util.datatransfer;

import jacz.peerengine.PeerID;

/**
 * This interface contains the methods of a resource store (i.e. a store of resources shared to other peers).
 * The store must simply answer queries from other peers for specific resources (for example files). Resources are
 * identified by a hash value (the implementation of the actual hash relies on the store implementation).
 */
public interface ResourceStore {

    /**
     * Request for a resource from another peer
     *
     * @param peerID     requesting peer id
     * @param resourceID requested resource
     * @return the response to this request
     */
    public ResourceStoreResponse requestResource(PeerID peerID, String resourceID);
}
