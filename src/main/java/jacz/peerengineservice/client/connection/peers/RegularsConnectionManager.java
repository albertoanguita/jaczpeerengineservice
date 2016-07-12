package jacz.peerengineservice.client.connection.peers;

import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.connection.ConnectedPeers;
import jacz.peerengineservice.client.connection.peers.kb.PeerEntryFacade;
import jacz.peerengineservice.client.connection.peers.kb.PeerKnowledgeBase;
import org.aanguita.jacuzzi.AI.evolve.EvolvingState;
import org.aanguita.jacuzzi.AI.evolve.EvolvingStateController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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

        public int size() {
            return targetPeers.size();
        }

        public List<PeerEntryFacade> retrieveTargetBatch(int maxSize) {
            List<PeerEntryFacade> targets = new ArrayList<>();
            while (!targetPeers.isEmpty() && targets.size() < TARGET_BATCH_SIZE && targets.size() < maxSize) {
                targets.add(targetPeers.remove(0));
            }
            return targets;
        }
    }

    private class PeersRequiringMoreInfoBag {

        private final int maxCapacity;

        private final List<PeerId> peers;

        public PeersRequiringMoreInfoBag(int maxCapacity) {
            this.maxCapacity = maxCapacity;
            peers = new ArrayList<>();
        }

        public void clear() {
            peers.clear();
        }

        public void addPeer(PeerEntryFacade targetPeer) {
            if (peers.size() < maxCapacity) {
                peers.add(targetPeer.getPeerId());
            }
        }

        public List<PeerId> getPeers() {
            return peers;
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

        boolean mainCountryTurn;

        CountryCode lastCheckedAdditionalCountry;

        public State() {
            stateCase = StateCase.IDLE;
            currentCountry = null;
            mainCountryTurn = true;
            lastCheckedAdditionalCountry = null;
        }

        private void reset() {
            // reset state to idle and main language, ready for next connection wish
            stateCase = StateCase.IDLE;
            currentCountry = null;
        }

        private void checkMainCountry(CountryCode mainCountry) {
            currentCountry = mainCountry;
            mainCountryTurn = false;
            lastCheckedAdditionalCountry = null;
        }

        private void checkAdditionalCountry(CountryCode additionalCountry) {
            currentCountry = additionalCountry;
            mainCountryTurn = true;
            lastCheckedAdditionalCountry = additionalCountry;
        }

        public void noAdditionalCountryChecked() {
            lastCheckedAdditionalCountry = null;
        }

        @Override
        public String toString() {
            return "State{" +
                    "stateCase=" + stateCase +
                    ", currentCountry=" + currentCountry +
                    ", mainCountryTurn=" + mainCountryTurn +
                    ", lastCheckedAdditionalCountry=" + lastCheckedAdditionalCountry +
                    '}';
        }
    }

    private final static Logger logger = LoggerFactory.getLogger(RegularsConnectionManager.class);

    private static final long CONNECTIONS_DELAY = 5000L;

    private static final long GENERAL_DELAY = 3 * CONNECTIONS_DELAY;

    private static final int SMALL_TARGET_LIST = 50;

    private static final int TARGET_BATCH_SIZE = 5;

    private static final int PEERS_REQUIRING_MORE_INFO_BAG_SIZE = 15;





    private final PeerConnectionManager peerConnectionManager;

    private final ConnectedPeers connectedPeers;

    private final PeerConnectionConfig peerConnectionConfig;

    private TargetPeerList targetPeers;

    private final PeersRequiringMoreInfoBag peersRequiringMoreInfoBag;

    private final EvolvingState<State, Boolean> dynamicState;

    public RegularsConnectionManager(PeerConnectionManager peerConnectionManager, PeerKnowledgeBase peerKnowledgeBase, ConnectedPeers connectedPeers, PeerConnectionConfig peerConnectionConfig) {
        this.peerConnectionManager = peerConnectionManager;
        this.connectedPeers = connectedPeers;
        this.peerConnectionConfig = peerConnectionConfig;
        peersRequiringMoreInfoBag = new PeersRequiringMoreInfoBag(PEERS_REQUIRING_MORE_INFO_BAG_SIZE);
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
                            state.stateCase = StateCase.IDLE;
                            // look for a language that needs more connections (start by currentLanguage)
                            if (findCountryNeedingMoreConnections(state)) {
                                // country found, attempt connections with it
                                targetPeers = getTargetPeers(state.currentCountry, peerKnowledgeBase);
                                if (targetPeers.size() < SMALL_TARGET_LIST) {
                                    // we have ver few valid regular peers for this country -> ask for more
                                    // and go back to idle (and wait some time)
                                    peerConnectionManager.askForMoreRegularPeers(state.currentCountry);
                                    state.stateCase = StateCase.IDLE;
                                    controller.stateHasChanged();
                                    return true;
                                }
                                if (!targetPeers.isEmpty()) {
                                    // there are target peers -> try to connect with them
                                    peersRequiringMoreInfoBag.clear();
                                    state.stateCase = StateCase.ATTEMPTING_CONNECTIONS;
                                    controller.stateHasChanged();
                                    return false;
                                } else {
                                    // there are no target peers as of now -> go back to idle
                                    state.stateCase = StateCase.IDLE;
                                    controller.stateHasChanged();
                                    return true;
                                }
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
                                logger.info(peersRequiringMoreInfoBag.getPeers().size() + " peers have invalid address");
                                if (!peersRequiringMoreInfoBag.getPeers().isEmpty()) {
                                    logger.info("Requesting info to the server for those peers");
                                    peerConnectionManager.askForSpecificPeersInfo(peersRequiringMoreInfoBag.getPeers());
                                }
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
                        state.reset();
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
    }

    private TargetPeerList getTargetPeers(CountryCode currentCountry, PeerKnowledgeBase peerKnowledgeBase) {
        logger.info("Building target peer list for " + currentCountry.name());
        List<PeerEntryFacade> targetPeers = peerKnowledgeBase.getRegularPeers(PeerKnowledgeBase.ConnectedQuery.DISCONNECTED, currentCountry);

        return new TargetPeerList(peerKnowledgeBase.getRegularPeers(PeerKnowledgeBase.ConnectedQuery.DISCONNECTED, currentCountry));
    }

    private boolean findCountryNeedingMoreConnections(State state) {
        // start checking the main country, then the additional countries
        // once the main country is fulfilled, we cycle through additional countries until we find one lacking
        // connections (this way we avoid that one with no connections blocks the others)
        if (state.mainCountryTurn) {
            return checkMainCountry(state) || checkAdditionalCountries(state);
        } else {
            return checkAdditionalCountries(state) || checkMainCountry(state);
        }
    }

    private boolean checkMainCountry(State state) {
        if (!haveEnoughConnections(peerConnectionConfig.getMainCountry())) {
            state.checkMainCountry(peerConnectionConfig.getMainCountry());
            return true;
        } else {
            return false;
        }
    }

    private boolean checkAdditionalCountries(State state) {
        List<CountryCode> additionalCountries = peerConnectionConfig.getAdditionalCountries();
        // index of the last checked additional country, or -1 if no last checked additional country
        int indexOfLastCheckedAdditionalCountry = state.lastCheckedAdditionalCountry != null ? additionalCountries.indexOf(state.lastCheckedAdditionalCountry) : -1;
        for (int i = indexOfLastCheckedAdditionalCountry + 1; i < additionalCountries.size(); i++) {
            if (checkAdditionalCountry(state, additionalCountries, i)) {
                return true;
            }
        }
        for (int i = 0; i < indexOfLastCheckedAdditionalCountry; i++) {
            if (checkAdditionalCountry(state, additionalCountries, i)) {
                return true;
            }
        }
        // all additional countries have enough connections
        state.noAdditionalCountryChecked();
        return false;
    }

    private boolean checkAdditionalCountry(State state, List<CountryCode> additionalCountries, int index) {
        CountryCode country = additionalCountries.get(index);
        if (!haveEnoughConnections(country)) {
            state.checkAdditionalCountry(country);
            return true;
        } else {
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
            if (target.getPeerAddress().isNull()) {
                // the target peer has now an invalid peer address (maybe it was already invalid). Add it to the bag of
                // target peers for which ask for more info to the server once we finish attempting connections
                peersRequiringMoreInfoBag.addPeer(target);
            }
        }
    }

    public void setConnectionGoal(boolean connect) {
        dynamicState.setGoal(connect);
        dynamicState.evolve();
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
