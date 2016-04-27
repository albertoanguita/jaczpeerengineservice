package jacz.peerengineservice.client.connection.peers;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.connection.peers.kb.PeerEntryFacade;
import jacz.peerengineservice.client.connection.peers.kb.PeerKnowledgeBase;
import jacz.util.AI.evolve.EvolvingState;
import jacz.util.AI.evolve.EvolvingStateController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the connection to favorite peers
 */
public class FavoritesConnectionManager {

    enum State {
        SINGLE_STATE
    }

    private final static Logger logger = LoggerFactory.getLogger(FavoritesConnectionManager.class);

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
                    searchFriends();
                    connectToFavoritePeers();
                }
                return true;
            }

            @Override
            public boolean hasReachedGoal(State state, Boolean goal) {
                return true;
            }
        }, "FavoritesConnectionManager");
        dynamicState.setEvolveStateTimer(state -> true, RETRY);
    }

    /**
     * Performs a connected friend search. Searches are performed periodically, but the user can force a search using this method. If we are not
     * connected to the server, this method will have no effect
     */
    private void searchFriends() {
        // check if there are disconnected favorite peers of which we do not have address info
        peerConnectionManager.askForFavoritePeersInfo(buildNeedInfoFavoriteList());
    }

    private List<PeerId> buildNeedInfoFavoriteList() {
        List<PeerId> needInfoFavorites = new ArrayList<>();
        for (PeerEntryFacade peerEntryFacade : peerKnowledgeBase.getFavoritePeers(PeerKnowledgeBase.ConnectedQuery.DISCONNECTED)) {
            if (peerEntryFacade.getPeerAddress().isNull()) {
                // add this peer to the list of favorite peers for which we need to query their address
                needInfoFavorites.add(peerEntryFacade.getPeerId());
            }
        }
        return needInfoFavorites;
    }

    private void connectToFavoritePeers() {
        for (PeerEntryFacade peerEntryFacade : peerKnowledgeBase.getFavoritePeers(PeerKnowledgeBase.ConnectedQuery.DISCONNECTED)) {
            logger.info("Trying to connect to favorite peer: " + peerEntryFacade.getPeerId());
            // go through all disconnected favorite peers and try to connect to them
            if (!peerConnectionManager.discardConnectionAttempt(peerEntryFacade)) {
                logger.info("Attempt to connect with favorite peer: " + peerEntryFacade.getPeerId());
                peerConnectionManager.attemptConnection(peerEntryFacade);
            }
        }
    }

    public void setConnectionGoal(boolean connect) {
        dynamicState.setGoal(connect);
    }

    public void searchFavoritesNow() {
        dynamicState.evolve();
    }

    public void stop() {
        setConnectionGoal(false);
        dynamicState.blockUntilGoalReached(500);
        dynamicState.stop();
    }
}
