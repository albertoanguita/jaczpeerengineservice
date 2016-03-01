package jacz.peerengineservice.client;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.ConnectionStatus;

/**
 * This interface allows peer engine clients to indicate which custom Peer FSMs must be built upon requests from
 * other peers. One class implementing this interface builds one custom FSM (either generic or timed)
 */
public interface PeerFSMFactory {

    /**
     * This method retrieves the PeerFSMAction implementation for the required custom FSM
     *
     * @param requestingPeerStatus the connection status of the peer requesting this CustomFSM
     * @return the PeerFSMAction implementation for the required custom FSM, or null if the request is denied
     */
    PeerFSMAction<?> buildPeerFSMAction(PeerId clientPeer, ConnectionStatus requestingPeerStatus);

    // if this is null, then a non-timed FSM must be returned
    Long getTimeoutMillis();
}
