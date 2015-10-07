package jacz.peerengineservice.util.datatransfer;

import jacz.util.io.object_serialization.UnrecognizedVersionException;
import jacz.util.io.object_serialization.VersionedObject;
import jacz.util.io.object_serialization.VersionedObjectSerializer;
import jacz.util.io.object_serialization.VersionedSerializationException;
import jacz.util.lists.Duple;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Statistics for generic transfers
 */
public class TransferStatistics implements VersionedObject {

    private static final String VERSION_0_1 = "transfer_0.1";

    private static final String CURRENT_VERSION = VERSION_0_1;

    private static final Character VERSION_SEPARATOR = '@';

    /**
     * Date when these global statistics were created
     */
    public Date creationDate;

    /**
     * Total bytes transferred
     */
    public long transferredSize;

    /**
     * Time in millis that there have been active transfers
     */
    public long transferTime;

    /**
     * Amount of transferred sessions that have taken place (sessions initiated)
     */
    public long transferSessions;

    public TransferStatistics() {
        reset();
    }

    public TransferStatistics(byte[] data) throws VersionedSerializationException {
        VersionedObjectSerializer.deserialize(this, data);
    }


    public void reset() {
        creationDate = new Date();
        transferredSize = 0l;
        transferTime = 0L;
        transferSessions = 0L;
    }

    protected String appendSuperVersion(String version) {
        return version + VERSION_SEPARATOR + CURRENT_VERSION;
    }

    protected String extractChildVersion(String version) throws IllegalArgumentException {
        return parseVersion(version).element1;
    }

    protected String extractSuperVersion(String version) throws IllegalArgumentException {
        return parseVersion(version).element2;
    }

    private Duple<String, String> parseVersion(String version) throws IllegalArgumentException {
        int indexOfSeparator = version.lastIndexOf(VERSION_SEPARATOR);
        if (indexOfSeparator >= 0 && indexOfSeparator == version.length() - 1) {
            return new Duple<>(version.substring(0, indexOfSeparator), version.substring(indexOfSeparator + 1));
        } else {
            throw new IllegalArgumentException("Version does not include the separator character, or it is at the end: " + VERSION_SEPARATOR);
        }
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public synchronized long getTransferredSize() {
        return transferredSize;
    }

    public synchronized long getTransferTime() {
        return transferTime;
    }

    public synchronized long getTransferSessions() {
        return transferSessions;
    }

    public synchronized void addTransferSize(long size) {
        transferredSize += size;
    }

    public synchronized void startTransferSession() {
        transferSessions++;
    }

    public synchronized void endTransferSession(long sessionMillis) {
        transferTime += sessionMillis;
    }

    @Override
    public String getCurrentVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public Map<String, Serializable> serialize() {
        Map<String, Serializable> attributes = new HashMap<>();
        attributes.put("creationDate", creationDate);
        attributes.put("transferredSize", transferredSize);
        attributes.put("transferTime", transferTime);
        attributes.put("transferSessions", transferSessions);
        return attributes;
    }

    @Override
    public void deserialize(String version, Map<String, Object> attributes) throws UnrecognizedVersionException {
        if (version.equals(CURRENT_VERSION)) {
            creationDate = (Date) attributes.get("creationDate");
            transferredSize = (long) attributes.get("transferredSize");
            transferTime = (long) attributes.get("transferTime");
            transferSessions = (long) attributes.get("transferSessions");
            if (creationDate == null) {
                // no field can be null -> error
                throw new RuntimeException();
            }
        } else {
            throw new UnrecognizedVersionException();
        }
    }
}
