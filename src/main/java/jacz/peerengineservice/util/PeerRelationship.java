package jacz.peerengineservice.util;

/**
 * Created by Alberto on 04/04/2016.
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
