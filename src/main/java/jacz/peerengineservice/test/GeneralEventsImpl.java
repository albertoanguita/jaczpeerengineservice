package jacz.peerengineservice.test;

import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.GeneralEvents;
import jacz.peerengineservice.client.PeerRelations;

/**
 * Created by Alberto on 10/12/2015.
 */
public class GeneralEventsImpl implements GeneralEvents {

    protected jacz.peerengineservice.test.Client client;

    private String initMessage;

    public void init(PeerId ownPeerId, jacz.peerengineservice.test.Client client) {
        this.client = client;
        initMessage = formatPeer(ownPeerId) + ": ";
    }

    protected boolean equalsPeerID(int id) {
        return client.getPeerClient().getOwnPeerId().equals(PeerIDGenerator.peerID(id));
    }

    @Override
    public void peerAddedAsFriend(PeerId peerId, PeerRelations peerRelations) {
        System.out.println(initMessage + "peer added as friend: " + formatPeer(peerId));
    }

    @Override
    public void peerRemovedAsFriend(PeerId peerId, PeerRelations peerRelations) {
        System.out.println(initMessage + "peer removed as friend: " + formatPeer(peerId));
    }

    @Override
    public void peerAddedAsBlocked(PeerId peerId, PeerRelations peerRelations) {
        System.out.println(initMessage + "peer added as blocked: " + formatPeer(peerId));
    }

    @Override
    public void peerRemovedAsBlocked(PeerId peerId, PeerRelations peerRelations) {
        System.out.println(initMessage + "peer removed as blocked: " + formatPeer(peerId));
    }

    @Override
    public void newPeerConnected(PeerId peerId) {
        System.out.println(initMessage + "New peer connected: " + formatPeer(peerId));
    }

    @Override
    public void newObjectMessage(PeerId peerId, Object message) {
        System.out.println(initMessage + "New object message from " + formatPeer(peerId) + ": " + message);
    }

    @Override
    public void newPeerNick(PeerId peerId, String nick) {
        System.out.println("Peer " + formatPeer(peerId) + " changed his nick to " + nick);
    }

//    @Override
//    public void peerValidatedUs(PeerId peerId) {
//        System.out.println("Peer " + formatPeer(peerId) + " has validated us, connection status is now " + client.getPeerClient().getPeerConnectionStatus(peerId));
//    }

    @Override
    public void peerDisconnected(PeerId peerId, CommError error) {
        System.out.println(initMessage + "Peer disconnected (" + formatPeer(peerId) + "). Error = " + error);
    }

    @Override
    public void stop() {
        System.out.println(initMessage + "Stop");
    }

    private String formatPeer(PeerId peerId) {
        return "{" + peerId.toString().substring(40) + "}";
    }

}
