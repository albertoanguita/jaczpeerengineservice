package jacz.peerengineservice.server;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.clientserver.server.ServerAction;
import jacz.commengine.communication.CommError;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.network.IP4Port;

import java.util.Set;

/**
 * ServerAction employed by a PeerServer to listen to messages from connected clients. No messages are actually
 * expected at this level, since all communications are handled with FSMs at this level.
 */
class ServerActionImpl implements ServerAction {

    private PeerServer peerServer;

    public ServerActionImpl(PeerServer peerServer) {
        this.peerServer = peerServer;
    }

    @Override
    public void newClientConnection(UniqueIdentifier clientID, ChannelConnectionPoint ccp, IP4Port ip4Port) {
        // set the two FSMs (connection and request dispatcher)
        // in the future, for every software acting as server and offering services, there should be one channel
        // where they hear requests for new services. This FSM with only one state (in one step solves every request)
        // is able to create a service in a random channel, and report the client to use that channel. This will be the
        // only non-timed FSM, because the rest are created on request.
        if (peerServer.isRunning()) {
            ccp.registerTimedFSM(new ServerConnectionFSM(peerServer, ip4Port.getIp(), clientID), PeerServer.CONNECTION_TIMEOUT_MILLIS, "ServerConnectionFSM", PeerServer.CONNECTION_CHANNEL);
            ccp.registerTimedFSM(new RequestDispatcherFSM(peerServer, clientID), PeerServer.TIMEOUT_MILLIS, PeerServer.REQUESTS_CHANNEL);
            // todo parametrize timeout time and tell the client its value during the communication process. FOR TCP HOLE!
            // also transmit server metadata (name, capacity, current users, ...)
        }
    }

    @Override
    public void newMessage(UniqueIdentifier clientID, ChannelConnectionPoint ccp, byte channel, Object message) {
        // messages at this level are unexpected
        peerServer.clientBehavingBad(clientID);
    }

    @Override
    public void newMessage(UniqueIdentifier clientID, ChannelConnectionPoint ccp, byte channel, byte[] data) {
        // messages at this level are unexpected
        peerServer.clientBehavingBad(clientID);
    }

    @Override
    public void channelsFreed(UniqueIdentifier clientID, ChannelConnectionPoint ccp, Set<Byte> channels) {
        // ignore
    }


    @Override
    public void clientDisconnected(UniqueIdentifier clientID, ChannelConnectionPoint ccp, boolean expected) {
        peerServer.clientDisconnected(clientID);
    }

    @Override
    public void clientError(UniqueIdentifier clientID, ChannelConnectionPoint ccp, CommError e) {
        peerServer.clientConnectionError(clientID, e);
    }

    @Override
    public void newConnectionError(Exception e, IP4Port ip4Port) {
        peerServer.failedConnectionWithNewClient(e, ip4Port);
    }

    @Override
    public void TCPServerError(Exception e) {
        peerServer.TCPServerFailed(e);
    }
}
