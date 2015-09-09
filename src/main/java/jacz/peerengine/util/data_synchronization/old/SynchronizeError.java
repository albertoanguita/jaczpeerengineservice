package jacz.peerengine.util.data_synchronization.old;

/**
 * Different types of errors that can happen during synchronization
 */
public class SynchronizeError {

    public static enum Type {
        PEER_CLIENT_BUSY,
        DISCONNECTED,
        SERVER_BUSY,
        ERROR_IN_PROTOCOL,
        UNKNOWN_LIST,
        ELEMENT_NOT_FOUND,
        INVALID_LEVEL,
        REQUEST_DENIED,
        DIFFERENT_LISTS_CONFIG,
        ELEMENT_CHANGED_IN_SERVER,
        DATA_TRANSFER_FAILED,
        DATA_ACCESS_ERROR,
        UNDEFINED
    }

    public final Type type;

    public final String details;

    public SynchronizeError(Type type, String details) {
        this.type = type;
        this.details = details;
    }

    @Override
    public String toString() {
        if (details == null) {
            return type.toString();
        } else {
            return type.toString() + ": " + details;
        }
    }
}
