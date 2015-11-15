package jacz.peerengineservice.server;

import jacz.peerengineservice.PeerID;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.clientserver.server.ServerModule;
import jacz.commengine.communication.CommError;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.network.IP4Port;

import java.io.IOException;
import java.util.*;

/**
 * This class represents the Peer Server that accepts connections from peer clients. The Peer Server mission is to allow clients find each other.
 * todo remove
 */
public class PeerServer {

    /**
     * Channel for exchanging connection data
     */
    public static final byte CONNECTION_CHANNEL = 0;

    public static final long CONNECTION_TIMEOUT_MILLIS = 5000l;

    /**
     * Channel for requesting friends data
     */
    public static final byte REQUESTS_CHANNEL = 1;

    public static final long TIMEOUT_MILLIS = 60000l;


    /**
     * Table of connected clients, indexed by their peer identifier. The data stored in each row is all
     * the data we have about each client. All connected clients are stored here, and any client not appearing here
     * is not connected to the server.
     */
    private Map<PeerID, ConnectedClientData> connectedClients;

    /**
     * Table of connected clients, indexed by their connection identifier (to their identifier in the server). This
     * table allows to find the client's connection data when we only have their connection id (given by the TCP server
     * when the client connects)
     */
    private Map<UniqueIdentifier, PeerID> clientIDToPeerID;

    /**
     * Server of the client-server level. With it we open a server for peer clients to connect and exchange information with us
     */
    private ServerModule serverModule;

    /**
     * Events logged by the PeerServer are reported here
     */
    private PeerServerAction peerServerAction;

    /**
     * Indicates if this server is running (accepting connections from clients) or not. It is needed because we might receive connection
     * requests after we have stopped the server (so we check this variable in each new connection to ensure that it must be accepted)
     */
    private boolean running;


    public PeerServer(PeerServerConfig peerServerConfig, PeerServerAction peerServerAction) {
        connectedClients = new HashMap<>();
        clientIDToPeerID = new HashMap<>();
        Set<Byte> allChannels = new HashSet<>();
        for (Byte channel = Byte.MIN_VALUE; channel < Byte.MAX_VALUE; channel++) {
            allChannels.add(channel);
        }
        allChannels.add(Byte.MAX_VALUE);
        Set<Set<Byte>> concurrentChannels = new HashSet<>();
        concurrentChannels.add(allChannels);
        serverModule = new ServerModule(peerServerConfig.getPort(), new ServerActionImpl(this), concurrentChannels);
        this.peerServerAction = peerServerAction;
        running = false;
    }

    public synchronized void startServer() {
        try {
            serverModule.startListeningConnections();
        } catch (IOException e) {
            e.printStackTrace();
        }
        running = true;
        peerServerAction.serverStarted();
    }

    public synchronized void stopServer() {
        // stop accepting new connections and disconnect all connected clients
        serverModule.stopAndDisconnect();
        running = false;
        peerServerAction.serverStopped();
    }

    synchronized boolean isRunning() {
        return running;
    }

    /**
     * The server failed due to an error in the TCP connection server. The server was stopped
     *
     * @param e exception that caused the error
     */
    synchronized void TCPServerFailed(Exception e) {
        stopServer();
        peerServerAction.serverFailed(e);
    }

    /**
     * This method allows the ServerConnectionFSM to inform about the successful connection of a new client
     *
     * @param peerID  ID of the new client
     * @param ccp     ChannelConnectionPoint object to communicate with this new client
     * @param ip4Port network data of the client
     * @param localIP local ip of the client
     * @return result of the connection attempt
     */
    synchronized ServerConnectionFSM.ConnectionResult newClientConnected(PeerID peerID, ChannelConnectionPoint ccp, IP4Port ip4Port, String localIP) {
        if (running) {
            if (!connectedClients.containsKey(peerID)) {
                UniqueIdentifier clientID = ccp.getId();
                ConnectedClientData connectedClientData = new ConnectedClientData(clientID, peerID, ccp, ip4Port, localIP);
                connectedClients.put(peerID, connectedClientData);
                clientIDToPeerID.put(clientID, peerID);
                peerServerAction.clientConnected(connectedClientData);
                return ServerConnectionFSM.ConnectionResult.CONNECTED;
            } else {
                return ServerConnectionFSM.ConnectionResult.CLIENT_ALREADY_CONNECTED;
            }
        } else {
            return ServerConnectionFSM.ConnectionResult.SERVER_NOT_ACCEPTING_NEW_CONNECTIONS;
        }
    }

