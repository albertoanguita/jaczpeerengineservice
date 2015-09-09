package jacz.peerengine.util.datatransfer;

import jacz.peerengine.PeerID;

import java.io.Serializable;

/**
 * The request data for a new resource
 */
public class ResourceRequest implements Serializable {

    private PeerID requestingPeer;

    private String storeName;

    private String resourceID;

    private short subchannel;

    private final Long preferredIntermediateHashesSize;

    private Float priority;

    public ResourceRequest(PeerID requestingPeer, String storeName, String resourceID, short subchannel, Long preferredIntermediateHashesSize) {
        this(requestingPeer, storeName, resourceID, subchannel, preferredIntermediateHashesSize, null);
    }

    public ResourceRequest(PeerID requestingPeer, String storeName, String resourceID, short subchannel, Long preferredIntermediateHashesSize, Float priority) {
        this.requestingPeer = requestingPeer;
        this.storeName = storeName;
        this.resourceID = resourceID;
        this.subchannel = subchannel;
        this.preferredIntermediateHashesSize = preferredIntermediateHashesSize;
        this.priority = priority;
    }

    public PeerID getRequestingPeer() {
        return requestingPeer;
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

    public Long getPreferredIntermediateHashesSize() {
        return preferredIntermediateHashesSize;
    }

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
