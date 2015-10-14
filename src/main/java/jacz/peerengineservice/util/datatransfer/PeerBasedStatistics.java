package jacz.peerengineservice.util.datatransfer;

import jacz.peerengineservice.PeerID;
import jacz.util.io.object_serialization.UnrecognizedVersionException;
import jacz.util.io.object_serialization.VersionedObject;
import jacz.util.io.object_serialization.VersionedObjectSerializer;
import jacz.util.io.object_serialization.VersionedSerializationException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Statistics referring to the transfers with other peers, divided by peer
 */
public class PeerBasedStatistics implements VersionedObject {

    public static class OnePeerStatistics implements Serializable {

        public final TransferStatistics downloads;

        public final TransferStatistics uploads;

        public OnePeerStatistics() {
            downloads = new TransferStatistics() {};
            uploads = new TransferStatistics() {};
        }
    }

    private static final String VERSION_0_1 = "0.1";

    private static final String CURRENT_VERSION = VERSION_0_1;

    private HashMap<PeerID, OnePeerStatistics> statistics;

    public PeerBasedStatistics() {
        reset();
    }

    public PeerBasedStatistics(byte[] data) throws VersionedSerializationException {
        VersionedObjectSerializer.deserialize(this, data);
    }

    public void reset() {
        statistics = new HashMap<>();
    }

    public synchronized void addPeer(PeerID peerID) {
        statistics.put(peerID, new OnePeerStatistics());
    }

    public synchronized void startDownloadSession(PeerID peerID) {
        if (!statistics.containsKey(peerID)) {
            addPeer(peerID);
        }
        statistics.get(peerID).downloads.startTransferSession();
    }

    public synchronized void endDownloadSession(PeerID peerID, long sessionMillis) {
        if (!statistics.containsKey(peerID)) {
            addPeer(peerID);
        }
        statistics.get(peerID).downloads.endTransferSession(sessionMillis);
    }

    public synchronized void addDownloadedSize(PeerID peerID, long size) {
        if (!statistics.containsKey(peerID)) {
            addPeer(peerID);
        }
        statistics.get(peerID).downloads.addTransferSize(size);
    }

    public synchronized void startUploadSession(PeerID peerID) {
        if (!statistics.containsKey(peerID)) {
            addPeer(peerID);
        }
        statistics.get(peerID).uploads.startTransferSession();
    }

    public synchronized void endUploadSession(PeerID peerID, long sessionMillis) {
        if (!statistics.containsKey(peerID)) {
            addPeer(peerID);
        }
        statistics.get(peerID).uploads.endTransferSession(sessionMillis);
    }

    public synchronized void addUploadedSize(PeerID peerID, long size) {
        if (!statistics.containsKey(peerID)) {
            addPeer(peerID);
        }
        statistics.get(peerID).uploads.addTransferSize(size);
    }

    public synchronized OnePeerStatistics getOnePeerStatistics(PeerID peerID) {
        if (!statistics.containsKey(peerID)) {
            addPeer(peerID);
        }
        return statistics.get(peerID);
    }

    @Override
    public String getCurrentVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public Map<String, Serializable> serialize() {
        Map<String, Serializable> attributes = new HashMap<>();
        attributes.put("statistics", statistics);
        return attributes;
    }

    @Override
    public void deserialize(Map<String, Object> attributes) {
        statistics = (HashMap<PeerID, OnePeerStatistics>) attributes.get("statistics");
    }

    @Override
    public void deserializeOldVersion(String version, Map<String, Object> attributes) throws UnrecognizedVersionException {
        throw new UnrecognizedVersionException();
    }
}
