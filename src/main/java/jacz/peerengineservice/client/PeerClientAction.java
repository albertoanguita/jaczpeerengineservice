package jacz.peerengineservice.client;

import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.connection.State;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.peerengineservice.util.datatransfer.DownloadsManager;
import jacz.peerengineservice.util.datatransfer.UploadsManager;
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

    void unrecognizedMessageFromServer(State state);

    void tryingToConnectToServer(State state);

    void connectionToServerEstablished(State state);

    void registrationRequired(State state);

    void localServerUnreachable(State state);

    void unableToConnectToServer(State state);

    void disconnectedFromServer(State state);

    void failedToRefreshServerConnection(State state);

    void tryingToRegisterWithServer(State state);

    void registrationSuccessful(State state);

    void alreadyRegistered(State state);

    void localServerOpen(int port, State state);

    void localServerClosed(int port, State state);

    void undefinedOwnInetAddress();

    void peerCouldNotConnectToUs(Exception e, IP4Port ip4Port);

    void localServerError(Exception e);

    void periodicDownloadsNotification(DownloadsManager downloadsManager);

    void periodicUploadsNotification(UploadsManager uploadsManager);

    void stop();
}
