package jacz.peerengineservice.test;

import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.GeneralEvents;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.util.ConnectionStatus;

/**
 * Created by Alberto on 10/12/2015.
 */
public class GeneralEventsImpl implements GeneralEvents {

    protected jacz.peerengineservice.test.Client client;

    private String initMessage;

    public void init(PeerID ownPeerID, jacz.peerengineservice.test.Client client) {
        this.client = client;
        initMessage = formatPeer(ownPeerID) + ": ";
    }

    protected boolean equalsPeerID(int id) {
        return client.getPeerClient().getOwnPeerID().equals(PeerIDGenerator.peerID(id));
    }

    @Override
    public void peerAddedAsFriend(PeerID peerID, PeerRelations peerRelations) {
        System.out.println(initMessage + "peer added as friend: " + formatPeer(peerID));
    }

    @Override
    public void peerRemovedAsFriend(PeerID peerID, PeerRelations peerRelations) {
        System.out.println(initMessage + "peer removed as friend: " + formatPeer(peerID));
    }

    @Override
    public void peerAddedAsBlocked(PeerID peerID, PeerRelations peerRelations) {
        System.out.println(initMessage + "peer added as blocked: " + formatPeer(peerID));
    }

    @Override
    public void peerRemovedAsBlocked(PeerID peerID, PeerRelations peerRelations) {
        System.out.println(initMessage + "peer removed as blocked: " + formatPeer(peerID));
    }

    @Override
    public void newPeerConnected(PeerID peerID, ConnectionStatus status) {
        System.out.println(initMessage + "New peer connected: " + formatPeer(peerID) + ", " + status);
    }

    @Override
    public void newObjectMessage(PeerID peerID, Object message) {
        System.out.println(initMessage + "New object message from " + formatPeer(peerID) + ": " + message);
    }

    @Override
    public void newPeerNick(PeerID peerID, String nick) {
        System.out.println("Peer " + formatPeer(peerID) + " changed his nick to " + nick);
    }

    @Override
    public void peerValidatedUs(PeerID peerID) {
        System.out.println("Peer " + formatPeer(peerID) + " has validated us, connection status is now " + client.getPeerClient().getPeerConnectionStatus(peerID));
    }

    @Override
    public void peerDisconnected(PeerID peerID, CommError error) {
        System.out.println(initMessage + "Peer disconnected (" + formatPeer(peerID) + "). Error = " + error);
    }

    @Override
    public void stop() {
        System.out.println(initMessage + "Stop");
    }

    private String formatPeer(PeerID peerID) {
        return "{" + peerID.toString().substring(40) + "}";
    }

}
