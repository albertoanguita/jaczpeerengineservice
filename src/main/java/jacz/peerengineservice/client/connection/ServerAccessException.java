package jacz.peerengineservice.client.connection;

/**
 * Exception accessing the server
 */
public class ServerAccessException extends Exception {

    public final int code;

    public ServerAccessException(String message, int code) {
        super(message);
        this.code = code;
    }
}
