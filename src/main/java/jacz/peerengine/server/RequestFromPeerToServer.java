package jacz.peerengine.server;

import jacz.peerengine.PeerID;

import java.io.Serializable;
import java.util.Collection;

/**
 * Request that a PeerServer is able to process. These requests are created by peer clients connected to a server,
 * to require an action from the server
 */
public class RequestFromPeerToServer implements Serializable {

    public enum RequestType {
        // search for connected friends in the server
        SEARCH_FRIENDS,
        PING
    }

    RequestType requestType;

    /**
     * Channel assigned for receiving answers to this request
     */
    byte channel;

    /**
     * List of peers for friend search
     */
    Collection<PeerID> friendPeerIDs;


    public static RequestFromPeerToServer friendSearchRequest(byte channel, Collection<PeerID> friendPeerIDs) {
        RequestFromPeerToServer requestFromPeerToServer = new RequestFromPeerToServer();
        requestFromPeerToServer.requestType = RequestType.SEARCH_FRIENDS;
        requestFromPeerToServer.channel = channel;
        requestFromPeerToServer.friendPeerIDs = friendPeerIDs;
        return requestFromPeerToServer;
    }

    public static RequestFromPeerToServer pingRequest(byte channel) {
        RequestFromPeerToServer requestFromPeerToServer = new RequestFromPeerToServer();
        requestFromPeerToServer.requestType = RequestType.PING;
        requestFromPeerToServer.channel = channel;
        return requestFromPeerToServer;
    }
}