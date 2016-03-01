package jacz.peerengineservice.client;

import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.util.concurrency.task_executor.SequentialTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridge for general events
 */
public class GeneralEventsBridge implements GeneralEvents {

    final static Logger logger = LoggerFactory.getLogger(GeneralEvents.class);

    private final GeneralEvents generalEvents;

    private final SequentialTaskExecutor sequentialTaskExecutor;

    public GeneralEventsBridge(GeneralEvents generalEvents) {
        this.generalEvents = generalEvents;
        sequentialTaskExecutor = new SequentialTaskExecutor();
    }

    @Override
    public void peerAddedAsFriend(final PeerId peerId, final PeerRelations peerRelations) {
        logger.info("PEER ADDED AS FRIEND. Peer: " + peerId);
        sequentialTaskExecutor.executeTask(new Runnable() {
            @Override
            public void run() {
                generalEvents.peerAddedAsFriend(peerId, peerRelations);
            }
        });
    }

    @Override
    public void peerRemovedAsFriend(final PeerId peerId, final PeerRelations peerRelations) {
        logger.info("PEER REMOVED AS FRIEND. Peer: " + peerId);
        sequentialTaskExecutor.executeTask(new Runnable() {
            @Override
            public void run() {
                generalEvents.peerRemovedAsFriend(peerId, peerRelations);
            }
        });
    }

    @Override
    public void peerAddedAsBlocked(final PeerId peerId, final PeerRelations peerRelations) {
        logger.info("PEER ADDED AS BLOCKED. Peer: " + peerId);
        sequentialTaskExecutor.executeTask(new Runnable() {
            @Override
            public void run() {
                generalEvents.peerAddedAsBlocked(peerId, peerRelations);
            }
        });
    }

    @Override
    public void peerRemovedAsBlocked(final PeerId peerId, final PeerRelations peerRelations) {
        logger.info("PEER REMOVED AS BLOCKED. Peer: " + peerId);
        sequentialTaskExecutor.executeTask(new Runnable() {
            @Override
            public void run() {
                generalEvents.peerRemovedAsBlocked(peerId, peerRelations);
            }
        });
    }

    @Override
    public void newPeerConnected(final PeerId peerId, final ConnectionStatus status) {
        logger.info("NEW PEER CONNECTED. Peer: " + peerId + ". Status: " + status);
        sequentialTaskExecutor.executeTask(new Runnable() {
            @Override
            public void run() {
                generalEvents.newPeerConnected(peerId, status);
            }
        });
    }

    @Override
    public void newObjectMessage(final PeerId peerId, final Object message) {
        logger.info("NEW OBJECT MESSAGE. Peer: " + peerId + ". Message: " + message.toString());
        sequentialTaskExecutor.executeTask(new Runnable() {
            @Override
            public void run() {
                generalEvents.newObjectMessage(peerId, message);
            }
        });
    }

    @Override
    public void newPeerNick(final PeerId peerId, final String nick) {
        logger.info("NEW PEER NICK. Peer: " + peerId + ". Nick: " + nick);
        sequentialTaskExecutor.executeTask(new Runnable() {
            @Override
            public void run() {
                generalEvents.newPeerNick(peerId, nick);
            }
        });
    }

    @Override
    public void peerValidatedUs(final PeerId peerId) {
        logger.info("PEER VALIDATED US. Peer: " + peerId);
        sequentialTaskExecutor.executeTask(new Runnable() {
            @Override
            public void run() {
                generalEvents.peerValidatedUs(peerId);
            }
        });
    }

    @Override
    public void peerDisconnected(final PeerId peerId, final CommError error) {
        logger.info("PEER DISCONNECTED. Peer: " + peerId + ". Error: " + error);
        sequentialTaskExecutor.executeTask(new Runnable() {
            @Override
            public void run() {
                generalEvents.peerDisconnected(peerId, error);
            }
        });
    }

    @Override
    public void stop() {
        logger.info("STOP");
        sequentialTaskExecutor.executeTask(new Runnable() {
            @Override
            public void run() {
                generalEvents.stop();
            }
        });
        sequentialTaskExecutor.stopAndWaitForFinalization();
    }

}
