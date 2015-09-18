package jacz.peerengineservice.util.datatransfer;

import jacz.peerengineservice.PeerID;
import jacz.util.io.object_serialization.VersionedObject;
import jacz.util.io.object_serialization.VersionedObjectSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Statistics referring to the transfers with other peers
 */
public class PeerStatistics implements VersionedObject {

    public static class OnePeerStatistics {

        public final TransferStatistics downloads;

        public final TransferStatistics uploads;

        public OnePeerStatistics() {
            downloads = new TransferStatistics() {
                @Override
                public String getCurrentVersion() {
                    return "1.0";
                }
            };
            uploads = new TransferStatistics() {
                @Override
                public String getCurrentVersion() {
                    return "1.0";
                }
            };
        }
    }

    private Map<PeerID, OnePeerStatistics> statistics;

    public PeerStatistics() {
        reset();
    }

    public PeerStatistics(byte[] data) {
        VersionedObjectSerializer.deserializeVersionedObject(this, data);
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
        return "1.0";
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("statistics", statistics);
        return attributes;
    }

    @Override
    public void deserialize(String version, Map<String, Object> attributes) throws RuntimeException {
        statistics = (Map<PeerID, OnePeerStatistics>) attributes.get("statistics");
    }

    @Override
    public void errorDeserializing(String version, Map<String, Object> attributes) {
        // todo notify client and reset
    }

}
