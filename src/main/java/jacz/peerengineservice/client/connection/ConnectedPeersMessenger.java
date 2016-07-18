package jacz.peerengineservice.client.connection;

import jacz.peerengineservice.PeerId;
import org.aanguita.jtcpserver.channel.ChannelConnectionPoint;

import java.io.Serializable;

/**
 * This class allows sending messages to the connected peers. All communications with connected peers should be done through here
 */
public class ConnectedPeersMessenger {


    /**
     * Relation of currently connected peers
     */
    private final ConnectedPeers connectedPeers;

    /**
     * The RequestDispatcher works in a fixed channel
     */
    public final byte requestDispatcherChannel;


    public ConnectedPeersMessenger(ConnectedPeers connectedPeers, byte requestDispatcherChannel) {
        this.connectedPeers = connectedPeers;
        this.requestDispatcherChannel = requestDispatcherChannel;
    }

    /**
     * Sends an object message to a connected peer. If the given peer is not among the list of connected peers, the
     * message will be ignored
     *
     * @param peerId  ID of the peer to which the message is to be sent
     * @param message string message to send
     */
    public long sendObjectMessage(PeerId peerId, byte channel, Serializable message, boolean flush) {
        ChannelConnectionPoint ccp = connectedPeers.getPeerChannelConnectionPoint(peerId);
        if (ccp != null) {
            return ccp.write(channel, message, flush);
        } else {
            return 0L;
        }
    }

    /**
     * Sends an object message to all connected peers
     *
     * @param message string message to send to all connected peers
     */
    public void broadcastObjectMessage(byte channel, Serializable message) {
        for (PeerId peerId : connectedPeers.getConnectedPeers()) {
            sendObjectMessage(peerId, channel, message, true);
        }
    }

    /**
     * Sends an object message to a connected peer. If the given peer is not among the list of connected peers, the
     * message will be ignored
     *
     * @param peerId  ID of the peer to which the message is to be sent
     * @param message string message to send
     */
    public long sendObjectRequest(PeerId peerId, Serializable message) {
        RequestFromPeerToPeer requestFromPeerToPeer = RequestFromPeerToPeer.generateObjectMessageRequest(message);
        return sendObjectMessage(peerId, requestDispatcherChannel, requestFromPeerToPeer, true);
    }

    /**
     * Sends an object message to all connected peers
     *
     * @param message string message to send to all connected peers
     */
    public void broadcastObjectRequest(Serializable message) {
        RequestFromPeerToPeer requestFromPeerToPeer = RequestFromPeerToPeer.generateObjectMessageRequest(message);
        broadcastObjectMessage(requestDispatcherChannel, requestFromPeerToPeer);
    }

    /**
     * Sends an object message to a connected peer. If the given peer is not among the list of connected peers, the
     * message will be ignored
     *
     * @param peerId ID of the peer to which the message is to be sent
     * @param data   data to send
     */
    public long sendDataMessage(PeerId peerId, byte channel, byte[] data, boolean flush) {
        ChannelConnectionPoint ccp = connectedPeers.getPeerChannelConnectionPoint(peerId);
        if (ccp != null) {
            return ccp.write(channel, data, flush);
        } else {
            return 0L;
        }
    }

    public long flush(PeerId peerId) {
        ChannelConnectionPoint ccp = connectedPeers.getPeerChannelConnectionPoint(peerId);
        if (ccp != null) {
            return ccp.flush();
        } else {
            return 0L;
        }
    }
}
