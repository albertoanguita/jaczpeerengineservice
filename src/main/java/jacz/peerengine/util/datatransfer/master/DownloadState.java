package jacz.peerengine.util.datatransfer.master;

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
