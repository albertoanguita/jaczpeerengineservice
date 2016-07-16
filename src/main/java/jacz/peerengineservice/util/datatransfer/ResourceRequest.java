package jacz.peerengineservice.util.datatransfer;

import jacz.peerengineservice.PeerId;

import java.io.Serializable;

/**
 * The request data for a new resource
 */
public class ResourceRequest implements Serializable {

    private final byte[] requestingPeer;

    private final String storeName;

    private final String resourceID;

    private final short subchannel;

    private final Float priority;

    public ResourceRequest(PeerId requestingPeer, String storeName, String resourceID, short subchannel) {
        this(requestingPeer, storeName, resourceID, subchannel, null);
    }

    public ResourceRequest(PeerId requestingPeer, String storeName, String resourceID, short subchannel, Float priority) {
        this.requestingPeer = requestingPeer.toByteArray();
        this.storeName = storeName;
        this.resourceID = resourceID;
        this.subchannel = subchannel;
        this.priority = priority;
    }

    public PeerId getRequestingPeer() {
        return new PeerId(requestingPeer);
    }

    public String getStoreName() {
        return storeName;
    }

    public String getResourceID() {
        return resourceID;
    }

    public short getSubchannel() {
        return subchannel;
    }

//    public Long getPreferredIntermediateHashesSize() {
//        return preferredIntermediateHashesSize;
//    }

    public Float getPriority() {
        return priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceRequest)) return false;

        ResourceRequest that = (ResourceRequest) o;

        if (!requestingPeer.equals(that.requestingPeer)) return false;
        if (!resourceID.equals(that.resourceID)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = requestingPeer.hashCode();
        result = 31 * result + resourceID.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Resource request: " + requestingPeer + "/" + storeName + "/" + resourceID + "/" + subchannel;
    }
}
