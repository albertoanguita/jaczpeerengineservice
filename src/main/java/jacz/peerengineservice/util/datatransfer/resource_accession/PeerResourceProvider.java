package jacz.peerengineservice.util.datatransfer.resource_accession;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.datatransfer.ResourceRequest;
import jacz.peerengineservice.util.datatransfer.ResourceStreamingManager;

/**
 * Peer resource provider, represents a peer in the network which offers resources to us
 */
public class PeerResourceProvider implements ResourceProvider {

    private PeerID ownPeerID;

    private PeerID otherPeerID;

    private ResourceStreamingManager resourceStreamingManager;

    public PeerResourceProvider(PeerID ownPeerID, PeerID otherPeerID, ResourceStreamingManager resourceStreamingManager) {
        this.ownPeerID = ownPeerID;
        this.otherPeerID = otherPeerID;
        this.resourceStreamingManager = resourceStreamingManager;
    }

    @Override
    public String getID() {
        return otherPeerID.toString();
    }

    @Override
    public Type getType() {
        return Type.PEER;
    }

    @Override
    public ResourceLink requestResource(String storeName, String resourceID, short assignedSubchannel, Long preferredIntermediateHashesSize) {
        // send the request to the peer and initialize the resource link
        resourceStreamingManager.write(otherPeerID, ResourceStreamingManager.SLAVE_GRANT_SUBCHANNEL, new ResourceRequest(ownPeerID, storeName, resourceID, assignedSubchannel, preferredIntermediateHashesSize));
        return new PeerResourceLink(resourceStreamingManager, otherPeerID);
    }

    public PeerID getPeerID() {
        return otherPeerID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WebServerResourceProvider that = (WebServerResourceProvider) o;

        if (!getID().equals(that.getID())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getID().hashCode();
    }
}
