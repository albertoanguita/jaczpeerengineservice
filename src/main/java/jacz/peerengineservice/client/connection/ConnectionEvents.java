package jacz.peerengineservice.client.connection;

import jacz.util.network.IP4Port;

/**
 * Events related to connection with server or connection with other peers
 */
public interface ConnectionEvents {

    void listeningPortModified(int port);

    void initializingConnection();

    void localAddressFetched(String localAddress, State state);

    void couldNotFetchLocalAddress(State state);

    void tryingToFetchExternalAddress(State state);

    void externalAddressFetched(String externalAddress, boolean hasGateway, State state);

    void couldNotFetchExternalAddress(State state);

    void connectionParametersChanged(State state);

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

    void tryingToOpenLocalServer(State state);

    void localServerOpen(State state);

    void couldNotOpenLocalServer(State state);

    void tryingToCloseLocalServer(State state);

    void localServerClosed(State state);

    void tryingToCreateNATRule(State state);

    void NATRuleCreated(State state);

    void couldNotFetchUPNPGateway(State state);

    void errorCreatingNATRule(State state);

    void tryingToDestroyNATRule(State state);

    void NATRuleDestroyed(State state);

    void couldNotDestroyNATRule(State state);

    void listeningConnectionsWithoutNATRule(State state);

    void peerCouldNotConnectToUs(Exception e, IP4Port ip4Port);

    void localServerError(Exception e);
}