    synchronized Map<PeerID, PeerConnectionInfo> searchConnectedClients(Collection<PeerID> clients) {
        Map<PeerID, PeerConnectionInfo> result = new HashMap<PeerID, PeerConnectionInfo>(clients.size());
        for (PeerID peerID : clients) {
            if (connectedClients.containsKey(peerID)) {
                result.put(peerID, connectedClients.get(peerID).getPeerConnectionInfo());
            }
        }
        return result;
    }

    /**
     * This method allows the ServerActionImpl to notify us that a client has disconnected from us
     *
     * @param clientID UniqueIdentifier identifying the client
     */
    synchronized void clientDisconnected(UniqueIdentifier clientID) {
        // not all clients that can disconnect are stored. For examples, client that attempted to connect but were already connected
        if (clientIDToPeerID.containsKey(clientID)) {
            // remove all data from this client
            ConnectedClientData connectedClientData = removeClient(clientID, false);
            peerServerAction.clientDisconnected(connectedClientData);
        }
    }

    synchronized void clientTimedOut(UniqueIdentifier clientID) {
        if (clientIDToPeerID.containsKey(clientID)) {
            // remove all data from this client and disconnect it
            ConnectedClientData connectedClientData = removeClient(clientID, true);
            peerServerAction.clientTimedOut(connectedClientData);
        }
    }

    /**
     * This method allows the ServerActionImpl to notify us that there has been a problem with a connected client.
     * The connection has broken.
     *
     * @param clientID UniqueIdentifier identifying the client
     * @param error    the exception that caused the connection to break
     */
    synchronized void clientConnectionError(UniqueIdentifier clientID, CommError error) {
        // remove all data from this client
        // no need to disconnect from client, as the server already tool care of that
        ConnectedClientData connectedClientData = removeClient(clientID, false);
        peerServerAction.clientConnectionError(connectedClientData, error);
    }

    /**
     * This method allows the ServerActionImpl to notify us that a client is not following the communication rules. The client will
     * be disconnected
     *
     * @param clientID UniqueIdentifier identifying the client
     */
    synchronized void clientBehavingBad(UniqueIdentifier clientID) {
        // remove all data from this client and disconnect it
        ConnectedClientData connectedClientData = removeClient(clientID, true);
        peerServerAction.clientDisconnectedDueToBadBehavior(connectedClientData);
    }

    synchronized void clientBehavingBadDuringConnectionProcess(UniqueIdentifier clientID, String clientIP, ServerConnectionFSM.ConnectionResult reason) {
        peerServerAction.clientCouldNotConnect(clientID, clientIP, reason);
    }

    synchronized void failedConnectionWithNewClient(Exception e, IP4Port ip4Port) {
        peerServerAction.clientFailedConnection(e, ip4Port);
    }

    /**
     * Removes all data from a connected client (due to him disconnecting, or an error in the connection)
     *
     * @param clientID       UniqueIdentifier identifying the client
     * @param alsoDisconnect if the server must disconnect the client (true) or not (false)
     * @return the connected client data object of the removed client, or null if this client did not exist
     */
    private synchronized ConnectedClientData removeClient(UniqueIdentifier clientID, boolean alsoDisconnect) {
        if (clientIDToPeerID.containsKey(clientID)) {
            if (alsoDisconnect) {
                serverModule.disconnectClient(clientID);
            }
            ConnectedClientData connectedClientData = connectedClients.remove(clientIDToPeerID.get(clientID));
            clientIDToPeerID.remove(clientID);
            return connectedClientData;
        }
        return null;
    }
}
