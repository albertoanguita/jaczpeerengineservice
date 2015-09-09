package jacz.peerengine.test;

import jacz.peerengine.PeerID;
import jacz.peerengine.client.PeerClientAction;
import jacz.peerengine.client.PeerRelations;
import jacz.peerengine.client.PeerServerData;
import jacz.peerengine.client.connection.ClientConnectionToServerFSM;
import jacz.peerengine.client.connection.State;
import jacz.peerengine.util.ConnectionStatus;
import jacz.peerengine.util.data_synchronization.premade_lists.old.ForeignPeerDataAction;
import jacz.peerengine.util.datatransfer.DownloadsManager;
import jacz.peerengine.util.datatransfer.UploadsManager;
import jacz.commengine.communication.CommError;
import jacz.util.network.IP4Port;

/**
 *
 */
public class SimplePeerClientActionImpl implements PeerClientAction, ForeignPeerDataAction {

    protected jacz.peerengine.test.Client client;

    private String initMessage;

    public void init(PeerID ownPeerID, jacz.peerengine.test.Client client) {
        this.client = client;
        initMessage = ownPeerID.toString() + ": ";
    }

    protected boolean equalsPeerID(int id) {
        return client.getPeerClientData().getOwnPeerID().equals(PeerIDGenerator.peerID(id));
    }

    @Override
    public void peerAddedAsFriend(PeerID peerID, PeerRelations peerRelations) {
        System.out.println(initMessage + "peer added as friend: " + peerID);
    }

    @Override
    public void peerRemovedAsFriend(PeerID peerID, PeerRelations peerRelations) {
        System.out.println(initMessage + "peer removed as friend: " + peerID);
    }

    @Override
    public void peerAddedAsBlocked(PeerID peerID, PeerRelations peerRelations) {
        System.out.println(initMessage + "peer added as blocked: " + peerID);
    }

    @Override
    public void peerRemovedAsBlocked(PeerID peerID, PeerRelations peerRelations) {
        System.out.println(initMessage + "peer removed as blocked: " + peerID);
    }

    @Override
    public void newPeerConnected(PeerID peerID, ConnectionStatus status) {
        System.out.println(initMessage + "New peer connected: " + peerID + ", " + status);
    }

    @Override
    public void newObjectMessage(PeerID peerID, Object message) {
        System.out.println(initMessage + "New object message from " + peerID + ": " + message);
    }

    @Override
    public void newChatMessage(PeerID peerID, String message) {
        System.out.println(initMessage + "New chat message from " + peerID + ": " + message);
    }

    @Override
    public void peerValidatedUs(PeerID peerID) {
        System.out.println("Peer " + peerID + " has validated us, connection status is now " + client.getPeerClient().getPeerConnectionStatus(peerID));
    }

    @Override
    public void peerDisconnected(PeerID peerID, CommError error) {
        System.out.println(initMessage + "Peer disconnected (" + peerID + "). Error = " + error);
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
    public void connectionToServerEstablished(jacz.peerengine.client.PeerServerData peerServerData, State state) {
        System.out.println(initMessage + "Connected to server. State: " + state);
    }

    @Override
    public void unableToConnectToServer(jacz.peerengine.client.PeerServerData peerServerData, State state) {
        System.out.println(initMessage + "Unable to connect to server. State: " + state);
    }

    @Override
    public void serverTookToMuchTimeToAnswerConnectionRequest(jacz.peerengine.client.PeerServerData peerServerData, State state) {
        System.out.println(initMessage + "Server took too much time to answer. State: " + state);
    }

    @Override
    public void connectionToServerTimedOut(jacz.peerengine.client.PeerServerData peerServerData, State state) {
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
    public void connectionToServerDenied(jacz.peerengine.client.PeerServerData peerServerData, ClientConnectionToServerFSM.ConnectionFailureReason reason, State state) {
        System.out.println(initMessage + "Connection to server denied. " + reason + ". State: " + state);
    }

    @Override
    public void disconnectedFromServer(boolean expected, jacz.peerengine.client.PeerServerData peerServerData, State state) {
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

    @Override
    public void newPeerNick(PeerID peerID, String nick) {
        System.out.println("Peer " + peerID + " changed his nick to " + nick);
    }
}
