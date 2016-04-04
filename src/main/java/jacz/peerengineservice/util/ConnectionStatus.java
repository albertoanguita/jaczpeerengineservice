package jacz.peerengineservice.util;

/**
 * These are connection states used by the peer engine, they don't need to be the states that the client see's.
 * <p/>
 * The client is not able to modify these states, as they are for internal use of the Peer engine (although they are
 * readable by the client)
 * TODO REMOVE
 */
public enum ConnectionStatus {

//    DISCONNECTED_REGULAR,
//    DISCONNECTED_FAVORITE,
//    CONNECTED_REGULARS,
//    CONNECTED_FAVORITE_TO_REGULAR,
//    CONNECTED_REGULAR_TO_FAVORITE,
//    CONNECTED_FAVORITES

//    /**
//     * We are not connected to this peer
//     */
//    DISCONNECTED,
//
//    /**
//     * The respective peer has connected to us but he is not marked either as friend nor blocked peer. Connection is allowed, but it is up to
//     * the client to decide which services he is allowed to access
//     */
//    UNVALIDATED,
//
//    /**
//     * We connected to the respective peer as he is marked as friend. He accepted the connection, but we are not marked as friend to him. We might
//     * not have access to all services
//     */
//    WAITING_FOR_REMOTE_VALIDATION,
//
//    /**
//     * Correct connection. The respective peer can be client or server, and we are both friends
//     */
//    CORRECT;

//    public boolean isFriend() {
//        return this == WAITING_FOR_REMOTE_VALIDATION || this == CORRECT;
//    }
}
