package jacz.peerengineservice.util.datatransfer;

import jacz.util.io.object_serialization.UnrecognizedVersionException;
import jacz.util.io.object_serialization.VersionedSerializationException;

import java.io.Serializable;
import java.util.Map;

/**
 * Global download statistics
 */
public class GlobalDownloadStatistics extends TransferStatistics {

    private static final String VERSION_0_1 = "0.1";

    private static final String CURRENT_VERSION = VERSION_0_1;

    /**
     * Amount of downloads that have been successfully completed
     */
    public long downloadsCompleted;

    public GlobalDownloadStatistics() {
        reset();
    }

    public GlobalDownloadStatistics(byte[] data) throws VersionedSerializationException {
        super(data);
    }

    public void reset() {
        super.reset();
        downloadsCompleted = 0L;
    }

    public long getDownloadsCompleted() {
        return downloadsCompleted;
    }

    public synchronized void downloadComplete() {
        downloadsCompleted++;
    }

    @Override
    public String getCurrentVersion() {
        return appendSuperVersion(CURRENT_VERSION);
    }

    @Override
    public Map<String, Serializable> serialize() {
        Map<String, Serializable> attributes = super.serialize();
        attributes.put("downloadsCompleted", downloadsCompleted);
        return attributes;
    }

    @Override
    public void deserialize(String version, Map<String, Object> attributes) throws UnrecognizedVersionException {
        // no older versions than 1.0
        String ownVersion = extractChildVersion(version);
        if (ownVersion.equals(CURRENT_VERSION)) {
            downloadsCompleted = (long) attributes.get("downloadsCompleted");
            super.deserialize(extractSuperVersion(version), attributes);
        } else {
            throw new UnrecognizedVersionException();
        }
    }
}
