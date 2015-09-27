package jacz.peerengineservice.util.datatransfer;

import jacz.peerengineservice.PeerID;
import jacz.util.io.object_serialization.VersionedObject;
import jacz.util.io.object_serialization.VersionedObjectSerializer;
import jacz.util.io.object_serialization.VersionedSerializationException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Statistics referring to the transfers with other peers
 */
public class PeerStatistics implements VersionedObject {

    public static class OnePeerStatistics implements Serializable {

        public final TransferStatistics downloads;

        public final TransferStatistics uploads;

        public OnePeerStatistics() {
            downloads = new TransferStatistics() {};
            uploads = new TransferStatistics() {};
        }
    }

    private static final String VERSION_0_1 = "0.1";

    private HashMap<PeerID, OnePeerStatistics> statistics;

    public PeerStatistics() {
        reset();
    }

    public PeerStatistics(byte[] data) throws VersionedSerializationException {
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
        return VERSION_0_1;
    }

    @Override
    public Map<String, Serializable> serialize() {
        Map<String, Serializable> attributes = new HashMap<>();
        attributes.put("statistics", statistics);
        return attributes;
    }

    @Override
    public void deserialize(String version, Map<String, Object> attributes) throws RuntimeException, VersionedSerializationException {
        statistics = (HashMap<PeerID, OnePeerStatistics>) attributes.get("statistics");
    }
}
