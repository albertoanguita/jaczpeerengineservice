package jacz.peerengineservice.client.connection.peers;

import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.connection.ConnectedPeers;
import jacz.peerengineservice.client.connection.peers.kb.Management;
import jacz.peerengineservice.client.connection.peers.kb.PeerEntryFacade;
import jacz.peerengineservice.client.connection.peers.kb.PeerKnowledgeBase;
import org.aanguita.jacuzzi.AI.evolve.EvolvingState;
import org.aanguita.jacuzzi.AI.evolve.EvolvingStateController;
import org.aanguita.jacuzzi.AI.evolve.StateCondition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

    /**
     * We use this variable so subsequent set timers replace previous ones
     */
    private static final StateCondition<State> trueStateCondition = state -> true;

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
        // todo try to disconnect peers with no active transfers (@FUTURE@)
        Set<PeerId> peersToDisconnect = new HashSet<>();
        int maxRegularConnectionsForMainCountry = peerConnectionManager.getMaxRegularConnections();
        Map<CountryCode, Integer> maxRegularConnectionsForAdditionalCountry = initMaxRegularConnectionsForAdditionalCountries();
        int maxRegularConnectionsForOtherCountries = peerConnectionManager.getMaxRegularConnectionsForOtherCountries();
        for (PeerId peerId : connectedPeers.getConnectedPeers()) {
            PeerEntryFacade peerEntryFacade = peerKnowledgeBase.getPeerEntryFacade(peerId);

            // BLOCKED peers
            if (peerEntryFacade.getRelationship() == Management.Relationship.BLOCKED) {
                peersToDisconnect.add(peerId);
                continue;
            }

            // REGULAR peers, and we do not wish to connect to regulars
            if (peerEntryFacade.getRelationship() == Management.Relationship.REGULAR
                    && !peerConnectionManager.isOwnWishForRegularConnections()) {
                peersToDisconnect.add(peerId);
                continue;
            }

            // REGULAR peers, we do wish regular connections, and he offers our main country
            if (peerEntryFacade.getRelationship() == Management.Relationship.REGULAR
                    && peerConnectionManager.isOwnWishForRegularConnections()
                    && peerEntryFacade.getMainCountry() == peerConnectionManager.getMainCountry()) {
                if (maxRegularConnectionsForMainCountry > 0) {
                    maxRegularConnectionsForMainCountry--;
                } else {
                    peersToDisconnect.add(peerId);
                    continue;
                }
            }

            // REGULAR peers, we do wish regular connections, and he offers one additional country
            if (peerEntryFacade.getRelationship() == Management.Relationship.REGULAR
                    && peerConnectionManager.isOwnWishForRegularConnections()
                    && peerConnectionManager.isAdditionalCountry(peerEntryFacade.getMainCountry())) {
                if (maxRegularConnectionsForAdditionalCountry.get(peerEntryFacade.getMainCountry()) > 0) {
                    maxRegularConnectionsForAdditionalCountry.put(peerEntryFacade.getMainCountry(), maxRegularConnectionsForAdditionalCountry.get(peerEntryFacade.getMainCountry()) -1);
                } else {
                    peersToDisconnect.add(peerId);
                    continue;
                }
            }

            // REGULAR peers, we do wish regular connections, and he offers an unknown country
            if (peerEntryFacade.getRelationship() == Management.Relationship.REGULAR
                    && peerConnectionManager.isOwnWishForRegularConnections()
                    && !peerConnectionManager.getAllCountries().contains(peerEntryFacade.getMainCountry())) {
                if (maxRegularConnectionsForOtherCountries > 0) {
                    maxRegularConnectionsForOtherCountries--;
                } else {
                    peersToDisconnect.add(peerId);
                }
            }
        }
        mustDisconnectFromPeers(peersToDisconnect);
    }

    private Map<CountryCode, Integer> initMaxRegularConnectionsForAdditionalCountries() {
        Map<CountryCode, Integer> maxRegularConnectionsForMainCountry = new HashMap<>();
        for (CountryCode countryCode : peerConnectionManager.getAdditionalCountries()) {
            maxRegularConnectionsForMainCountry.put(countryCode, peerConnectionManager.getMaxRegularConnectionsForAdditionalCountries());
        }
        return maxRegularConnectionsForMainCountry;
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
        // replace previously set time with new one
        dynamicState.setEvolveStateTimer(trueStateCondition, time);
        currentRetry = time;
    }

    public void checkDisconnections() {
        setRetryTime(MIN_RETRY);
    }

    public void stop() {
        dynamicState.stop();
    }
}
