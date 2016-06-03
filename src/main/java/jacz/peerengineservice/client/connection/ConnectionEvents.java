package jacz.peerengineservice.client.connection;

import jacz.util.network.IP4Port;

/**
 * Events related to connection with server or connection with other peers
 */
public interface ConnectionEvents {

    void localPortModified(ConnectionState connectionState);

    void externalPortModified(ConnectionState connectionState);

    void initializingConnection(ConnectionState connectionState);

    void localAddressFetched(ConnectionState connectionState);

    void couldNotFetchLocalAddress(ConnectionState connectionState);

    void tryingToFetchExternalAddress(ConnectionState connectionState);

    void externalAddressFetched(ConnectionState connectionState);

    void couldNotFetchExternalAddress(ConnectionState connectionState);

    void unrecognizedMessageFromServer(ConnectionState connectionState);

    void tryingToConnectToServer(ConnectionState connectionState);

    void connectionToServerEstablished(ConnectionState connectionState);

    void registrationRequired(ConnectionState connectionState);

    void localServerUnreachable(ConnectionState connectionState);

    void unableToConnectToServer(ConnectionState connectionState);

    void disconnectedFromServer(ConnectionState connectionState);

    void failedToRefreshServerConnection(ConnectionState connectionState);

    void tryingToRegisterWithServer(ConnectionState connectionState);

    void registrationSuccessful(ConnectionState connectionState);

    void alreadyRegistered(ConnectionState connectionState);

    void tryingToOpenLocalServer(ConnectionState connectionState);

    void localServerOpen(ConnectionState connectionState);

    void couldNotOpenLocalServer(ConnectionState connectionState);

    void tryingToCloseLocalServer(ConnectionState connectionState);

    void localServerClosed(ConnectionState connectionState);

    void tryingToCreateNATRule(ConnectionState connectionState);

    void NATRuleCreated(ConnectionState connectionState);

    void couldNotFetchUPNPGateway(ConnectionState connectionState);

    void errorCreatingNATRule(ConnectionState connectionState);

    void tryingToDestroyNATRule(ConnectionState connectionState);

    void NATRuleDestroyed(ConnectionState connectionState);

    void couldNotDestroyNATRule(ConnectionState connectionState);

    void listeningConnectionsWithoutNATRule(ConnectionState connectionState);

    void peerCouldNotConnectToUs(Exception e, IP4Port ip4Port);

    void localServerError(ConnectionState connectionState, Exception e);
}
