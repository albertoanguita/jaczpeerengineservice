package jacz.peerengine.util.datatransfer.resource_accession;

/**
 * This interface contain methods to implement by a resource provider. A resource provider is any entity that can
 * provide us a resource (e.g. a peer, a direct link server, etc)
 */
public interface ResourceProvider {

    public static enum Type {
        PEER,
        WEB
    }

    /**
     * This method retrieves the String identifier of the provider. Each provider must have a unique ID
     *
     * @return the identifier of the provider
     */
    public String getID();

    /**
     * Returns the type of the resource provider
     *
     * @return the type of the resource provider
     */
    public Type getType();

    /**
     * This method makes a request to the provider for a new resource. The method returns a resourceID for identifying the
     * request. The request is totally asynchronous, meaning that the response will come later.
     *
     * @param storeName          name of the store holding the resource
     * @param resourceID         the resourceID code of the required resource
     * @param assignedSubchannel subchannel where the corresponding answer (and all subsequent incoming data) must go
     *                           @param preferredIntermediateHashesSize preferred size for intermediate hashes (null if no hashes required)
     * @return the resource link for the requested resource (the request is still not validated though)
     */
    public ResourceLink requestResource(String storeName, String resourceID, short assignedSubchannel, Long preferredIntermediateHashesSize);
}
