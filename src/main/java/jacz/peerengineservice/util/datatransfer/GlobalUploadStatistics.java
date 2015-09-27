package jacz.peerengineservice.util.datatransfer;

import jacz.util.io.object_serialization.VersionedSerializationException;

import java.util.Map;

/**
 * Global upload statistics
 */
public class GlobalUploadStatistics extends TransferStatistics {

    public GlobalUploadStatistics() {
        reset();
    }

    public GlobalUploadStatistics(byte[] data) throws VersionedSerializationException {
        super(data);
    }
}
