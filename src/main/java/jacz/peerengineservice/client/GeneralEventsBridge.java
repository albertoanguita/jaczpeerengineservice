package jacz.peerengineservice.client;

import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.concurrency.task_executor.SequentialTaskExecutor;
import org.apache.log4j.Logger;

/**
 * Bridge for general events
 */
public class GeneralEventsBridge implements GeneralEvents {

    final static Logger logger = Logger.getLogger(GeneralEvents.class);

    private final GeneralEvents generalEvents;

    private final SequentialTaskExecutor sequentialTaskExecutor;

    public GeneralEventsBridge(GeneralEvents generalEvents) {
        this.generalEvents = generalEvents;
        sequentialTaskExecutor = new SequentialTaskExecutor();
    }

    @Override
    public void peerAddedAsFriend(final PeerID peerID, final PeerRelations peerRelations) {
        logger.info("PEER ADDED AS FRIEND. Peer: " + peerID);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                generalEvents.peerAddedAsFriend(peerID, peerRelations);
            }
        });
    }

    @Override
    public void peerRemovedAsFriend(final PeerID peerID, final PeerRelations peerRelations) {
        logger.info("PEER REMOVED AS FRIEND. Peer: " + peerID);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                generalEvents.peerRemovedAsFriend(peerID, peerRelations);
            }
        });
    }

    @Override
    public void peerAddedAsBlocked(final PeerID peerID, final PeerRelations peerRelations) {
        logger.info("PEER ADDED AS BLOCKED. Peer: " + peerID);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                generalEvents.peerAddedAsBlocked(peerID, peerRelations);
            }
        });
    }

    @Override
    public void peerRemovedAsBlocked(final PeerID peerID, final PeerRelations peerRelations) {
        logger.info("PEER REMOVED AS BLOCKED. Peer: " + peerID);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                generalEvents.peerRemovedAsBlocked(peerID, peerRelations);
            }
        });
    }

    @Override
    public void newPeerConnected(final PeerID peerID, final ConnectionStatus status) {
        logger.info("NEW PEER CONNECTED. Peer: " + peerID + ". Status: " + status);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                generalEvents.newPeerConnected(peerID, status);
            }
        });
    }

    @Override
    public void newObjectMessage(final PeerID peerID, final Object message) {
        logger.info("NEW OBJECT MESSAGE. Peer: " + peerID + ". Message: " + message.toString());
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                generalEvents.newObjectMessage(peerID, message);
            }
        });
    }

    @Override
    public void newPeerNick(final PeerID peerID, final String nick) {
        logger.info("NEW PEER NICK. Peer: " + peerID + ". Nick: " + nick);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                generalEvents.newPeerNick(peerID, nick);
            }
        });
    }

    @Override
    public void peerValidatedUs(final PeerID peerID) {
        logger.info("PEER VALIDATED US. Peer: " + peerID);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                generalEvents.peerValidatedUs(peerID);
            }
        });
    }

    @Override
    public void peerDisconnected(final PeerID peerID, final CommError error) {
        logger.info("PEER DISCONNECTED. Peer: " + peerID + ". Error: " + error);
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                generalEvents.peerDisconnected(peerID, error);
            }
        });
    }

    @Override
    public void stop() {
        logger.info("STOP");
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                generalEvents.stop();
            }
        });
        sequentialTaskExecutor.stopAndWaitForFinalization();
    }

}
