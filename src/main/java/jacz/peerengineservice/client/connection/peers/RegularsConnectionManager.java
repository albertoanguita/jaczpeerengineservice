package jacz.peerengineservice.client.connection.peers;

import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineservice.client.connection.ConnectedPeers;
import jacz.peerengineservice.client.connection.peers.kb.PeerEntryFacade;
import jacz.peerengineservice.client.connection.peers.kb.PeerKnowledgeBase;
import jacz.util.AI.evolve.EvolvingState;
import jacz.util.AI.evolve.EvolvingStateController;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Handles connections to regular peers
 */
public class RegularsConnectionManager {

    private class TargetPeerList {

        private final List<PeerEntryFacade> targetPeers;

        public TargetPeerList(List<PeerEntryFacade> targetPeers) {
            this.targetPeers = targetPeers;
            filterTargetPeers();
        }

        private void filterTargetPeers() {
            // remove target peers who do not meet conditions for connecting
            Iterator<PeerEntryFacade> it = targetPeers.iterator();
            while (it.hasNext()) {
                PeerEntryFacade peerEntryFacade = it.next();
                if (peerConnectionManager.discardConnectionAttempt(peerEntryFacade)) {
                    // invalid target peer -> remove
                    it.remove();
                }
            }
        }

        public boolean isEmpty() {
            return targetPeers.isEmpty();
        }

        public List<PeerEntryFacade> retrieveTargetBatch(int maxSize) {
            List<PeerEntryFacade> targets = new ArrayList<>();
            while (!targetPeers.isEmpty() && targets.size() < TARGET_BATCH_SIZE && targets.size() < maxSize) {
                targets.add(targetPeers.remove(0));
            }
            return targets;
        }
    }

    private enum StateCase {
        IDLE,
        LOOKING_FOR_CONNECTIONS_NEED,
        ATTEMPTING_CONNECTIONS
    }

    private class State {
        StateCase stateCase;
        CountryCode currentCountry;

        public State() {
            stateCase = StateCase.IDLE;
            currentCountry = null;
        }
    }


    private static final long CONNECTIONS_DELAY = 5000L;

    private static final long GENERAL_DELAY = 2 * CONNECTIONS_DELAY;

    private static final int TARGET_BATCH_SIZE = 5;

//    private static final int MINIMUM_ATTEMPTS_FOR_REGULAR_REQUEST = 20;


    private final PeerConnectionManager peerConnectionManager;

    private final ConnectedPeers connectedPeers;

    private final PeerConnectionConfig peerConnectionConfig;

    private TargetPeerList targetPeers;

    private final EvolvingState<State, Boolean> dynamicState;

