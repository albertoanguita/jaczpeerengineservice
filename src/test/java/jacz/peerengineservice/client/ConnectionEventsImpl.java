package jacz.peerengineservice.client;

import jacz.peerengineservice.client.connection.ConnectionEvents;
import jacz.peerengineservice.client.connection.ConnectionState;
import jacz.util.network.IP4Port;

/**
 * Created by Alberto on 12/04/2016.
 */
public class ConnectionEventsImpl implements ConnectionEvents {

    @Override
    public void localPortModified(ConnectionState connectionState) {
        System.out.println("Local port modified: " + connectionState);
    }

    @Override
    public void externalPortModified(ConnectionState connectionState) {
        System.out.println("External port modified: " + connectionState);
    }

    @Override
    public void initializingConnection(ConnectionState connectionState) {
        System.out.println("Initializing connection");
    }

    @Override
    public void localAddressFetched(ConnectionState connectionState) {
        System.out.println("Local address fetched. ConnectionState: " + connectionState);
    }

    @Override
    public void couldNotFetchLocalAddress(ConnectionState connectionState) {
        System.out.println("Could not fetch local address. ConnectionState: " + connectionState);
    }

    @Override
    public void tryingToFetchExternalAddress(ConnectionState connectionState) {
        System.out.println("Trying to fetch external address. ConnectionState: " + connectionState);
    }

    @Override
    public void externalAddressFetched(ConnectionState connectionState) {
        System.out.println("External address fetched. ConnectionState: " + connectionState);
    }

    @Override
    public void couldNotFetchExternalAddress(ConnectionState connectionState) {
        System.out.println("Could not fetch external address. ConnectionState: " + connectionState);
    }

    @Override
    public void unrecognizedMessageFromServer(ConnectionState connectionState) {
        System.out.println("Unrecognized message from server. ConnectionState: " + connectionState);
    }

    @Override
    public void tryingToConnectToServer( ConnectionState connectionState) {
        System.out.println("Trying to connect to server. ConnectionState: " + connectionState);
    }

    @Override
    public void connectionToServerEstablished(ConnectionState connectionState) {
        System.out.println("Connected to server. ConnectionState: " + connectionState);
    }

    @Override
    public void registrationRequired(ConnectionState connectionState) {
        System.out.println("Registration with server required. ConnectionState: " + connectionState);
    }

    @Override
    public void localServerUnreachable(ConnectionState connectionState) {
        System.out.println("Local server unreachable. ConnectionState: " + connectionState);
    }

    @Override
    public void unableToConnectToServer(ConnectionState connectionState) {
        System.out.println("Unable to connect to server. ConnectionState: " + connectionState);
    }

    @Override
    public void tryingToOpenLocalServer(ConnectionState connectionState) {
        System.out.println("Trying to open Local server. ConnectionState: " + connectionState);
    }

    @Override
    public void localServerOpen(ConnectionState connectionState) {
        System.out.println("Local server open. ConnectionState: " + connectionState);
    }

    @Override
    public void couldNotOpenLocalServer(ConnectionState connectionState) {
        System.out.println("Could not open local server. ConnectionState: " + connectionState);
    }

    @Override
    public void tryingToCloseLocalServer(ConnectionState connectionState) {
        System.out.println("Trying to close local server. ConnectionState: " + connectionState);
    }

    @Override
    public void localServerClosed(ConnectionState connectionState) {
        System.out.println("Local server closed. ConnectionState: " + connectionState);
    }

    @Override
    public void tryingToCreateNATRule(ConnectionState connectionState) {
        System.out.println("Trying to create NAT rule. ConnectionState: " + connectionState);
    }

    @Override
    public void NATRuleCreated(ConnectionState connectionState) {
        System.out.println("NAT rule created. ConnectionState: " + connectionState);
    }

    @Override
    public void couldNotFetchUPNPGateway(ConnectionState connectionState) {
        System.out.println("Could not fetch UPNP gateway. ConnectionState: " + connectionState);
    }

    @Override
    public void errorCreatingNATRule(ConnectionState connectionState) {
        System.out.println("Error creating NAT rule. ConnectionState: " + connectionState);
    }

    @Override
    public void tryingToDestroyNATRule(ConnectionState connectionState) {
        System.out.println("Trying to destroy NAT rule. ConnectionState: " + connectionState);
    }

    @Override
    public void NATRuleDestroyed(ConnectionState connectionState) {
        System.out.println("NAT rule destroyed. ConnectionState: " + connectionState);
    }

    @Override
    public void couldNotDestroyNATRule(ConnectionState connectionState) {
        System.out.println("Could not destroy NAT rule. ConnectionState: " + connectionState);
    }

    @Override
    public void listeningConnectionsWithoutNATRule(ConnectionState connectionState) {
        System.out.println("Listening connections without NAT rule. ConnectionState: " + connectionState);
    }

    @Override
    public void disconnectedFromServer(ConnectionState connectionState) {
        System.out.println("Disconnected from server. ConnectionState: " + connectionState);
    }

    @Override
    public void failedToRefreshServerConnection(ConnectionState connectionState) {
        System.out.println("Failed to refresh server connection. ConnectionState: " + connectionState);
    }

    @Override
    public void tryingToRegisterWithServer(ConnectionState connectionState) {
        System.out.println("Trying to register with server. ConnectionState: " + connectionState);
    }

    @Override
    public void registrationSuccessful(ConnectionState connectionState) {
        System.out.println("Registration with server successful. ConnectionState: " + connectionState);
    }

    @Override
    public void alreadyRegistered(ConnectionState connectionState) {
        System.out.println("Already registered. ConnectionState: " + connectionState);
    }

    @Override
    public void peerCouldNotConnectToUs(Exception e, IP4Port ip4Port) {
        System.out.println("Peer failed to connect to us from " + ip4Port.toString() + ". " + e.getMessage());
    }

    @Override
    public void localServerError(ConnectionState connectionState, Exception e) {
        System.out.println("Error in the peer connections listener. All connections closed. ConnectionState: " + connectionState + ". Error: " + e.getMessage());
    }

}
