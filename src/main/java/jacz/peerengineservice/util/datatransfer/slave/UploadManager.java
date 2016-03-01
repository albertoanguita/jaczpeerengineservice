package jacz.peerengineservice.util.datatransfer.slave;

import jacz.peerengineservice.PeerId;
import jacz.util.identifier.UniqueIdentifier;

/**
 * This class provides information about an active upload (but no control over it)
 */
public class UploadManager {

    private final SlaveResourceStreamer slaveResourceStreamer;

    private final String store;

    private final String resourceID;

    private final PeerId requestingPeer;

    public UploadManager(SlaveResourceStreamer slaveResourceStreamer) {
        this.slaveResourceStreamer = slaveResourceStreamer;
        store = slaveResourceStreamer.getResourceRequest().getStoreName();
        resourceID = slaveResourceStreamer.getResourceRequest().getResourceID();
        requestingPeer = slaveResourceStreamer.getResourceRequest().getRequestingPeer();
    }

    public UniqueIdentifier getId() {
        return slaveResourceStreamer.getId();
    }

    public String getStore() {
        return store;
    }

    public String getResourceID() {
        return resourceID;
    }

    public PeerId getRequestingPeer() {
        return requestingPeer;
    }

    /**
     * Not to invoke from outside the peer engine!!!
     */
    public void stop() {
        // external stop issued by the resource streaming manager
        slaveResourceStreamer.die(true);
    }
}
