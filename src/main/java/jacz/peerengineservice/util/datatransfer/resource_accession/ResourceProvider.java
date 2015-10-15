package jacz.peerengineservice.util.datatransfer.resource_accession;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.datatransfer.ResourceRequest;
import jacz.peerengineservice.util.datatransfer.ResourceStreamingManager;

/**
 * This interface contain methods to implement by a resource provider. A resource provider is any entity that can
 * provide us a resource (e.g. a peer, a direct link server, etc)
 */
public class ResourceProvider {

//    public static enum Type {
//        PEER,
//        WEB
//    }
//
//    /**
//     * This method retrieves the String identifier of the provider. Each provider must have a unique ID
//     *
//     * @return the identifier of the provider
//     */
//    public String getID();
//
//    /**
//     * Returns the type of the resource provider
//     *
//     * @return the type of the resource provider
//     */
//    public Type getType();
//
//    /**
//     * This method makes a request to the provider for a new resource. The method returns a resourceID for identifying the
//     * request. The request is totally asynchronous, meaning that the response will come later.
//     *
//     * @param storeName          name of the store holding the resource
//     * @param resourceID         the resourceID code of the required resource
//     * @param assignedSubchannel subchannel where the corresponding answer (and all subsequent incoming data) must go
//     * @return the resource link for the requested resource (the request is still not validated though)
//     */
//    public ResourceLink requestResource(String storeName, String resourceID, short assignedSubchannel);


    private PeerID ownPeerID;

    private PeerID otherPeerID;

    private ResourceStreamingManager resourceStreamingManager;

    public ResourceProvider(PeerID ownPeerID, PeerID otherPeerID, ResourceStreamingManager resourceStreamingManager) {
        this.ownPeerID = ownPeerID;
        this.otherPeerID = otherPeerID;
        this.resourceStreamingManager = resourceStreamingManager;
    }

    public ResourceLink requestResource(String storeName, String resourceID, short assignedSubchannel) {
        // send the request to the peer and initialize the resource link
        resourceStreamingManager.write(otherPeerID, ResourceStreamingManager.SLAVE_GRANT_SUBCHANNEL, new ResourceRequest(ownPeerID, storeName, resourceID, assignedSubchannel));
        return new PeerResourceLink(resourceStreamingManager, otherPeerID);
    }

    public PeerID getPeerID() {
        return otherPeerID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceProvider that = (ResourceProvider) o;

        return getPeerID().equals(that.getPeerID());
    }

    @Override
    public int hashCode() {
        return getPeerID().hashCode();
    }
}
