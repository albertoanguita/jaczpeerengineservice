package jacz.peerengineservice.test;

import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClientAction;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeerServerData;
import jacz.peerengineservice.client.connection.ClientConnectionToServerFSM;
import jacz.peerengineservice.client.connection.State;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.peerengineservice.util.datatransfer.DownloadsManager;
import jacz.peerengineservice.util.datatransfer.UploadsManager;
import jacz.util.network.IP4Port;

/**
 *
 */
public class SimplePeerClientActionImpl implements PeerClientAction {

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
    public void listeningPortModified(int port) {
        System.out.println(initMessage + "Listening port modified: " + port);
    }

    @Override
    public void tryingToConnectToServer(PeerServerData peerServerData, State state) {
        System.out.println(initMessage + "Trying to connect to server. State: " + state);
    }

    @Override
    public void connectionToServerEstablished(jacz.peerengineservice.client.PeerServerData peerServerData, State state) {
        System.out.println(initMessage + "Connected to server. State: " + state);
    }

    @Override
    public void unableToConnectToServer(jacz.peerengineservice.client.PeerServerData peerServerData, State state) {
        System.out.println(initMessage + "Unable to connect to server. State: " + state);
    }

    @Override
    public void serverTookToMuchTimeToAnswerConnectionRequest(jacz.peerengineservice.client.PeerServerData peerServerData, State state) {
        System.out.println(initMessage + "Server took too much time to answer. State: " + state);
    }

    @Override
    public void connectionToServerTimedOut(jacz.peerengineservice.client.PeerServerData peerServerData, State state) {
        System.out.println(initMessage + "Connection to server timed out. State: " + state);
    }

    @Override
    public void localServerOpen(int port, State state) {
        System.out.println(initMessage + "Local server open on port " + port + ". State: " + state);
    }

    @Override
    public void localServerClosed(int port, State state) {
        System.out.println(initMessage + "Local server closed on port " + port + ". State: " + state);
    }

    @Override
    public void connectionToServerDenied(jacz.peerengineservice.client.PeerServerData peerServerData, ClientConnectionToServerFSM.ConnectionFailureReason reason, State state) {
        System.out.println(initMessage + "Connection to server denied. " + reason + ". State: " + state);
    }

    @Override
    public void disconnectedFromServer(boolean expected, jacz.peerengineservice.client.PeerServerData peerServerData, State state) {
        System.out.println(initMessage + "Disconnected from server. Expected=" + expected + ". State: " + state);
    }

    @Override
    public void undefinedOwnInetAddress() {
        System.out.println(initMessage + "Inet address not defined");
    }

    @Override
    public void peerCouldNotConnectToUs(Exception e, IP4Port ip4Port) {
        System.out.println(initMessage + "Peer failed to connect to us from " + ip4Port.toString() + ". " + e.getMessage());
    }

    @Override
    public void localServerError(Exception e) {
        System.out.println(initMessage + "Error in the peer connections listener. All connections closed. Error: " + e.getMessage());
    }

    @Override
    public void periodicDownloadsNotification(DownloadsManager downloadsManager) {
        // do nothing
    }

    @Override
    public void periodicUploadsNotification(UploadsManager uploadsManager) {
        // do nothing
    }

    private String formatPeer(PeerID peerID) {
        return "{" + peerID.toString().substring(40) + "}";
    }
}
