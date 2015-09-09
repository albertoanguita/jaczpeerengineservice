package jacz.peerengine.server;

import jacz.peerengine.PeerID;
import jacz.peerengine.util.ChannelConstants;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.channel.TimedChannelFSMAction;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.io.object_serialization.ObjectListWrapper;
import jacz.util.network.IP4Port;

import java.io.Serializable;

/**
 * FSM used to negotiate connections with PeerClients
 */
public class ServerConnectionFSM implements TimedChannelFSMAction<ServerConnectionFSM.State> {

    enum State {
        // Initial state: the server is waiting for the client's data to arrive
        INIT,
        // The client's data arrived, everything was ok, connection was accepted
        SUCCESS,
        // There were errors in the client's data. Connection was denied
        ERROR
    }

    public enum ConnectionResult implements Serializable {
        CONNECTED,
        WRONG_DATA_RECEIVED,
        SERVER_FULL,
        SERVER_NOT_ACCEPTING_NEW_CONNECTIONS,
        CLIENT_ALREADY_CONNECTED,
    }

    /**
     * The PeerServer to which the client pretends to connect, and to which this FSM serves
     */
    private PeerServer peerServer;

    /**
     * The ip of the client connecting (the PeerServer itself gives us this value at construction time, so we can use
     * it later when notifying him about the connection result)
     */
    private String ip;

    /**
     * ID of the client whose connection we must negotiate. This ID is used to identify the client in case of timeout
     */
    private UniqueIdentifier clientID;

    /**
     * Class constructor
     *
     * @param peerServer the PeerServer to which this FSM serves
     * @param ip         the ip of the client connecting to the PeerServer
     * @param clientID   ID of the client connecting
     */
    public ServerConnectionFSM(PeerServer peerServer, String ip, UniqueIdentifier clientID) {
        this.peerServer = peerServer;
        this.ip = ip;
        this.clientID = clientID;
    }

    @Override
    public State processMessage(State state, byte canal, Object message, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        // the only possible state is INIT, since the rest of states are final
        if (state != State.INIT) {
            throw new IllegalArgumentException();
        }
        if (message instanceof ObjectListWrapper) {
            // extract received data (it should be the client's id and its port)
            ObjectListWrapper objects = (ObjectListWrapper) message;
            if (!(objects.getObjects().get(0) instanceof PeerID) || !(objects.getObjects().get(1) instanceof String) || !(objects.getObjects().get(2) instanceof Integer)) {
                ccp.write(ChannelConstants.PEER_SERVER_CONNECTION_CHANNEL, ConnectionResult.WRONG_DATA_RECEIVED);
                peerServer.clientBehavingBadDuringConnectionProcess(clientID, ip, ConnectionResult.WRONG_DATA_RECEIVED);
                return State.ERROR;
            }
            PeerID clientPeerID = (PeerID) objects.getObjects().get(0);
            String localIP = (String) objects.getObjects().get(1);
            int port = (Integer) objects.getObjects().get(2);

            // everything ok -> inform peer server about new client
            IP4Port clientIp4Port = new IP4Port(ip, port);
            ConnectionResult result = peerServer.newClientConnected(clientPeerID, ccp, clientIp4Port, localIP);

            // inform client (send a message) of connection result
            ccp.write(ChannelConstants.PEER_SERVER_CONNECTION_CHANNEL, result);
            if (result == ConnectionResult.CONNECTED) {
                return State.SUCCESS;
            } else {
                peerServer.clientBehavingBadDuringConnectionProcess(clientID, ip, result);
                return State.ERROR;
            }
        } else {
            // inform client (send a message)
            ccp.write(ChannelConstants.PEER_SERVER_CONNECTION_CHANNEL, ConnectionResult.WRONG_DATA_RECEIVED);
            peerServer.clientBehavingBadDuringConnectionProcess(clientID, ip, ConnectionResult.WRONG_DATA_RECEIVED);
            return State.ERROR;
        }
    }

    @Override
    public State processMessage(State state, byte canal, byte[] data, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        ccp.write(ChannelConstants.PEER_SERVER_CONNECTION_CHANNEL, ConnectionResult.WRONG_DATA_RECEIVED);
        return State.ERROR;
    }

    @Override
    public State init(ChannelConnectionPoint ccp) {
        return State.INIT;
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
        // the peer got disconnected before completing the connection process -> ignore
    }

    @Override
    public void timedOut(State state, ChannelConnectionPoint ccp) {
        peerServer.clientTimedOut(clientID);
    }
}
