package jacz.peerengineservice.client;

import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.connection.State;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.peerengineservice.util.datatransfer.DownloadsManager;
import jacz.peerengineservice.util.datatransfer.UploadsManager;
import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.concurrency.task_executor.SequentialTaskExecutor;
import jacz.util.network.IP4Port;
import org.apache.log4j.Logger;

/**
 * Bridge class for logging and handling threads in PeerClientAction calls
 */
public class PeerClientActionBridge implements PeerClientAction {

    final static Logger logger = Logger.getLogger(PeerClientAction.class);

    private final PeerClientAction peerClientAction;

    private final SequentialTaskExecutor sequentialTaskExecutor;

    public PeerClientActionBridge(PeerClientAction peerClientAction) {
        this.peerClientAction = peerClientAction;
        sequentialTaskExecutor = new SequentialTaskExecutor();
    }

    @Override
    public void peerAddedAsFriend(final PeerID peerID, final PeerRelations peerRelations) {
        logger.info("PEER ADDED AS FRIEND. Peer: " + peerID);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.peerAddedAsFriend(peerID, peerRelations);
            }
        });
    }

    @Override
    public void peerRemovedAsFriend(final PeerID peerID, final PeerRelations peerRelations) {
        logger.info("PEER REMOVED AS FRIEND. Peer: " + peerID);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.peerRemovedAsFriend(peerID, peerRelations);
            }
        });
    }

    @Override
    public void peerAddedAsBlocked(final PeerID peerID, final PeerRelations peerRelations) {
        logger.info("PEER ADDED AS BLOCKED. Peer: " + peerID);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.peerAddedAsBlocked(peerID, peerRelations);
            }
        });
    }

    @Override
    public void peerRemovedAsBlocked(final PeerID peerID, final PeerRelations peerRelations) {
        logger.info("PEER REMOVED AS BLOCKED. Peer: " + peerID);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.peerRemovedAsBlocked(peerID, peerRelations);
            }
        });
    }

    @Override
    public void newPeerConnected(final PeerID peerID, final ConnectionStatus status) {
        logger.info("NEW PEER CONNECTED. Peer: " + peerID + ". Status: " + status);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.newPeerConnected(peerID, status);
            }
        });
    }

    @Override
    public void newObjectMessage(final PeerID peerID, final Object message) {
        logger.info("NEW OBJECT MESSAGE. Peer: " + peerID + ". Message: " + message.toString());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.newObjectMessage(peerID, message);
            }
        });
    }

    @Override
    public void newPeerNick(final PeerID peerID, final String nick) {
        logger.info("NEW PEER NICK. Peer: " + peerID + ". Nick: " + nick);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.newPeerNick(peerID, nick);
            }
        });
    }

    @Override
    public void peerValidatedUs(final PeerID peerID) {
        logger.info("PEER VALIDATED US. Peer: " + peerID);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.peerValidatedUs(peerID);
            }
        });
    }

    @Override
    public void peerDisconnected(final PeerID peerID, final CommError error) {
        logger.info("PEER DISCONNECTED. Peer: " + peerID + ". Error: " + error);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.peerDisconnected(peerID, error);
            }
        });
    }

    @Override
    public void listeningPortModified(final int port) {
        logger.info("LISTENING PORT MODIFIED. Port: " + port);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.listeningPortModified(port);
            }
        });
    }

    @Override
    public void initializingConnection() {
        logger.info("INITIALIZING CONNECTION");
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.initializingConnection();
            }
        });
    }

    @Override
    public void localAddressFetched(final String localAddress, final State state) {
        logger.info("LOCAL ADDRESS FETCHED. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.localAddressFetched(localAddress, state);
            }
        });
    }

    @Override
    public void couldNotFetchLocalAddress(final State state) {
        logger.info("COULD NOT FETCH LOCAL ADDRESS. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.couldNotFetchLocalAddress(state);
            }
        });
    }

    @Override
    public void tryingToFetchExternalAddress(final State state) {
        logger.info("TRYING TO FETCH EXTERNAL ADDRESS. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.tryingToFetchExternalAddress(state);
            }
        });
    }


    @Override
    public void externalAddressFetched(final String externalAddress, final boolean hasGateway, final State state) {
        logger.info("EXTERNAL ADDRESS FETCHED. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.externalAddressFetched(externalAddress, hasGateway, state);
            }
        });
    }

    @Override
    public void couldNotFetchExternalAddress(final State state) {
        logger.info("COULD NOT FETCH EXTERNAL ADDRESS. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.couldNotFetchExternalAddress(state);
            }
        });
    }

    @Override
    public void unrecognizedMessageFromServer(final State state) {
        logger.info("UNRECOGNIZED MESSAGE FROM SERVER. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.unrecognizedMessageFromServer(state);
            }
        });
    }

    @Override
    public void tryingToConnectToServer(final State state) {
        logger.info("TRYING TO CONNECT TO SERVER. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.tryingToConnectToServer(state);
            }
        });
    }

    @Override
    public void connectionToServerEstablished(final State state) {
        logger.info("CONNECTION TO SERVER ESTABLISHED. Server: \" + peerServerData + \". State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.connectionToServerEstablished(state);
            }
        });
    }

    @Override
    public void registrationRequired(final State state) {
        logger.info("REGISTRATION WITH SERVER REQUIRED. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.registrationRequired(state);
            }
        });
    }

    @Override
    public void localServerUnreachable(final State state) {
        logger.info("LOCAL SERVER UNREACHABLE. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.localServerUnreachable(state);
            }
        });
    }

    @Override
    public void unableToConnectToServer(final State state) {
        logger.info("UNABLE TO CONNECT TO SERVER. Server: \" + peerServerData + \". State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.unableToConnectToServer(state);
            }
        });
    }

    @Override
    public void disconnectedFromServer(final State state) {
        logger.info("DISCONNECTED FROM SERVER. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.disconnectedFromServer(state);
            }
        });
    }

    @Override
    public void failedToRefreshServerConnection(final State state) {
        logger.info("FAILED TO REFRESH SERVER CONNECTION. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.failedToRefreshServerConnection(state);
            }
        });
    }

    @Override
    public void tryingToRegisterWithServer(final State state) {
        logger.info("TRYING TO REGISTER WITH SERVER. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.tryingToRegisterWithServer(state);
            }
        });
    }

    @Override
    public void registrationSuccessful(final State state) {
        logger.info("REGISTRATION SUCCESSFUL. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.registrationSuccessful(state);
            }
        });
    }

    @Override
    public void alreadyRegistered(final State state) {
        logger.info("ALREADY REGISTERED. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.alreadyRegistered(state);
            }
        });
    }

    @Override
    public void tryingToOpenLocalServer(final State state) {
        logger.info("TRYING TO OPEN LOCAL SERVER. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.tryingToOpenLocalServer(state);
            }
        });
    }

    @Override
    public void localServerOpen(final State state) {
        logger.info("LOCAL SERVER OPEN. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.localServerOpen(state);
            }
        });
    }

    @Override
    public void couldNotOpenLocalServer(final State state) {
        logger.info("COULD NOT OPEN LOCAL SERVER. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.couldNotOpenLocalServer(state);
            }
        });
    }

    @Override
    public void tryingToCloseLocalServer(final State state) {
        logger.info("TRYING TO CLOSE LOCAL SERVER. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.tryingToCloseLocalServer(state);
            }
        });
    }

    @Override
    public void localServerClosed(final State state) {
        logger.info("LOCAL SERVER CLOSED. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.localServerClosed(state);
            }
        });
    }

    @Override
    public void tryingToCreateNATRule(final State state) {
        logger.info("TRYING TO CREATE NAT RULE. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.tryingToCreateNATRule(state);
            }
        });
    }

    @Override
    public void NATRuleCreated(final State state) {
        logger.info("NAT RULE CREATED. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.NATRuleCreated(state);
            }
        });
    }

    @Override
    public void couldNotFetchUPNPGateway(final State state) {
        logger.info("COULD NOT FETCH UPNP GATEWAY. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.couldNotFetchUPNPGateway(state);
            }
        });
    }

    @Override
    public void errorCreatingNATRule(final State state) {
        logger.info("ERROR CREATING NAT RULE. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.errorCreatingNATRule(state);
            }
        });
    }

    @Override
    public void tryingToDestroyNATRule(final State state) {
        logger.info("TRYING TO DESTROY NAT RULE. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.tryingToDestroyNATRule(state);
            }
        });
    }

    @Override
    public void NATRuleDestroyed(final State state) {
        logger.info("NAT RULE DESTROYED. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.NATRuleDestroyed(state);
            }
        });
    }

    @Override
    public void couldNotDestroyNATRule(final State state) {
        logger.info("COULD NOT DESTROY NAT RULE. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.couldNotDestroyNATRule(state);
            }
        });
    }

    @Override
    public void listeningConnectionsWithoutNATRule(final State state) {
        logger.info("LISTENING CONNECTIONS WITHOUT NAT RULE. State: " + state);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.listeningConnectionsWithoutNATRule(state);
            }
        });
    }

    @Override
    public void undefinedOwnInetAddress() {
        logger.info("UNDEFINED OWN INET ADDRESS");
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.undefinedOwnInetAddress();
            }
        });
    }

    @Override
    public void peerCouldNotConnectToUs(final Exception e, final IP4Port ip4Port) {
        logger.info("PEER COULD NOT CONNECT TO US. Exception: " + e.getMessage() + ". ip4Port: " + ip4Port);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.peerCouldNotConnectToUs(e, ip4Port);
            }
        });
    }

    @Override
    public void localServerError(final Exception e) {
        logger.info("LOCAL SERVER ERROR. Exception: " + e.getMessage());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.localServerError(e);
            }
        });
    }

    @Override
    public void periodicDownloadsNotification(final DownloadsManager downloadsManager) {
        // no log of active downloads
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.periodicDownloadsNotification(downloadsManager);
            }
        });
    }

    @Override
    public void periodicUploadsNotification(final UploadsManager uploadsManager) {
        // no log of active uploads
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.periodicUploadsNotification(uploadsManager);
            }
        });
    }

    @Override
    public void stop() {
        logger.info("STOP");
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.stop();
            }
        });
        sequentialTaskExecutor.stopAndWaitForFinalization();
    }
}
