package jacz.peerengineservice.util.datatransfer.resource_accession;

/**
 * Peer resource provider, represents a peer in the network which offers resources to us
 */
public class PeerResourceProvider /*implements ResourceProvider*/ {
//
//    private PeerId ownPeerID;
//
//    private PeerId otherPeerID;
//
//    private ResourceStreamingManager resourceStreamingManager;
//
//    public PeerResourceProvider(PeerId ownPeerID, PeerId otherPeerID, ResourceStreamingManager resourceStreamingManager) {
//        this.ownPeerID = ownPeerID;
//        this.otherPeerID = otherPeerID;
//        this.resourceStreamingManager = resourceStreamingManager;
//    }
//
//    @Override
//    public String getID() {
//        return otherPeerID.toString();
//    }
//
//    @Override
//    public Type getType() {
//        return Type.PEER;
//    }
//
//    @Override
//    public ResourceLink requestResource(String storeName, String resourceID, short assignedSubchannel) {
//        // send the request to the peer and initialize the resource link
//        resourceStreamingManager.write(otherPeerID, ResourceStreamingManager.SLAVE_GRANT_SUBCHANNEL, new ResourceRequest(ownPeerID, storeName, resourceID, assignedSubchannel));
//        return new PeerResourceLink(resourceStreamingManager, otherPeerID);
//    }
//
//    public PeerId getPeerId() {
//        return otherPeerID;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//
//        WebServerResourceProvider that = (WebServerResourceProvider) o;
//
//        if (!getID().equals(that.getID())) return false;
//
//        return true;
//    }
//
//    @Override
//    public int hashCode() {
//        return getID().hashCode();
//    }
}
