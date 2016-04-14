package jacz.peerengineservice.client.connection.peers;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.connection.ConnectedPeers;
import jacz.peerengineservice.client.connection.peers.kb.Management;
import jacz.peerengineservice.client.connection.peers.kb.PeerEntryFacade;
import jacz.peerengineservice.client.connection.peers.kb.PeerKnowledgeBase;
import jacz.util.AI.evolve.EvolvingState;
import jacz.util.AI.evolve.EvolvingStateController;

import java.util.HashSet;
import java.util.Set;

/**
 * This class performs regular checks for peers to which we should not be connected (blocked, or regular and no wish
 * for regular connections, or too many connections, or undesired countries...)
 */
public class DisconnectionsManager {

    enum Goal {
        SINGLE_GOAL
    }

    enum State {
        SINGLE_STATE
    }

    private static final long MIN_RETRY = 1000L;

    private static final long MAX_RETRY = 90000L;

    private long currentRetry;

    private final PeerConnectionManager peerConnectionManager;

    private final ConnectedPeers connectedPeers;

    private final PeerKnowledgeBase peerKnowledgeBase;

    private final EvolvingState<State, Goal> dynamicState;

    public DisconnectionsManager(PeerConnectionManager peerConnectionManager, ConnectedPeers connectedPeers, PeerKnowledgeBase peerKnowledgeBase) {
        this.peerConnectionManager = peerConnectionManager;
        this.connectedPeers = connectedPeers;
        this.peerKnowledgeBase = peerKnowledgeBase;
        dynamicState = new EvolvingState<>(State.SINGLE_STATE, Goal.SINGLE_GOAL, new EvolvingState.Transitions<State, Goal>() {
            @Override
            public boolean runTransition(State state, Goal goal, EvolvingStateController<State, Goal> controller) {
                checkPeersToDisconnect();
                updateRetryTime();
                return true;
            }

            @Override
            public boolean hasReachedGoal(State state, Goal goal) {
                return true;
            }
        }, "DisconnectionsManager");
        setRetryTime(MAX_RETRY);
    }

    private void checkPeersToDisconnect() {
        Set<PeerId> peersToDisconnect = new HashSet<>();
        for (PeerId peerId : connectedPeers.getConnectedPeers()) {
            PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(peerId);

            // BLOCKED peers
            if (peerEntryFacade.getRelationship() == Management.Relationship.BLOCKED) {
                peersToDisconnect.add(peerId);
                continue;
            }

            // REGULAR peers, and we do not wish to connect to regulars
            if (peerEntryFacade.getRelationship() == Management.Relationship.REGULAR && !peerConnectionManager.isOwnWishForRegularConnections()) {
                peersToDisconnect.add(peerId);
                continue;
            }

            // todo add conditions for too many connections, or undesired countries
        }
        mustDisconnectFromPeers(peersToDisconnect);
    }

    synchronized void mustDisconnectFromPeers(Set<PeerId> peerIds) {
        for (PeerId peerId : peerIds) {
            connectedPeers.disconnectPeer(peerId);
        }
    }

    private void updateRetryTime() {
        setRetryTime(Math.min(currentRetry * 2, MAX_RETRY));
    }

    private void setRetryTime(long time) {
        dynamicState.setEvolveStateTimer(state -> true, MIN_RETRY);
        currentRetry = time;
    }

    public void checkDisconnections() {
        setRetryTime(MIN_RETRY);
    }

    public void stop() {
        dynamicState.stop();
    }
}
