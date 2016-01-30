package jacz.peerengineservice.util.datatransfer;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClient;
import jacz.util.date_time.SpeedRegistry;
import jacz.util.io.serialization.*;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores amount of data sent and received (both global and peer-wise)
 */
public class TransferStatistics implements VersionedObject {

    public static class BytesTransferred implements VersionedObject {

        private static final String VERSION_0_1 = "0.1";

        private static final String CURRENT_VERSION = VERSION_0_1;

        private final static long SPEED_MILLIS_MEASURE = 3000L;

        private final static long SPEED_TIME_STORED = 1800000L;

        private final static long SPEED_MONITOR_FREQUENCY = 3000L;

        private long bytesUploaded;

        private long bytesDownloaded;

        private transient SpeedRegistry uploadSpeed;

        private transient SpeedRegistry downloadSpeed;

        public BytesTransferred() {
            this.bytesUploaded = 0L;
            this.bytesDownloaded = 0L;
            initSpeedRegistries();
        }

        public BytesTransferred(byte[] data) throws VersionedSerializationException {
            this(data, new MutableOffset());
        }

        public BytesTransferred(byte[] data, MutableOffset offset) throws VersionedSerializationException {
            VersionedObjectSerializer.deserialize(this, data, offset);
        }

        private void initSpeedRegistries() {
            uploadSpeed = new SpeedRegistry(SPEED_MILLIS_MEASURE, SPEED_TIME_STORED, SPEED_MONITOR_FREQUENCY);
            downloadSpeed = new SpeedRegistry(SPEED_MILLIS_MEASURE, SPEED_TIME_STORED, SPEED_MONITOR_FREQUENCY);
        }

        public synchronized void addUploaded(long bytes) {
            bytesUploaded += bytes;
            uploadSpeed.addProgress(bytes);
        }

        public synchronized void addDownloaded(long bytes) {
            bytesDownloaded += bytes;
            downloadSpeed.addProgress(bytes);
        }

        public synchronized long getBytesUploaded() {
            return bytesUploaded;
        }

        public synchronized long getBytesDownloaded() {
            return bytesDownloaded;
        }

        public synchronized Double[] getUploadSpeedRegistry() {
            return uploadSpeed.getRegistry();
        }

        public synchronized Double[] getDownloadSpeedRegistry() {
            return downloadSpeed.getRegistry();
        }

        public synchronized void stop() {
            uploadSpeed.stop();
            downloadSpeed.stop();
        }

        @Override
        public String toString() {
            return "BytesTransferred{" +
                    "bytesUploaded=" + bytesUploaded +
                    ", bytesDownloaded=" + bytesDownloaded +
                    ", uploadSpeed=" + Arrays.toString(uploadSpeed.getRegistry()) +
                    ", downloadSpeed=" + Arrays.toString(downloadSpeed.getRegistry()) +
                    '}';
        }

        @Override
        public VersionStack getCurrentVersion() {
            return new VersionStack(CURRENT_VERSION);
        }

        @Override
        public Map<String, Serializable> serialize() {
            Map<String, Serializable> attributes = new HashMap<>();
            attributes.put("bytesUploaded", bytesUploaded);
            attributes.put("bytesDownloaded", bytesDownloaded);
            return attributes;
        }

        @Override
        public void deserialize(String version, Map<String, Object> attributes, VersionStack parentVersions) throws UnrecognizedVersionException {
            if (version.equals(CURRENT_VERSION)) {
                bytesUploaded = (long) attributes.get("bytesUploaded");
                bytesDownloaded = (long) attributes.get("bytesDownloaded");
                initSpeedRegistries();
            } else {
                throw new UnrecognizedVersionException();
            }
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

    public TransferStatistics() {
        reset();
    }

    public TransferStatistics(String path, String... backupPaths) throws VersionedSerializationException, IOException {
        VersionedObjectSerializer.deserialize(this, path, backupPaths);
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

    public synchronized void stop() {
        if (globalTransfer != null) {
            globalTransfer.stop();
        }
        if (peerTransfers != null) {
            for (BytesTransferred peerTransfer : peerTransfers.values()) {
                peerTransfer.stop();
            }
        }
    }

    @Override
    public String toString() {
        return "TransferStatistics{" +
                "creationDate=" + creationDate +
                ", globalTransfer=" + globalTransfer +
                ", peerTransfers=" + peerTransfers +
                '}';
    }

    @Override
    public VersionStack getCurrentVersion() {
        return new VersionStack(CURRENT_VERSION);
    }

    @Override
    public Map<String, Serializable> serialize() {
        Map<String, Serializable> attributes = new HashMap<>();
        attributes.put("creationDate", creationDate);
        try {
            attributes.put("globalTransfer", VersionedObjectSerializer.serialize(globalTransfer));
        } catch (NotSerializableException e) {
            PeerClient.reportError("Could not serialize the globalTransfer object", globalTransfer);
        }
        FragmentedByteArray fragmentedByteArray = new FragmentedByteArray();
        for (Map.Entry<PeerID, BytesTransferred> entry : peerTransfers.entrySet()) {
            try {
                fragmentedByteArray.add(Serializer.serialize(entry.getKey().toByteArray()), VersionedObjectSerializer.serialize(entry.getValue()));
            } catch (NotSerializableException e) {
                PeerClient.reportError("Could not serialize a peerTransfers entry", entry);
            }
        }
        attributes.put("peerTransfers", fragmentedByteArray.generateArray());
        return attributes;
    }

    @Override
    public void deserialize(String version, Map<String, Object> attributes, VersionStack parentVersions) throws UnrecognizedVersionException {
        if (version.equals(CURRENT_VERSION)) {
            creationDate = (Date) attributes.get("creationDate");
            try {
                globalTransfer = new BytesTransferred((byte[]) attributes.get("globalTransfer"));
            } catch (VersionedSerializationException e) {
                throw new RuntimeException();
            }
            peerTransfers = new HashMap<>();
            byte[] peerTransfersData = (byte[]) attributes.get("peerTransfers");
            MutableOffset offset = new MutableOffset();
            while (offset.value() < peerTransfersData.length) {
                PeerID peerID = new PeerID(Serializer.deserializeBytes(peerTransfersData, offset));
                try {
                    BytesTransferred bytesTransferred = new BytesTransferred(peerTransfersData, offset);
                    peerTransfers.put(peerID, bytesTransferred);
                } catch (VersionedSerializationException e) {
                    stop();
                    throw new RuntimeException();
                }
            }
        } else {
            throw new UnrecognizedVersionException();
        }
    }
}
