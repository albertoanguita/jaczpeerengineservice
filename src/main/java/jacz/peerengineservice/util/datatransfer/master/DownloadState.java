package jacz.peerengineservice.util.datatransfer.master;

/**
 * The possible states of a download
 */
public enum DownloadState {
    RUNNING,
    PAUSED,
    STOPPED,
    COMPLETED,
    CANCELLED
}
