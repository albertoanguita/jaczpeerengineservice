package jacz.peerengineservice.util.datatransfer;

import jacz.util.io.object_serialization.VersionedObject;
import jacz.util.io.object_serialization.VersionedObjectSerializer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Statistics for generic transfers
 */
public abstract class TransferStatistics implements VersionedObject {

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

    public TransferStatistics(byte[] data) {
        VersionedObjectSerializer.deserializeVersionedObject(this, data);
    }


    public void reset() {
        creationDate = new Date();
        transferredSize = 0l;
        transferTime = 0L;
        transferSessions = 0L;
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
    public Map<String, Object> serialize() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("creationDate", creationDate);
        attributes.put("transferredSize", transferredSize);
        attributes.put("transferTime", transferTime);
        attributes.put("transferSessions", transferSessions);
        return attributes;
    }

    @Override
    public void deserialize(String version, Map<String, Object> attributes) throws RuntimeException {
        creationDate = (Date) attributes.get("creationDate");
        transferredSize = (long) attributes.get("transferredSize");
        transferTime = (long) attributes.get("transferTime");
        transferSessions = (long) attributes.get("transferSessions");
        if (creationDate == null) {
            // no field can be null -> error
            throw new RuntimeException();
        }
    }

    @Override
    public void errorDeserializing(String version, Map<String, Object> attributes) {
        // todo notify client and reset
    }
}
