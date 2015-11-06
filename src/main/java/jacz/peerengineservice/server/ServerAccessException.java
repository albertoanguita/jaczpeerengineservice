package jacz.peerengineservice.server;

/**
 * Created by Alberto on 06/11/2015.
 */
public class ServerAccessException extends Exception {

    public final int code;

    public ServerAccessException(String message, int code) {
        super(message);
        this.code = code;
    }
}
