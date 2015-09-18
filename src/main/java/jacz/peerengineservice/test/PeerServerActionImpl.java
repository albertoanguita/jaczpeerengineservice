package jacz.peerengineservice.test;

import jacz.commengine.communication.CommError;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.network.IP4Port;
import jacz.peerengineservice.server.ConnectedClientData;
import jacz.peerengineservice.server.PeerServerAction;
import jacz.peerengineservice.server.ServerConnectionFSM;

/**
 * Simple implementation of the PeerServerAction interface
 */
public class PeerServerActionImpl implements PeerServerAction {

    @Override
    public void serverStarted() {
        System.out.println("Server started");
    }

    @Override
    public void serverStopped() {
        System.out.println("Server stopped");
    }

    @Override
    public void serverFailed(Exception e) {
        System.out.println("Server failed");
    }

    @Override
    public void clientConnected(ConnectedClientData connectedClientData) {
        System.out.println("Client connected");
    }

    @Override
    public void clientDisconnected(ConnectedClientData connectedClientData) {
        System.out.println("Client disconnected");
    }

    @Override
    public void clientConnectionError(ConnectedClientData connectedClientData, CommError error) {
        System.out.println("Error in client connection");
    }

    @Override
    public void clientDisconnectedDueToBadBehavior(ConnectedClientData connectedClientData) {
        System.out.println("Client disconnected due to bad behaviour");
    }

    @Override
    public void clientCouldNotConnect(UniqueIdentifier clientID, String clientIP, ServerConnectionFSM.ConnectionResult reason) {
        System.out.println("Client failed connecting due to bad behaviour");
    }

    @Override
    public void clientTimedOut(ConnectedClientData connectedClientData) {
        System.out.println("Client timed out");
    }

    @Override
    public void clientFailedConnection(Exception e, IP4Port ip4Port) {
        System.out.println("Client failed connecting");
    }
}
