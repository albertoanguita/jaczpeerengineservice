package jacz.peerengineservice.client.connection.peers;

import jacz.peerengineservice.client.connection.peers.kb.PeerEntryFacade;
import jacz.peerengineservice.client.connection.peers.kb.PeerKnowledgeBase;
import jacz.util.AI.evolve.EvolvingState;
import jacz.util.AI.evolve.EvolvingStateController;

/**
 * Handles the connection to favorite peers
 */
public class FavoritesConnectionManager {

    enum State {
        SINGLE_STATE
    }

    private static final long RETRY = 90000L;

    private final PeerConnectionManager peerConnectionManager;

    private final PeerKnowledgeBase peerKnowledgeBase;

    private final EvolvingState<State, Boolean> dynamicState;

    public FavoritesConnectionManager(PeerConnectionManager peerConnectionManager, PeerKnowledgeBase peerKnowledgeBase) {
        this.peerConnectionManager = peerConnectionManager;
        this.peerKnowledgeBase = peerKnowledgeBase;
        dynamicState = new EvolvingState<>(State.SINGLE_STATE, false, new EvolvingState.Transitions<State, Boolean>() {
            @Override
            public boolean runTransition(State state, Boolean goal, EvolvingStateController<State, Boolean> controller) {
                if (goal) {
                    // trying to connect to favorite peers
                    connectToFavoritePeers();
                }
                return true;
            }

            @Override
            public boolean hasReachedGoal(State state, Boolean goal) {
                return true;
            }
        });
        dynamicState.setEvolveStateTimer(state -> true, RETRY);
    }

    private void connectToFavoritePeers() {
        for (PeerEntryFacade peerEntryFacade : peerKnowledgeBase.getFavoritePeers(PeerKnowledgeBase.ConnectedQuery.DISCONNECTED)) {
            // go through all disconnected favorite peers and try to connect to them
            if (!peerConnectionManager.discardConnectionAttempt(peerEntryFacade)) {
                peerConnectionManager.attemptConnection(peerEntryFacade);
            }
        }
    }

    public void setConnectionGoal(boolean connect) {
        dynamicState.setGoal(connect);
    }

    public void stop() {
        setConnectionGoal(false);
        dynamicState.blockUntilGoalReached(500);
        dynamicState.stop();
    }
}
