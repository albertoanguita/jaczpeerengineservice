package jacz.peerengineservice.util.data_synchronization.old;

import java.io.Serializable;

/**
 * A duple formed by a String index and a String hash
 */
public class IndexAndHash implements Serializable, Comparable<IndexAndHash> {

    private static final char SEPARATOR = '@';

    final String index;

    final String hash;

    public IndexAndHash(String index, String hash) {
        this.index = index;
        this.hash = hash;
    }

    @Override
    public int compareTo(IndexAndHash o) {
        if (index != null && o.index != null) {
            return index.compareTo(o.index);
        } else if (hash != null && o.hash != null) {
            return hash.compareTo(o.hash);
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        return index + SEPARATOR + hash;
    }

    public static IndexAndHash deserializeString(String str) throws IllegalArgumentException {
        int separatorIndex = str.indexOf(SEPARATOR);
        if (separatorIndex >= 0) {
            return new IndexAndHash(str.substring(0, separatorIndex), str.substring(separatorIndex + 1));
        } else {
            throw new IllegalArgumentException("Malformed string: " + str);
        }
    }
}
