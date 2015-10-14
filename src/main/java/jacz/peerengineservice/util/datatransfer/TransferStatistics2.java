package jacz.peerengineservice.util.datatransfer;

import jacz.peerengineservice.PeerID;
import jacz.util.io.object_serialization.UnrecognizedVersionException;
import jacz.util.io.object_serialization.VersionedObject;
import jacz.util.io.object_serialization.VersionedObjectSerializer;
import jacz.util.io.object_serialization.VersionedSerializationException;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores amount of data sent and received (both global and peer-wise)
 */
public class TransferStatistics2 implements VersionedObject {

    public static class BytesTransferred implements Serializable {

        private long bytesUploaded;

        private long bytesDownloaded;

        public BytesTransferred() {
            bytesUploaded = 0L;
            bytesDownloaded = 0L;
        }

        public synchronized void addUploaded(long bytes) {
            bytesUploaded += bytes;
        }

        public synchronized void addDownloaded(long bytes) {
            bytesDownloaded += bytes;
        }

        public synchronized long getBytesUploaded() {
            return bytesUploaded;
        }

        public synchronized long getBytesDownloaded() {
            return bytesDownloaded;
        }
    }

    private static final String VERSION_0_1 = "0.1";

    private static final String CURRENT_VERSION = VERSION_0_1;

    /**
     * Date when these global statistics were created
     */
    public Date creationDate;

    private BytesTransferred globalTransfer;

    private HashMap<PeerID, BytesTransferred> peerTransfers;

    public TransferStatistics2() {
        reset();
    }

    public TransferStatistics2(byte[] data) throws VersionedSerializationException {
        VersionedObjectSerializer.deserialize(this, data);
    }

    public void reset() {
        creationDate = new Date();
        globalTransfer = new BytesTransferred();
        peerTransfers = new HashMap<>();
    }

    public synchronized void addUploadedBytes(PeerID peerID, long bytes) {
        globalTransfer.addUploaded(bytes);
        checkPeerExists(peerID);
        peerTransfers.get(peerID).addUploaded(bytes);
    }

    public synchronized void addDownloadedBytes(PeerID peerID, long bytes) {
        globalTransfer.addDownloaded(bytes);
        checkPeerExists(peerID);
        peerTransfers.get(peerID).addDownloaded(bytes);
    }

    private void checkPeerExists(PeerID peerID) {
        if (!peerTransfers.containsKey(peerID)) {
            peerTransfers.put(peerID, new BytesTransferred());
        }
    }

    @Override
    public String getCurrentVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public Map<String, Serializable> serialize() {
        Map<String, Serializable> attributes = new HashMap<>();
        attributes.put("creationDate", creationDate);
        attributes.put("globalTransfer", globalTransfer);
        attributes.put("peerTransfers", peerTransfers);
        return attributes;
    }

    @Override
    public void deserialize(Map<String, Object> attributes) {
        creationDate = (Date) attributes.get("creationDate");
        globalTransfer = (BytesTransferred) attributes.get("globalTransfer");
        peerTransfers = (HashMap<PeerID, BytesTransferred>) attributes.get("peerTransfers");
    }

    @Override
    public void deserializeOldVersion(String version, Map<String, Object> attributes) throws UnrecognizedVersionException {
        throw new UnrecognizedVersionException();
    }
}
