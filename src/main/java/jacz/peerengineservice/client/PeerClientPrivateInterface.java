package jacz.peerengineservice.client;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.connection.RequestFromPeerToPeer;
import jacz.peerengineservice.client.connection.State;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.peerengineservice.util.datatransfer.DownloadsManager;
import jacz.peerengineservice.util.datatransfer.UploadsManager;
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

    private State.NetworkTopologyState networkTopologyState;

    private State.ConnectionToServerState connectionToServerState;

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
        networkTopologyState = State.NetworkTopologyState.NO_DATA;
        connectionToServerState = State.ConnectionToServerState.DISCONNECTED;
        localServerConnectionsState = State.LocalServerConnectionsState.CLOSED;
    }

    private synchronized State buildState() {
        return new State(networkTopologyState, connectionToServerState, localServerConnectionsState, port);
    }

    public synchronized State getState() {
        return buildState();
    }

    private void updateNetworkTopologyState(State.NetworkTopologyState networkTopologyState) {
        this.networkTopologyState = networkTopologyState;
    }

    private void updateConnectionToServerInfo(State.ConnectionToServerState connectionToServerStatus) {
        this.connectionToServerState = connectionToServerStatus;
    }

    private void updateLocalServerInfo(State.LocalServerConnectionsState localServerConnectionsState, int port) {
        this.localServerConnectionsState = localServerConnectionsState;
        if (localServerConnectionsState == State.LocalServerConnectionsState.OPEN) {
            this.port = port;
        } else {
            this.port = -1;
        }
    }

    ////////////////////////////////// PEER CLIENT CONNECTION MANAGER  //////////////////////////////////

    public synchronized void initializingConnection() {
        peerClient.initializingConnection();
    }

    public synchronized void listeningPortModified(int port) {
        peerClient.listeningPortModified(port);
    }

    public synchronized void undefinedOwnInetAddress() {
        peerClient.undefinedOwnInetAddress();
    }


    ////////////////////////////////// NETWORK TOPOLOGY MANAGER  //////////////////////////////////

    public synchronized void localAddressFetched(String localAddress, State.NetworkTopologyState networkTopologyState) {
        updateNetworkTopologyState(networkTopologyState);
        peerClient.localAddressFetched(localAddress, buildState());
    }

    public synchronized void couldNotFetchLocalAddress(State.NetworkTopologyState networkTopologyState) {
        updateNetworkTopologyState(networkTopologyState);
        peerClient.couldNotFetchLocalAddress(buildState());
    }

    public synchronized void tryingToFetchExternalAddress(State.NetworkTopologyState networkTopologyState) {
        updateNetworkTopologyState(networkTopologyState);
        peerClient.tryingToFetchExternalAddress(buildState());
    }

    public synchronized void externalAddressFetched(String externalAddress, boolean hasGateway, State.NetworkTopologyState networkTopologyState) {
        updateNetworkTopologyState(networkTopologyState);
        peerClient.externalAddressFetched(externalAddress, hasGateway, buildState());
    }

    public synchronized void couldNotFetchExternalAddress(State.NetworkTopologyState networkTopologyState) {
        updateNetworkTopologyState(networkTopologyState);
        peerClient.couldNotFetchExternalAddress(buildState());
    }






    ////////////////////////////////// PEER SERVER MANAGER  //////////////////////////////////

    public synchronized void unrecognizedMessageFromServer(State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        peerClient.unrecognizedMessageFromServer(buildState());
    }

    public synchronized void tryingToConnectToServer(State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        peerClient.tryingToConnectToServer(buildState());
    }

    public synchronized void connectionToServerEstablished(State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        peerClient.connectionToServerEstablished(buildState());
    }

    public synchronized void registrationRequired(State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        peerClient.registrationRequired(buildState());
    }

    public synchronized void localServerUnreachable(State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        peerClient.localServerUnreachable(buildState());
    }

    public synchronized void unableToConnectToServer(State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        peerClient.unableToConnectToServer(buildState());
    }

    public synchronized void disconnectedFromServer(State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        peerClient.disconnectedFromServer(buildState());
    }

    public synchronized void failedToRefreshServerConnection(State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        peerClient.failedToRefreshServerConnection(buildState());
    }

    public synchronized void tryingToRegisterWithServer(State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        peerClient.tryingToRegisterWithServer(buildState());
    }

    public synchronized void registrationSuccessful(State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        peerClient.registrationSuccessful(buildState());
    }

    public synchronized void alreadyRegistered(State.ConnectionToServerState connectionToServerStatus) {
        updateConnectionToServerInfo(connectionToServerStatus);
        peerClient.alreadyRegistered(buildState());
    }



    ////////////////////////////////// LOCAL SERVER MANAGER  //////////////////////////////////

    public synchronized void localServerOpen(int port, State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, port);
        peerClient.localServerOpen(port, buildState());
    }

    public synchronized void localServerClosed(int port, State.LocalServerConnectionsState localServerConnectionsState) {
        updateLocalServerInfo(localServerConnectionsState, port);
        peerClient.localServerClosed(port, buildState());
    }

    public synchronized void peerCouldNotConnectToUs(Exception e, IP4Port ip4Port) {
        peerClient.peerCouldNotConnectToUs(e, ip4Port);
    }

    public synchronized void localServerError(Exception e) {
        peerClient.localServerError(e);
    }


    ////////////////////////////////// FRIEND CONNECTION MANAGER  //////////////////////////////////

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
