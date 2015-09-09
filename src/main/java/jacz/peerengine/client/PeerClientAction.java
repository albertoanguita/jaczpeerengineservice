package jacz.peerengine.client;

import jacz.peerengine.PeerID;
import jacz.peerengine.client.connection.ClientConnectionToServerFSM;
import jacz.peerengine.client.connection.State;
import jacz.peerengine.util.ConnectionStatus;
import jacz.peerengine.util.datatransfer.DownloadsManager;
import jacz.peerengine.util.datatransfer.UploadsManager;
import jacz.commengine.communication.CommError;
import jacz.util.network.IP4Port;

/**
 * Actions invoked by the peer engine upon different events. Must be implemented by the user
 */
public interface PeerClientAction {

    public void peerAddedAsFriend(PeerID peerID, PeerRelations peerRelations);

    public void peerRemovedAsFriend(PeerID peerID, PeerRelations peerRelations);

    public void peerAddedAsBlocked(PeerID peerID, PeerRelations peerRelations);

    public void peerRemovedAsBlocked(PeerID peerID, PeerRelations peerRelations);

    public void newPeerConnected(PeerID peerID, ConnectionStatus status);

    public void newObjectMessage(PeerID peerID, Object message);

    public void newChatMessage(PeerID peerID, String message);

    public void peerValidatedUs(PeerID peerID);

    public void peerDisconnected(PeerID peerID, CommError error);

    public void listeningPortModified(int port);

    public void tryingToConnectToServer(PeerServerData peerServerData, State state);

    public void connectionToServerEstablished(PeerServerData peerServerData, State state);

    public void unableToConnectToServer(PeerServerData peerServerData, State state);

    public void serverTookToMuchTimeToAnswerConnectionRequest(PeerServerData peerServerData, State state);

    public void connectionToServerDenied(PeerServerData peerServerData, ClientConnectionToServerFSM.ConnectionFailureReason reason, State state);

    public void connectionToServerTimedOut(PeerServerData peerServerData, State state);

    public void localServerOpen(int port, State state);

    public void localServerClosed(int port, State state);

    public void disconnectedFromServer(boolean expected, PeerServerData peerServerData, State state);

    public void undefinedOwnInetAddress();

    public void peerCouldNotConnectToUs(Exception e, IP4Port ip4Port);

    public void localServerError(Exception e);

    public void periodicDownloadsNotification(DownloadsManager downloadsManager);

    public void periodicUploadsNotification(UploadsManager uploadsManager);
}
