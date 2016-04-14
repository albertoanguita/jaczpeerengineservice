package jacz.peerengineservice.client;

import jacz.peerengineservice.client.connection.ConnectionEvents;
import jacz.peerengineservice.client.connection.State;
import jacz.util.network.IP4Port;

/**
 * Created by Alberto on 12/04/2016.
 */
public class ConnectionEventsImpl implements ConnectionEvents {

    public void init() {
    }

    @Override
    public void listeningPortModified(int port) {
        System.out.println("Listening port modified: " + port);
    }

    @Override
    public void initializingConnection() {
        System.out.println("Initializing connection");
    }

    @Override
    public void localAddressFetched(String localAddress, State state) {
        System.out.println("Local address fetched. Local address: " + localAddress + ". State: " + state);
    }

    @Override
    public void couldNotFetchLocalAddress(State state) {
        System.out.println("Could not fetch local address. State: " + state);
    }

    @Override
    public void tryingToFetchExternalAddress(State state) {
        System.out.println("Trying to fetch external address. State: " + state);
    }

    @Override
    public void externalAddressFetched(String externalAddress, boolean hasGateway, State state) {
        System.out.println("External address fetched. External address: " + externalAddress + ". Has gateway: " + hasGateway + ". State: " + state);
    }

    @Override
    public void couldNotFetchExternalAddress(State state) {
        System.out.println("Could not fetch external address. State: " + state);
    }

    @Override
    public void unrecognizedMessageFromServer(State state) {
        System.out.println("Unrecognized message from server. State: " + state);
    }

    @Override
    public void tryingToConnectToServer( State state) {
        System.out.println("Trying to connect to server. State: " + state);
    }

    @Override
    public void connectionToServerEstablished(State state) {
        System.out.println("Connected to server. State: " + state);
    }

    @Override
    public void registrationRequired(State state) {
        System.out.println("Registration with server required. State: " + state);
    }

    @Override
    public void localServerUnreachable(State state) {
        System.out.println("Local server unreachable. State: " + state);
    }

    @Override
    public void unableToConnectToServer(State state) {
        System.out.println("Unable to connect to server. State: " + state);
    }

    @Override
    public void tryingToOpenLocalServer(State state) {
        System.out.println("Trying to open Local server. State: " + state);
    }

    @Override
    public void localServerOpen(State state) {
        System.out.println("Local server open. State: " + state);
    }

    @Override
    public void couldNotOpenLocalServer(State state) {
        System.out.println("Could not open local server. State: " + state);
    }

    @Override
    public void tryingToCloseLocalServer(State state) {
        System.out.println("Trying to close local server. State: " + state);
    }

    @Override
    public void localServerClosed(State state) {
        System.out.println("Local server closed. State: " + state);
    }

    @Override
    public void tryingToCreateNATRule(State state) {
        System.out.println("Trying to create NAT rule. State: " + state);
    }

    @Override
    public void NATRuleCreated(State state) {
        System.out.println("NAT rule created. State: " + state);
    }

    @Override
    public void couldNotFetchUPNPGateway(State state) {
        System.out.println("Could not fetch UPNP gateway. State: " + state);
    }

    @Override
    public void errorCreatingNATRule(State state) {
        System.out.println("Error creating NAT rule. State: " + state);
    }

    @Override
    public void tryingToDestroyNATRule(State state) {
        System.out.println("Trying to destroy NAT rule. State: " + state);
    }

    @Override
    public void NATRuleDestroyed(State state) {
        System.out.println("NAT rule destroyed. State: " + state);
    }

    @Override
    public void couldNotDestroyNATRule(State state) {
        System.out.println("Could not destroy NAT rule. State: " + state);
    }

    @Override
    public void listeningConnectionsWithoutNATRule(State state) {
        System.out.println("Listening connections without NAT rule. State: " + state);
    }

    @Override
    public void disconnectedFromServer(State state) {
        System.out.println("Disconnected from server. State: " + state);
    }

    @Override
    public void failedToRefreshServerConnection(State state) {
        System.out.println("Failed to refresh server connection. State: " + state);
    }

    @Override
    public void tryingToRegisterWithServer(State state) {
        System.out.println("Trying to register with server. State: " + state);
    }

    @Override
    public void registrationSuccessful(State state) {
        System.out.println("Registration with server successful. State: " + state);
    }

    @Override
    public void alreadyRegistered(State state) {
        System.out.println("Already registered. State: " + state);
    }

    @Override
    public void peerCouldNotConnectToUs(Exception e, IP4Port ip4Port) {
        System.out.println("Peer failed to connect to us from " + ip4Port.toString() + ". " + e.getMessage());
    }

    @Override
    public void localServerError(Exception e) {
        System.out.println("Error in the peer connections listener. All connections closed. Error: " + e.getMessage());
    }

}
