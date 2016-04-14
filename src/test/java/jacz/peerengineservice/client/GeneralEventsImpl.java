package jacz.peerengineservice.client;

import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.PeerRelationship;

/**
 * Created by Alberto on 13/04/2016.
 */
public class GeneralEventsImpl implements GeneralEvents {

    public void init() {
    }

    @Override
    public void peerAddedAsFriend(PeerId peerId) {
        System.out.println("peer added as friend: " + formatPeer(peerId));
    }

    @Override
    public void peerRemovedAsFriend(PeerId peerId) {
        System.out.println("peer removed as friend: " + formatPeer(peerId));
    }

    @Override
    public void peerAddedAsBlocked(PeerId peerId) {
        System.out.println("peer added as blocked: " + formatPeer(peerId));
    }

    @Override
    public void peerRemovedAsBlocked(PeerId peerId) {
        System.out.println("peer removed as blocked: " + formatPeer(peerId));
    }

    @Override
    public void newPeerConnected(PeerId peerId, PeerRelationship peerRelationship) {
        System.out.println("New peer connected: " + formatPeer(peerId) + ", " + peerRelationship);
    }

    @Override
    public void modifiedPeerRelationship(PeerId peerId, PeerRelationship peerRelationship, boolean connected) {
        System.out.println("Modified peer relationship: " + formatPeer(peerId) + ", " + peerRelationship + ", " + connected);
    }

    @Override
    public void newObjectMessage(PeerId peerId, Object message) {
        System.out.println("New object message from " + formatPeer(peerId) + ": " + message);
    }

    @Override
    public void newPeerNick(PeerId peerId, String nick) {
        System.out.println("Peer " + formatPeer(peerId) + " changed his nick to " + nick);
    }

    @Override
    public void peerDisconnected(PeerId peerId, CommError error) {
        System.out.println("Peer disconnected (" + formatPeer(peerId) + "). Error = " + error);
    }

    @Override
    public void stop() {
        System.out.println("Stop");
    }

    private String formatPeer(PeerId peerId) {
        return "{" + peerId.toString().substring(40) + "}";
    }

}
