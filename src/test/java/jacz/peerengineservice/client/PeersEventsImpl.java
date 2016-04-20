package jacz.peerengineservice.client;

import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.connection.peers.PeerInfo;
import jacz.peerengineservice.client.connection.peers.PeersEvents;

/**
 * Created by Alberto on 18/04/2016.
 */
public class PeersEventsImpl implements PeersEvents {

    protected Client client;

    void init(Client client) {
        this.client = client;
    }

    @Override
    public void newPeerConnected(PeerId peerId, PeerInfo peerInfo) {
        System.out.println("New peer connected: " + formatPeer(peerId) + ", " + peerInfo);
    }

    @Override
    public void modifiedPeerRelationship(PeerId peerId, PeerInfo peerInfo) {
        System.out.println("Modified peer relationship: " + formatPeer(peerId) + ", " + peerInfo);
    }

    @Override
    public void newPeerNick(PeerId peerId, String nick, PeerInfo peerInfo) {
        System.out.println("Peer " + formatPeer(peerId) + " changed his nick to " + nick + ", " + peerInfo);
    }

    @Override
    public void peerDisconnected(PeerId peerId, PeerInfo peerInfo, CommError error) {
        System.out.println("Peer disconnected (" + formatPeer(peerId) + "). Error = " + error);
    }

    private String formatPeer(PeerId peerId) {
        return "{" + peerId.toString().substring(40) + "}";
    }
}
