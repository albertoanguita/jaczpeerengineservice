package jacz.peerengineservice.client;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.connection.ClientConnectionToServerFSM;
import jacz.peerengineservice.client.connection.State;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.peerengineservice.util.datatransfer.DownloadsManager;
import jacz.peerengineservice.util.datatransfer.UploadsManager;
import jacz.commengine.communication.CommError;
import jacz.util.network.IP4Port;

/**
 * Actions invoked by the peer engine upon different events. Must be implemented by the user
 */
public interface PeerClientAction {

    void peerAddedAsFriend(PeerID peerID, PeerRelations peerRelations);

    void peerRemovedAsFriend(PeerID peerID, PeerRelations peerRelations);

    void peerAddedAsBlocked(PeerID peerID, PeerRelations peerRelations);

    void peerRemovedAsBlocked(PeerID peerID, PeerRelations peerRelations);

    void newPeerConnected(PeerID peerID, ConnectionStatus status);

    void newObjectMessage(PeerID peerID, Object message);

    void newPeerNick(PeerID peerID, String nick);

    void peerValidatedUs(PeerID peerID);

    void peerDisconnected(PeerID peerID, CommError error);

    void listeningPortModified(int port);

    void tryingToConnectToServer(PeerServerData peerServerData, State state);

    void connectionToServerEstablished(PeerServerData peerServerData, State state);

    void unableToConnectToServer(PeerServerData peerServerData, State state);

    void serverTookTooMuchTimeToAnswerConnectionRequest(PeerServerData peerServerData, State state);

    void connectionToServerDenied(PeerServerData peerServerData, ClientConnectionToServerFSM.ConnectionFailureReason reason, State state);

    void connectionToServerTimedOut(PeerServerData peerServerData, State state);

    void localServerOpen(int port, State state);

    void localServerClosed(int port, State state);

    void disconnectedFromServer(boolean expected, PeerServerData peerServerData, State state);

    void undefinedOwnInetAddress();

    void peerCouldNotConnectToUs(Exception e, IP4Port ip4Port);

    void localServerError(Exception e);

    void periodicDownloadsNotification(DownloadsManager downloadsManager);

    void periodicUploadsNotification(UploadsManager uploadsManager);

    void stop();
}
