package jacz.peerengineservice.util.datatransfer;

/**
 * Global upload statistics
 */
public class GlobalUploadStatistics extends TransferStatistics {

    public GlobalUploadStatistics() {
        reset();
    }

    public GlobalUploadStatistics(byte[] data) {
        super(data);
    }

    @Override
    public String getCurrentVersion() {
        return "1.0";
    }
}
