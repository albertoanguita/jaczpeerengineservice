package jacz.peerengineservice.util.datatransfer;

/**
 * Read-only interface for different transfer-related configuration options
 */
public interface TransfersConfig {

    Float getMaxDownloadSpeed();

    Float getMaxUploadSpeed();

    double getDownloadPartSelectionAccuracy();
}
