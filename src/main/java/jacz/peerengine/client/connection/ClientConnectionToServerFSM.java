package jacz.peerengine.client.connection;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.channel.TimedChannelFSMAction;
import jacz.util.io.object_serialization.ObjectListWrapper;
import jacz.peerengine.PeerID;
import jacz.peerengine.server.PeerServer;
import jacz.peerengine.server.ServerConnectionFSM;

import java.net.InetAddress;

/**
 * FSM used to connect to PeerServers
 */
public class ClientConnectionToServerFSM implements TimedChannelFSMAction<ClientConnectionToServerFSM.State> {

    enum State {
        // Initial state: the data of our peer has been sent to the server (ID and connection port)
        DATA_SENT,
        // The server answered ok to our connection attempt
        SUCCESS,
        // The connection to the server failed
        ERROR
    }

    public enum ConnectionFailureReason {
        WRONG_CONNECTION_NEGOTIATION_PROCESS,
        SERVER_FULL,
        SERVER_NOT_ACCEPTING_NEW_CONNECTIONS,
        CLIENT_ALREADY_CONNECTED,
        DISCONNECTED
    }

    private final PeerServerManager peerServerManager;

    /**
     * The ID of our peer
     */
    private final PeerID ownPeerID;

    /**
     * The client's local internet address
     */
    private final InetAddress localAddress;

    /**
     * The port at which we will be accepting future connections. The server needs to know it in order to tell other
     * peers how to connect to us
     */
    private final int port;

    /**
     * Class constructor
     *
     * @param peerServerManager the peerServerManager to which this connection FSM belongs to
     * @param ownPeerID         our peer ID
     * @param localAddress      ip address for our local network (the ip assigned to our network interface)
     * @param port              our connection port
     */
    public ClientConnectionToServerFSM(PeerServerManager peerServerManager, PeerID ownPeerID, InetAddress localAddress, int port) {
        this.peerServerManager = peerServerManager;
        this.ownPeerID = ownPeerID;
        this.localAddress = localAddress;
        this.port = port;
    }

    @Override
    public State processMessage(State state, byte canal, Object message, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        if (state != State.DATA_SENT) {
            connectionToServerFailed(ConnectionFailureReason.WRONG_CONNECTION_NEGOTIATION_PROCESS, ccp);
            throw new IllegalArgumentException();
        }
        // the only possible state at this point is DATA_SENT. A ConnectionResult object must be received
        if (message instanceof ServerConnectionFSM.ConnectionResult) {
            ServerConnectionFSM.ConnectionResult result = (ServerConnectionFSM.ConnectionResult) message;
            if (result == ServerConnectionFSM.ConnectionResult.CONNECTED) {
                connectionToServerSucceeded(ccp);
                return State.SUCCESS;
            } else {
                switch (result) {

                    case WRONG_DATA_RECEIVED:
                        connectionToServerFailed(ConnectionFailureReason.WRONG_CONNECTION_NEGOTIATION_PROCESS, ccp);
                        break;
                    case SERVER_FULL:
                        connectionToServerFailed(ConnectionFailureReason.SERVER_FULL, ccp);
                        break;
                    case SERVER_NOT_ACCEPTING_NEW_CONNECTIONS:
                        connectionToServerFailed(ConnectionFailureReason.SERVER_NOT_ACCEPTING_NEW_CONNECTIONS, ccp);
                        break;
                    case CLIENT_ALREADY_CONNECTED:
                        connectionToServerFailed(ConnectionFailureReason.CLIENT_ALREADY_CONNECTED, ccp);
                        break;
                }
                return State.ERROR;
            }
        } else {
            // wrong data received
            connectionToServerFailed(ConnectionFailureReason.WRONG_CONNECTION_NEGOTIATION_PROCESS, ccp);
            return State.ERROR;
        }
    }

    @Override
    public State processMessage(State state, byte canal, byte[] data, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        // unexpected data received
        return State.ERROR;
    }

    @Override
    public State init(ChannelConnectionPoint ccp) {
        // send personal id and port
        ObjectListWrapper personalData = new ObjectListWrapper(ownPeerID, localAddress.getHostAddress(), port);
        ccp.write(PeerServer.CONNECTION_CHANNEL, personalData);
        return State.DATA_SENT;
    }

    @Override
    public boolean isFinalState(State state, ChannelConnectionPoint ccp) {
        switch (state) {

            case SUCCESS:
                return true;
            case ERROR:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void disconnected(ChannelConnectionPoint ccp) {
        // we got disconnected from the server during the process
        connectionToServerFailed(ConnectionFailureReason.DISCONNECTED, ccp);
    }

    @Override
    public void timedOut(State state, ChannelConnectionPoint ccp) {
        peerServerManager.serverTookToMuchTimeToAnswerConnectionRequest(ccp);
    }

    private void connectionToServerSucceeded(ChannelConnectionPoint ccp) {
        peerServerManager.connectionToServerEstablished(ccp);
    }

    private void connectionToServerFailed(ConnectionFailureReason reason, ChannelConnectionPoint ccp) {
        peerServerManager.connectionToServerDenied(reason, ccp);
    }
}
