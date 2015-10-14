package jacz.peerengineservice.util.datatransfer.slave;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.datatransfer.GlobalUploadStatistics;
import jacz.peerengineservice.util.datatransfer.PeerBasedStatistics;
import jacz.util.identifier.UniqueIdentifier;

/**
 * This class provides information about an active upload (but no control over it)
 */
public class UploadManager {

    private final SlaveResourceStreamer slaveResourceStreamer;

    private final String store;

    private final PeerID requestingPeer;

    private final UploadSessionStatistics uploadSessionStatistics;

    public UploadManager(SlaveResourceStreamer slaveResourceStreamer, GlobalUploadStatistics globalUploadStatistics, PeerBasedStatistics peerBasedStatistics) {
        this.slaveResourceStreamer = slaveResourceStreamer;
        store = slaveResourceStreamer.getResourceRequest().getStoreName();
        requestingPeer = slaveResourceStreamer.getResourceRequest().getRequestingPeer();
        uploadSessionStatistics = new UploadSessionStatistics(requestingPeer, globalUploadStatistics, peerBasedStatistics);
    }

    public UniqueIdentifier getId() {
        return slaveResourceStreamer.getId();
    }

    public String getStore() {
        return store;
    }

    public PeerID getRequestingPeer() {
        return requestingPeer;
    }

    public UploadSessionStatistics getUploadSessionStatistics() {
        return uploadSessionStatistics;
    }

    /**
     * Not to invoke from outside the peer engine!!!
     */
    public void stop() {
        // external stop issued by the resource streaming manager
        slaveResourceStreamer.die(true);
    }
}
