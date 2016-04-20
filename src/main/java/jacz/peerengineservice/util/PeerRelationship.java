package jacz.peerengineservice.util;

/**
 * Different relationships that we can have with other peers
 */
public enum PeerRelationship {

    BLOCKED,
    REGULARS,
    FAVORITE_TO_REGULAR,
    REGULAR_TO_FAVORITE,
    FAVORITES;

    public boolean isFavorite() {
        return this == FAVORITE_TO_REGULAR || this == FAVORITES;
    }

    public boolean isRegular() {
        return this == REGULAR_TO_FAVORITE || this == REGULARS;
    }

    public boolean isBlocked() {
        return this == BLOCKED;
    }
}
