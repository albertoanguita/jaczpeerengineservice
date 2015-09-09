package jacz.peerengine.server;

import jacz.commengine.communication.CommError;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.network.IP4Port;

/**
 * This interface contains methods invoked by the peer server so that different events are reported back to the application that creates the
 * peer server
 */
public interface PeerServerAction {

    /**
     * The server was started and is now running
     */
    public void serverStarted();

    /**
     * The server was stopped (user-initiated event)
     */
    public void serverStopped();

    /**
     * The server failed due to an error in the TCP server. The server was stopped and could not be restarted
     *
     * @param e exception that caused the error
     */
    public void serverFailed(Exception e);

    /**
     * A new client connected
     *
     * @param connectedClientData clients data
     */
    public void clientConnected(ConnectedClientData connectedClientData);

    /**
     * A connected client disconnected from the server
     *
     * @param connectedClientData clients data
     */
    public void clientDisconnected(ConnectedClientData connectedClientData);

    /**
     * A client was disconnected due to an error in the communication channel
     *
     * @param connectedClientData clients data
     * @param error               communication error
     */
    public void clientConnectionError(ConnectedClientData connectedClientData, CommError error);

    /**
     * A client was disconnected due to the client's bad behavior (the client sent data to the server in an incorrect format, so the server
     * disconnected him)
     *
     * @param connectedClientData clients data
     */
    public void clientDisconnectedDueToBadBehavior(ConnectedClientData connectedClientData);

    /**
     * A clients connection process did not finish successfully due to the client submitting data in incorrect format. The client was disconnected
     *
     * @param clientID clients id
     * @param clientIP ip of the client
     * @param reason   reason for the connection failure
     */
    public void clientCouldNotConnect(UniqueIdentifier clientID, String clientIP, ServerConnectionFSM.ConnectionResult reason);

    /**
     * A connected client was disconnected due to timeout
     *
     * @param connectedClientData clients data
     */
    public void clientTimedOut(ConnectedClientData connectedClientData);

    /**
     * A new client's connection process failed due to errors in the TCP listening process. The client was disconnected
     *
     * @param e       exception that caused the error
     * @param ip4Port ip and port of the disconnected client
     */
    public void clientFailedConnection(Exception e, IP4Port ip4Port);

}
