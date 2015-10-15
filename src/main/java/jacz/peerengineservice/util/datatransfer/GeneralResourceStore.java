package jacz.peerengineservice.util.datatransfer;

import jacz.peerengineservice.PeerID;

/**
 * This interface contains a method for handling resource requests at any store. It can be used in addition to the
 * normal resource stores, to handle requests to stores that have not been specifically registered
 */
public interface GeneralResourceStore {

    /**
     * Request for a new resource
     *
     * @param resourceStore resource store of the request
     * @param peerID        requesting peer id
     * @param resourceID    requested resource
     * @return the response to this request
     */
    ResourceStoreResponse requestResource(String resourceStore, PeerID peerID, String resourceID);
}
