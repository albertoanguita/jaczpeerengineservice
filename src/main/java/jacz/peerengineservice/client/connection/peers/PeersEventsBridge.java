package jacz.peerengineservice.client.connection.peers;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerClientPrivateInterface;
import jacz.peerengineservice.client.connection.peers.kb.Management;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bridge class for user-provided peers events
 */
class PeersEventsBridge {

    private final static Logger logger = LoggerFactory.getLogger(PeersEvents.class);

    private final PeersEvents peersEvents;

    private final PeerClientPrivateInterface peerClientPrivateInterface;

    private final ExecutorService sequentialTaskExecutor;

    PeersEventsBridge(PeersEvents peersEvents, PeerClientPrivateInterface peerClientPrivateInterface) {
        this.peersEvents = peersEvents;
        this.peerClientPrivateInterface = peerClientPrivateInterface;
        sequentialTaskExecutor = Executors.newSingleThreadExecutor();
    }

    void newPeerConnected(final PeerId peerId, ChannelConnectionPoint ccp, PeerInfo peerInfo) {
        logger.info("NEW PEER CONNECTED. Peer: " + peerId + ". Info: " + peerInfo);
        peerClientPrivateInterface.newPeerConnected(peerId, ccp, peerInfo.relationship);
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                peersEvents.newPeerConnected(peerId, peerInfo);
            }
        });
    }

    void modifiedPeerRelationship(PeerId peerId, Management.Relationship relationship, PeerInfo peerInfo, boolean mustNotifyOtherPeer) {
        logger.info("MODIFIED PEER RELATIONSHIP. Peer: " + peerId + ". Info: " + peerInfo);
        if (mustNotifyOtherPeer) {
            peerClientPrivateInterface.modifiedPeerRelationship(peerId, relationship);
        }
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                peersEvents.modifiedPeerRelationship(peerId, peerInfo);
            }
        });
    }

    void modifiedMainCountry(PeerId peerId, PeerInfo peerInfo) {
        logger.info("MODIFIED MAIN COUNTRY. Peer: " + peerId + ". Info: " + peerInfo);
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                peersEvents.modifiedMainCountry(peerId, peerInfo);
            }
        });
    }

    void modifiedAffinity(PeerId peerId, PeerInfo peerInfo) {
        logger.info("MODIFIED PEER RELATIONSHIP. Peer: " + peerId + ". Info: " + peerInfo);
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                peersEvents.modifiedAffinity(peerId, peerInfo);
            }
        });
    }

    void newPeerNick(final PeerId peerId, PeerInfo peerInfo) {
        logger.info("NEW PEER NICK. Peer: " + peerId + ". Info: " + peerInfo);
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                peersEvents.newPeerNick(peerId, peerInfo);
            }
        });
    }

    void peerDisconnected(final PeerId peerId, PeerInfo peerInfo, final CommError error) {
        logger.info("PEER DISCONNECTED. Peer: " + peerId + ". Info: " + peerInfo + ". Error: " + error);
        peerClientPrivateInterface.peerDisconnected(peerId, error);
        sequentialTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                peersEvents.peerDisconnected(peerId, peerInfo, error);
            }
        });
    }

    void stop() {
        sequentialTaskExecutor.shutdown();
    }
}
