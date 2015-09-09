package jacz.peerengine.client;

import jacz.peerengine.util.ConnectionStatus;

/**
 * This interface allows peer engine clients to indicate which custom Peer FSMs must be built upon requests from
 * other peers. One class implementing this interface builds one custom FSM (either generic or timed)
 */
public interface PeerFSMFactory {

    /**
     * This method retrieves the PeerFSMAction implementation for the required custom FSM
     *
     * @param connectionStatus the connection status of the peer requesting this CustomFSM
     * @return the PeerFSMAction implementation for the required custom FSM, or null if the request is denied
     */
    PeerFSMAction<?> buildPeerFSMAction(ConnectionStatus connectionStatus);

    // if this is null, then a non-timed FSM must be returned
    Long getTimeoutMillis();
}
