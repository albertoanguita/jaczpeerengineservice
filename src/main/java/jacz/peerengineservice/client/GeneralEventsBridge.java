package jacz.peerengineservice.client;

import jacz.peerengineservice.PeerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bridge for general events
 */
public class GeneralEventsBridge implements GeneralEvents {

    final static Logger logger = LoggerFactory.getLogger(GeneralEvents.class);

    private final GeneralEvents generalEvents;

    private final ExecutorService sequentialTaskExecutor;

    public GeneralEventsBridge(GeneralEvents generalEvents) {
        this.generalEvents = generalEvents;
        sequentialTaskExecutor = Executors.newSingleThreadExecutor();
    }

//    @Override
//    public void peerAddedAsFriend(final PeerId peerId) {
//        logger.info("PEER ADDED AS FRIEND. Peer: " + peerId);
//        sequentialTaskExecutor.submit(new Runnable() {
//            @Override
//            public void run() {
//                generalEvents.peerAddedAsFriend(peerId);
//            }
//        });
//    }
//
//    @Override
//    public void peerRemovedAsFriend(final PeerId peerId) {
//        logger.info("PEER REMOVED AS FRIEND. Peer: " + peerId);
//        sequentialTaskExecutor.submit(new Runnable() {
//            @Override
//            public void run() {
//                generalEvents.peerRemovedAsFriend(peerId);
//            }
//        });
//    }
//
//    @Override
//    public void peerAddedAsBlocked(final PeerId peerId) {
//        logger.info("PEER ADDED AS BLOCKED. Peer: " + peerId);
//        sequentialTaskExecutor.submit(new Runnable() {
//            @Override
//            public void run() {
//                generalEvents.peerAddedAsBlocked(peerId);
//            }
//        });
//    }
//
//    @Override
//    public void peerRemovedAsBlocked(final PeerId peerId) {
//        logger.info("PEER REMOVED AS BLOCKED. Peer: " + peerId);
//        sequentialTaskExecutor.submit(new Runnable() {
//            @Override
//            public void run() {
//                generalEvents.peerRemovedAsBlocked(peerId);
//            }
//        });
//    }
//
//    @Override
//    public void newPeerConnected(final PeerId peerId, PeerRelationship peerRelationship) {
//        logger.info("NEW PEER CONNECTED. Peer: " + peerId + ". Relationship: " + peerRelationship);
//        sequentialTaskExecutor.submit(new Runnable() {
//            @Override
//            public void run() {
//                generalEvents.newPeerConnected(peerId, peerRelationship);
//            }
//        });
//    }
//
//    @Override
//    public void modifiedPeerRelationship(PeerId peerId, PeerRelationship peerRelationship, boolean connected) {
//        logger.info("MODIFIED PEER RELATIONSHIP. Peer: " + peerId + ". Relationship: " + peerRelationship + ". Connected: " + connected);
//        sequentialTaskExecutor.submit(new Runnable() {
//            @Override
//            public void run() {
//                generalEvents.modifiedPeerRelationship(peerId, peerRelationship, connected);
//            }
//        });
//    }

    @Override
    public void newOwnNick(String newNick) {
        logger.info("NEW OWN NICK: " + newNick);
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                generalEvents.newOwnNick(newNick);
            }
        });
    }

    @Override
    public void newObjectMessage(final PeerId peerId, final Object message) {
        logger.info("NEW OBJECT MESSAGE. Peer: " + peerId + ". Message: " + message.toString());
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                generalEvents.newObjectMessage(peerId, message);
            }
        });
    }

//    @Override
//    public void newPeerNick(final PeerId peerId, final String nick) {
//        logger.info("NEW PEER NICK. Peer: " + peerId + ". Nick: " + nick);
//        sequentialTaskExecutor.submit(new Runnable() {
//            @Override
//            public void run() {
//                generalEvents.newPeerNick(peerId, nick);
//            }
//        });
//    }
//
//    @Override
//    public void peerDisconnected(final PeerId peerId, final CommError error) {
//        logger.info("PEER DISCONNECTED. Peer: " + peerId + ". Error: " + error);
//        sequentialTaskExecutor.submit(new Runnable() {
//            @Override
//            public void run() {
//                generalEvents.peerDisconnected(peerId, error);
//            }
//        });
//    }

    @Override
    public void stop() {
        logger.info("STOP");
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                generalEvents.stop();
            }
        });
        sequentialTaskExecutor.shutdown();
    }

}
