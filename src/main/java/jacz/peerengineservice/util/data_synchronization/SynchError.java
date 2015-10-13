package jacz.peerengineservice.util.data_synchronization;

/**
 * Different types of errors that can happen during synchronization
 */
public class SynchError {

    public static enum Type {
        PEER_CLIENT_BUSY,
        DISCONNECTED,
        SERVER_BUSY,
        ERROR_IN_PROTOCOL,
        UNKNOWN_ACCESSOR,
        REQUEST_DENIED,
        NO_PERMISSION,
        SERVER_ERROR,
        TRANSMISSION_ERROR,
        DATA_ACCESS_ERROR,
        UNDEFINED
    }

    public final Type type;

    public final String details;

    public SynchError(Type type, String details) {
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
