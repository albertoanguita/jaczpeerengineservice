package jacz.peerengineservice.util.datatransfer;

import jacz.peerengineservice.PeerId;

/**
 * This interface contains the methods of a resource store (i.e. a store of resources shared to other peers).
 * The store must simply answer queries from other peers for specific resources (for example files). Resources are
 * identified by a hash value (the implementation of the actual hash relies on the store implementation).
 */
public interface ResourceStore {

    /**
     * Request for a resource from another peer
     *
     * @param peerId     requesting peer id
     * @param resourceID requested resource
     * @return the response to this request
     */
    ResourceStoreResponse requestResource(PeerId peerId, String resourceID);
}
