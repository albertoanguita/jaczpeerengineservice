package jacz.peerengineservice.util.datatransfer;

import jacz.util.io.object_serialization.VersionedSerializationException;

/**
 * Global upload statistics
 */
public class GlobalUploadStatistics extends TransferStatistics {

    private static final String VERSION_0_1 = "0.1";

    public GlobalUploadStatistics() {
        reset();
    }

    public GlobalUploadStatistics(byte[] data) throws VersionedSerializationException {
        super(data);
    }

    @Override
    public String getCurrentVersion() {
        return VERSION_0_1;
    }
}
