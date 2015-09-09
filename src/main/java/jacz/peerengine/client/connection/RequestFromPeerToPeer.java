package jacz.peerengine.client.connection;

import java.io.Serializable;

/**
 * The different types of request that a PeerClient can send to another PeerClient. This request carry a RequestType
 * value, indicating the type of the request, and some additional attributes for additional information of the
 * request
 */
public class RequestFromPeerToPeer implements Serializable {

    public enum RequestType {
        // to submit a personalized message to the other end. The message itself will be handled by the client
        OBJECT_MESSAGE,
        // to send a new chat message
        CHAT,
        // defined by the client of this layer (the actual request comes in the "value" field, as a String)
        CUSTOM
    }

    RequestType requestType;

    /**
     * Channel assigned for receiving answers to this request
     */
    Byte channel;

    /**
     * CHAT: the String message of the chat
     * CUSTOM: the type of customFSM that must be created to answer this request
     */
    String value;

    /**
     * OBJECT_MESSAGE: the message being transferred
     */
    Serializable customObject;

    private RequestFromPeerToPeer(RequestType requestType, Byte channel, String value, Serializable customObject) {
        this.requestType = requestType;
        this.channel = channel;
        this.value = value;
        this.customObject = customObject;
    }

    static RequestFromPeerToPeer generateObjectMessageRequest(Serializable customObject) {
        return new RequestFromPeerToPeer(RequestType.OBJECT_MESSAGE, null, null, customObject);
    }

    static RequestFromPeerToPeer generateChatRequest(String message) {
        return new RequestFromPeerToPeer(RequestType.CHAT, null, message, null);
    }

    public static RequestFromPeerToPeer generateCustomRequest(byte assignedChannel, String serverFSMName) {
        return new RequestFromPeerToPeer(RequestType.CUSTOM, assignedChannel, serverFSMName, null);
    }
}
