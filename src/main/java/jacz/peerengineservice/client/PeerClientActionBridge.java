package jacz.peerengineservice.client;

import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.connection.ClientConnectionToServerFSM;
import jacz.peerengineservice.client.connection.State;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.peerengineservice.util.datatransfer.DownloadsManager;
import jacz.peerengineservice.util.datatransfer.UploadsManager;
import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.concurrency.task_executor.SequentialTaskExecutor;
import jacz.util.network.IP4Port;

/**
 * Created by Alberto on 11/10/2015.
 */
public class PeerClientActionBridge implements PeerClientAction {

    private final PeerClientAction peerClientAction;

    private final SequentialTaskExecutor sequentialTaskExecutor;

    public PeerClientActionBridge(PeerClientAction peerClientAction) {
        this.peerClientAction = peerClientAction;
        sequentialTaskExecutor = new SequentialTaskExecutor();
    }

    @Override
    public void peerAddedAsFriend(final PeerID peerID, final PeerRelations peerRelations) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.peerAddedAsFriend(peerID, peerRelations);
            }
        });
    }

    @Override
    public void peerRemovedAsFriend(final PeerID peerID, final PeerRelations peerRelations) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.peerRemovedAsFriend(peerID, peerRelations);
            }
        });
    }

    @Override
    public void peerAddedAsBlocked(final PeerID peerID, final PeerRelations peerRelations) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.peerAddedAsBlocked(peerID, peerRelations);
            }
        });
    }

    @Override
    public void peerRemovedAsBlocked(final PeerID peerID, final PeerRelations peerRelations) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.peerRemovedAsBlocked(peerID, peerRelations);
            }
        });
    }

    @Override
    public void newPeerConnected(final PeerID peerID, final ConnectionStatus status) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.newPeerConnected(peerID, status);
            }
        });
    }

    @Override
    public void newObjectMessage(final PeerID peerID, final Object message) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.newObjectMessage(peerID, message);
            }
        });
    }

    @Override
    public void newPeerNick(final PeerID peerID, final String nick) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.newPeerNick(peerID, nick);
            }
        });
    }

    @Override
    public void peerValidatedUs(final PeerID peerID) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.peerValidatedUs(peerID);
            }
        });
    }

    @Override
    public void peerDisconnected(final PeerID peerID, final CommError error) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.peerDisconnected(peerID, error);
            }
        });
    }

    @Override
    public void listeningPortModified(final int port) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.listeningPortModified(port);
            }
        });
    }

    @Override
    public void tryingToConnectToServer(final PeerServerData peerServerData, final State state) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.tryingToConnectToServer(peerServerData, state);
            }
        });
    }

    @Override
    public void connectionToServerEstablished(final PeerServerData peerServerData, final State state) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.connectionToServerEstablished(peerServerData, state);
            }
        });
    }

    @Override
    public void unableToConnectToServer(final PeerServerData peerServerData, final State state) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.unableToConnectToServer(peerServerData, state);
            }
        });
    }

    @Override
    public void serverTookToMuchTimeToAnswerConnectionRequest(final PeerServerData peerServerData, final State state) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.serverTookToMuchTimeToAnswerConnectionRequest(peerServerData, state);
            }
        });
    }

    @Override
    public void connectionToServerDenied(final PeerServerData peerServerData, final ClientConnectionToServerFSM.ConnectionFailureReason reason, final State state) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.connectionToServerDenied(peerServerData, reason, state);
            }
        });
    }

    @Override
    public void connectionToServerTimedOut(final PeerServerData peerServerData, final State state) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.connectionToServerTimedOut(peerServerData, state);
            }
        });
    }

    @Override
    public void localServerOpen(final int port, final State state) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.localServerOpen(port, state);
            }
        });
    }

    @Override
    public void localServerClosed(final int port, final State state) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.localServerClosed(port, state);
            }
        });
    }

    @Override
    public void disconnectedFromServer(final boolean expected, final PeerServerData peerServerData, final State state) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.disconnectedFromServer(expected, peerServerData, state);
            }
        });
    }

    @Override
    public void undefinedOwnInetAddress() {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.undefinedOwnInetAddress();
            }
        });
    }

    @Override
    public void peerCouldNotConnectToUs(final Exception e, final IP4Port ip4Port) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.peerCouldNotConnectToUs(e, ip4Port);
            }
        });
    }

    @Override
    public void localServerError(final Exception e) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.localServerError(e);
            }
        });
    }

    @Override
    public void periodicDownloadsNotification(final DownloadsManager downloadsManager) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.periodicDownloadsNotification(downloadsManager);
            }
        });
    }

    @Override
    public void periodicUploadsNotification(final UploadsManager uploadsManager) {
        // todo log
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClientAction.periodicUploadsNotification(uploadsManager);
            }
        });
    }
}
