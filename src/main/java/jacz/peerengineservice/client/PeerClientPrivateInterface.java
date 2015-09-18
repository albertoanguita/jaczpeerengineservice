package jacz.peerengineservice.client;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.connection.ClientConnectionToServerFSM;
import jacz.peerengineservice.client.connection.RequestFromPeerToPeer;
import jacz.peerengineservice.client.connection.State;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.peerengineservice.util.datatransfer.DownloadsManager;
import jacz.peerengineservice.util.datatransfer.UploadsManager;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.communication.CommError;
import jacz.util.network.IP4Port;

/**
 * This class handles the messages that the PeerClientConnectionManager send to its corresponding PeerClient. It is
 * done so several important methods in the PeerClient can be made non-public (new client, free channels, etc).
 * <p/>
 * This class contains the code that handles new connections with other clients, as well as disconnections or errors.
 * The freeing of channels also goes through this class, since that event is captured by the
 * PeerClientConnectionManager.
 */
public class PeerClientPrivateInterface {


    /**
     * PeerClient to which this class gives access
     */
    private final PeerClient peerClient;

    private State.ConnectionToServerState connectionToServerState;

    /**
     * Information about the server to which we are trying to connect or connected (null otherwise)
     */
    private PeerServerData peerServerData;

    private State.LocalServerConnectionsState localServerConnectionsState;

    /**
     * Port at which we listen to connections from other peers, if the local server is open (-1 otherwise)
     */
    private int port;

    /**
     * Class constructor. Non-public so this class cannot be used outside this package
     *
     * @param peerClient the PeerClient for which this PeerClientPrivateInterface works
     */
    PeerClientPrivateInterface(PeerClient peerClient) {
        this.peerClient = peerClient;
        connectionToServerState = State.ConnectionToServerState.DISCONNECTED;
        localServerConnectionsState = State.LocalServerConnectionsState.CLOSED;
    }

    private synchronized State buildState() {
        return new State(connectionToServerState, peerServerData, localServerConnectionsState, port);
    }

    public synchronized State getState() {
        return buildState();
    }

    private void updateConnectionToServerInfo(State.ConnectionToServerState connectionToServerStatus, PeerServerData peerServerData) {
        this.connectionToServerState = connectionToServerStatus;
        if (connectionToServerStatus == State.ConnectionToServerState.CONNECTED || connectionToServerStatus == State.ConnectionToServerState.ONGOING_CONNECTION) {
            this.peerServerData = peerServerData;
        } else {
            this.peerServerData = null;
        }
    }

    private void updateLocalServerInfo(State.LocalServerConnectionsState localServerConnectionsState, int port) {
        this.localServerConnectionsState = localServerConnectionsState;
        if (localServerConnectionsState == State.LocalServerConnectionsState.OPEN) {
            this.port = port;
        } else {
            this.port = -1;
        }
    }

    public synchronized void listeningPortModified(int port) {
        peerClient.listeningPortModified(port);
    }

    public synchronized void tryingToConnectToServer(PeerServerData peerServerData, State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus, peerServerData);
        peerClient.tryingToConnectToServer(peerServerData, buildState());
    }

    public synchronized void connectionToServerEstablished(PeerServerData peerServerData, State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus, peerServerData);
        peerClient.connectionToServerEstablished(peerServerData, buildState());
    }

    public synchronized void unableToConnectToServer(PeerServerData peerServerData, State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus, peerServerData);
        peerClient.unableToConnectToServer(peerServerData, buildState());
    }

    public synchronized void serverTookToMuchTimeToAnswerConnectionRequest(PeerServerData peerServerData, State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus, peerServerData);
        peerClient.serverTookToMuchTimeToAnswerConnectionRequest(peerServerData, buildState());
    }

    public synchronized void connectionToServerDenied(PeerServerData peerServerData, ClientConnectionToServerFSM.ConnectionFailureReason reason, State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus, peerServerData);
        peerClient.connectionToServerDenied(peerServerData, reason, buildState());
    }

    public synchronized void disconnectedFromServer(boolean expected, PeerServerData peerServerData, State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus, peerServerData);
        peerClient.disconnectedFromServer(expected, peerServerData, buildState());
    }

    public synchronized void connectionToServerTimedOut(PeerServerData peerServerData, State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus, peerServerData);
        peerClient.connectionToServerTimedOut(peerServerData, buildState());
    }

    public synchronized void localServerOpen(int port, State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, port);
        peerClient.localServerOpen(port, buildState());
    }

    public synchronized void localServerClosed(int port, State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, port);
        peerClient.localServerClosed(port, buildState());
    }

    public synchronized void undefinedOwnInetAddress() {
        peerClient.undefinedOwnInetAddress();
    }

    public synchronized void peerCouldNotConnectToUs(Exception e, IP4Port ip4Port) {
        peerClient.peerCouldNotConnectToUs(e, ip4Port);
    }

    public synchronized void localServerError(Exception e) {
        peerClient.localServerError(e);
    }

    public synchronized void newPeerConnected(PeerID peerID, ChannelConnectionPoint ccp, ConnectionStatus status) {
        peerClient.newPeerConnected(peerID, ccp, status);
    }

    /**
     * The PeerClientConnectionManager informs that we have lost connection to a peer (either we disconnected from him, or he disconnected from us)
     *
     * @param peerID peer that was disconnected
     */
    public synchronized void peerDisconnected(PeerID peerID) {
        peerClient.peerDisconnected(peerID, null);
    }

    /**
     * The PeerClientConnectionManager informs that we have lost connection to a peer due to an error in our end
     *
     * @param peerID peer that was disconnected
     * @param error  error raised
     */
    public synchronized void peerError(PeerID peerID, CommError error) {
        peerClient.peerDisconnected(peerID, error);
    }

    public synchronized void newObjectMessageReceived(PeerID peerID, Object message) {
        peerClient.newObjectMessageReceived(peerID, message);
    }

    public synchronized void requestServerCustomFSM(RequestFromPeerToPeer requestFromPeerToPeer, String serverFSMName, PeerID peerID, ChannelConnectionPoint ccp, byte outgoingChannel) {
        peerClient.requestServerCustomFSM(requestFromPeerToPeer, serverFSMName, peerID, ccp, outgoingChannel);
    }

    public synchronized void periodicDownloadsNotification(DownloadsManager downloadsManager) {
        peerClient.periodicDownloadsNotification(downloadsManager);
    }

    public synchronized void periodicUploadsNotification(UploadsManager uploadsManager) {
        peerClient.periodicUploadsNotification(uploadsManager);
    }
}
