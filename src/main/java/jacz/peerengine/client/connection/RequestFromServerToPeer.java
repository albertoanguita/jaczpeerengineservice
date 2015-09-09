package jacz.peerengine.client.connection;

import java.io.Serializable;

/**
 *
 */
public class RequestFromServerToPeer implements Serializable {

    public enum RequestType {
        // ping to avoid connection timeout with server
        PING
    }

    RequestType requestType;

    public static RequestFromServerToPeer pingRequest() {
        RequestFromServerToPeer requestFromServerToPeer = new RequestFromServerToPeer();
        requestFromServerToPeer.requestType = RequestType.PING;
        return requestFromServerToPeer;
    }
}