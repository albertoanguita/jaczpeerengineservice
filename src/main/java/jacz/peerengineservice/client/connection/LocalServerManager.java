package jacz.peerengineservice.client.connection;

import jacz.peerengineservice.client.PeerClientPrivateInterface;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.clientserver.server.ServerAction;
import jacz.commengine.clientserver.server.ServerModule;
import jacz.commengine.communication.CommError;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.concurrency.daemon.Daemon;
import jacz.util.concurrency.daemon.DaemonAction;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.network.IP4Port;

import java.util.Set;

/**
 * This class manages the server for listening to other peers connecting to us
 */
public class LocalServerManager implements DaemonAction {

    /**
     * Used by the ServerModule of the PeerClientConnectionManager, to listen to new connections from other peers. This
     * class also handles every event related to used ChannelConnectionPoints of peers currently connected to use
     * (received messages, freed channels, etc). Received messages are ignored since every communication with other
     * peers is carried out through FSMs, not through direct messaging. The freeing of a channel does have to be notified
     * to the PeerClient.
     */
    private static class PeerClientServerActionImpl implements ServerAction {

        private final FriendConnectionManager friendConnectionManager;

        private final PeerClientPrivateInterface peerClientPrivateInterface;

        /**
         * Class constructor
         *
         * @param friendConnectionManager the FriendConnectionManager that handles friends events
         */
        public PeerClientServerActionImpl(
                FriendConnectionManager friendConnectionManager,
                PeerClientPrivateInterface peerClientPrivateInterface) {
            this.friendConnectionManager = friendConnectionManager;
            this.peerClientPrivateInterface = peerClientPrivateInterface;
        }

        @Override
        public void newClientConnection(UniqueIdentifier clientID, ChannelConnectionPoint ccp, IP4Port ip4Port) {
            friendConnectionManager.reportClientConnectedToOurPeerServer(ccp);
        }

        @Override
        public void newMessage(UniqueIdentifier clientID, ChannelConnectionPoint ccp, byte channel, Object message) {
            // ignore
        }

        @Override
        public void newMessage(UniqueIdentifier clientID, ChannelConnectionPoint ccp, byte channel, byte[] data) {
            // ignore
        }

        @Override
        public void channelsFreed(UniqueIdentifier clientID, ChannelConnectionPoint ccp, Set<Byte> channels) {
            friendConnectionManager.channelsFreed(ccp, channels);
        }

        @Override
        public void clientDisconnected(UniqueIdentifier clientID, ChannelConnectionPoint ccp, boolean expected) {
            friendConnectionManager.peerDisconnected(ccp);
        }

        @Override
        public void clientError(UniqueIdentifier clientID, ChannelConnectionPoint ccp, CommError e) {
            friendConnectionManager.peerError(ccp, e);
        }

        @Override
        public void newConnectionError(Exception e, IP4Port ip4Port) {
            peerClientPrivateInterface.peerCouldNotConnectToUs(e, ip4Port);
        }

        @Override
        public void TCPServerError(Exception e) {
            peerClientPrivateInterface.localServerError(e);
        }
    }

    private final FriendConnectionManager friendConnectionManager;

    private final PeerClientPrivateInterface peerClientPrivateInterface;

    /**
     * ServerModule object employed to receive connections from friend peers
     */
    private ServerModule serverModule;

    private boolean wishForConnect;

    /**
     * Daemon thread that helps keep the state in its desired values
     */
    private final Daemon stateDaemon;

    /**
     * Status of the server for listening to incoming peer connections
     */
    private State.LocalServerConnectionsState localServerConnectionsState;

    /**
     * Collection of all information related to connection to the server, as provided by the user
     */
    private ConnectionInformation wishedConnectionInformation;

    /**
     * Collection of all information related to connection to the server (what we are currently using)
     */
    private int listeningPort;


    public LocalServerManager(
            FriendConnectionManager friendConnectionManager,
            PeerClientPrivateInterface peerClientPrivateInterface,
            ConnectionInformation wishedConnectionInformation) {
        this.friendConnectionManager = friendConnectionManager;
        this.peerClientPrivateInterface = peerClientPrivateInterface;
        serverModule = null;
        wishForConnect = false;
        stateDaemon = new Daemon(this);
        localServerConnectionsState = State.LocalServerConnectionsState.CLOSED;
        this.wishedConnectionInformation = wishedConnectionInformation;
        listeningPort = -1;
    }

    synchronized void setWishForConnect(boolean wishForConnect) {
        boolean mustUpdateState = this.wishForConnect != wishForConnect;
        this.wishForConnect = wishForConnect;
        if (mustUpdateState) {
            updatedState();
        }
    }

    synchronized boolean isInWishedState() {
        if (wishForConnect) {
            return localServerConnectionsState == State.LocalServerConnectionsState.OPEN && isCorrectConnectionInformation();
        } else {
            return localServerConnectionsState == State.LocalServerConnectionsState.CLOSED;
        }
    }

    private boolean isCorrectConnectionInformation() {
        return listeningPort == wishedConnectionInformation.getListeningPort();
    }

    void stop() {
        setWishForConnect(false);
        // active wait until the local server is closed
        boolean mustWait;
        synchronized (this) {
            mustWait = localServerConnectionsState != State.LocalServerConnectionsState.CLOSED;
        }
        while (mustWait) {
            ThreadUtil.safeSleep(100L);
            synchronized (this) {
                mustWait = localServerConnectionsState != State.LocalServerConnectionsState.CLOSED;
            }
        }
    }

    synchronized void updatedState() {
        stateDaemon.stateChange();
        stateDaemon.interrupt();
    }

    @Override
    public synchronized boolean solveState() {
        if (wishForConnect) {
            // first open the server for listening to incoming peer connections, then connect to the peer server
            if (localServerConnectionsState == State.LocalServerConnectionsState.OPEN) {
                // check the correct listening port is being used
                if (!isCorrectConnectionInformation()) {
                    closePeerConnectionsServer();
                    return false;
                }
            }
            if (localServerConnectionsState == State.LocalServerConnectionsState.CLOSED) {
                // open the server for listening connections from other peers
                openPeerConnectionsServer();
                return false;
            }
        } else {
            if (localServerConnectionsState == State.LocalServerConnectionsState.OPEN) {
                // we must close our server and kick all connected clients
                closePeerConnectionsServer();
                return false;
            }
        }
        return true;
    }


    synchronized void openPeerConnectionsServer() {
        serverModule = new ServerModule(wishedConnectionInformation.getListeningPort(), new PeerClientServerActionImpl(friendConnectionManager, peerClientPrivateInterface), PeerClientConnectionManager.generateConcurrentChannelSets());
        serverModule.startListeningConnections();
        listeningPort = wishedConnectionInformation.getListeningPort();
        localServerConnectionsState = State.LocalServerConnectionsState.OPEN;
        peerClientPrivateInterface.localServerOpen(listeningPort, localServerConnectionsState);
    }

    synchronized void closePeerConnectionsServer() {
        if (serverModule != null) {
            serverModule.stopListeningConnections();
        }
        localServerConnectionsState = State.LocalServerConnectionsState.CLOSED;
        peerClientPrivateInterface.localServerClosed(listeningPort, localServerConnectionsState);
    }
}
