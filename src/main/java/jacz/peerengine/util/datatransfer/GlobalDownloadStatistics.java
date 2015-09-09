package jacz.peerengine.util.datatransfer;

import java.util.Map;

/**
 * Global download statistics
 *
 * todo some downloads must not account for global
 * todo do we count lists synch? no
 */
public class GlobalDownloadStatistics extends jacz.peerengine.util.datatransfer.TransferStatistics {

    /**
     * Amount of downloads that have been successfully completed
     */
    public long downloadsCompleted;

    public GlobalDownloadStatistics() {
        reset();
    }

    public GlobalDownloadStatistics(byte[] data) {
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
        return "1.0";
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> attributes = super.serialize();
        attributes.put("downloadsCompleted", downloadsCompleted);
        return attributes;
    }

    @Override
    public void deserialize(String version, Map<String, Object> attributes) throws RuntimeException {
        // no older versions than 1.0
        downloadsCompleted = (long) attributes.get("downloadsCompleted");
        super.deserialize(version, attributes);
    }
}