    public RegularsConnectionManager(PeerConnectionManager peerConnectionManager, PeerKnowledgeBase peerKnowledgeBase, ConnectedPeers connectedPeers, PeerConnectionConfig peerConnectionConfig) {
        this.peerConnectionManager = peerConnectionManager;
        this.connectedPeers = connectedPeers;
        this.peerConnectionConfig = peerConnectionConfig;
        dynamicState = new EvolvingState<>(new State(), false, new EvolvingState.Transitions<State, Boolean>() {
            @Override
            public boolean runTransition(State state, Boolean goal, EvolvingStateController<State, Boolean> controller) {
                if (goal && peerConnectionConfig.isWishRegularConnections()) {
                    switch (state.stateCase) {
                        case IDLE:
                            // look for some connections need
                            state.stateCase = StateCase.LOOKING_FOR_CONNECTIONS_NEED;
                            controller.stateHasChanged();
                            return false;
                        case LOOKING_FOR_CONNECTIONS_NEED:
                            // look for a language that needs more connections (start by currentLanguage)
                            if (findCountryNeedingMoreConnections(state, peerConnectionConfig)) {
                                // language found, attempt connections with it
                                targetPeers = getTargetPeers(state.currentCountry, peerKnowledgeBase);
                                if (targetPeers.isEmpty()) {
                                    // we do not have any valid regular peers for this country -> ask for more
                                    // and go back to idle
                                    peerConnectionManager.askForMoreRegularPeers(state.currentCountry);
                                    state.stateCase = StateCase.IDLE;
                                } else {
                                    // there are target peers -> try to connect with them
                                    state.stateCase = StateCase.ATTEMPTING_CONNECTIONS;
                                }
                                controller.stateHasChanged();
                                return false;
                            } else {
                                // all languages have enough connections -> go back to idle and wait
                                moveToIdle(state);
                                controller.stateHasChanged();
                                return true;
                            }
                        case ATTEMPTING_CONNECTIONS:
                            // check if we have reached the desired level of connections, or there are no more
                            // available target peers
                            if (haveEnoughConnections(state.currentCountry) || targetPeers.isEmpty()) {
                                // go to idle and try another country
                                moveToIdle(state);
                                controller.stateHasChanged();
                                return false;
                            } else {
                                // try a new batch of connections to the target peers and wait some time
                                attemptMoreConnections();
                                return true;
                            }
                        default:
                            return true;
                    }
                } else {
                    // everything ok, will retry soon
                    if (state.stateCase != StateCase.IDLE || state.currentCountry != null) {
                        // reset state to idle and main language, ready for next connection wish
                        state.stateCase = StateCase.IDLE;
                        state.currentCountry = null;
                        controller.stateHasChanged();
                    }
                    return true;
                }
            }

            @Override
            public boolean hasReachedGoal(State state, Boolean goal) {
                return true;
            }
        }, "RegularsConnectionManager");
        dynamicState.setEvolveStateTimer(state -> state.stateCase == StateCase.ATTEMPTING_CONNECTIONS, CONNECTIONS_DELAY);
        dynamicState.setEvolveStateTimer(state -> true, GENERAL_DELAY);
        dynamicState.evolve();
    }

    private TargetPeerList getTargetPeers(CountryCode currentCountry, PeerKnowledgeBase peerKnowledgeBase) {
        return new TargetPeerList(peerKnowledgeBase.getRegularPeers(PeerKnowledgeBase.ConnectedQuery.DISCONNECTED, currentCountry));
    }

    private boolean findCountryNeedingMoreConnections(State state, PeerConnectionConfig peerConnectionConfig) {
        // start checking the main country, then the additional countries
        if (!haveEnoughConnections(peerConnectionConfig.getMainCountry())) {
            state.currentCountry = peerConnectionConfig.getMainCountry();
            return true;
        } else {
            for (CountryCode country : peerConnectionConfig.getAdditionalCountries()) {
                if (!haveEnoughConnections(country)) {
                    state.currentCountry = country;
                    return true;
                }
            }
            // all additional countries have enough connections
            return false;
        }
    }

    private void moveToIdle(State state) {
        state.stateCase = StateCase.IDLE;
        state.currentCountry = null;
    }

    private boolean haveEnoughConnections(CountryCode country) {
        return remainingConnections(country) <= 0;
    }

    private int remainingConnections(CountryCode country) {
        int connectionsGoal;
        if (country.equals(peerConnectionConfig.getMainCountry())) {
            // main country
            connectionsGoal = peerConnectionConfig.getMaxRegularConnections();
        } else {
            connectionsGoal = peerConnectionConfig.getMaxRegularConnectionsForAdditionalCountries();
        }
        return connectionsGoal - connectedPeers.getConnectedPeersCountryCount(country);
    }

    private void attemptMoreConnections() {
        for (PeerEntryFacade target : targetPeers.retrieveTargetBatch(remainingConnections(dynamicState.state().currentCountry))) {
            peerConnectionManager.attemptConnection(target);
        }
    }

    public void setConnectionGoal(boolean connect) {
        dynamicState.setGoal(connect);
    }

    public void connectionConfigHasChanged() {
        dynamicState.evolve();
    }

    public void stop() {
        setConnectionGoal(false);
        dynamicState.blockUntilGoalReached(500);
        dynamicState.stop();
    }
}
