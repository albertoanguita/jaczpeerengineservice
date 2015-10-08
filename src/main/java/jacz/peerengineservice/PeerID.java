package jacz.peerengineservice;

import jacz.util.hash.SHA_256;
import jacz.util.io.SixBitSerializer;
import jacz.util.lists.Duple;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * This class represents an identifier for a peer in our network. The id consist on a 32-byte array (256 bits) obtained randomly through SHA-256
 */
public final class PeerID implements Comparable<PeerID>, Serializable {

    private static final int KEY_LENGTH = 32;

    // 32-byte array (43 characters in six-bit serialization format)
    private final byte[] id;

    public PeerID(byte[] id) {
        if (id.length == KEY_LENGTH) {
            this.id = id;
        } else {
            throw new IllegalArgumentException("Incorrect peer id. Received: " + Arrays.toString(id));
        }
    }

    public PeerID(String id) throws IllegalArgumentException {
        try {
            this.id = SixBitSerializer.deserialize(id, KEY_LENGTH);
        } catch (Exception e) {
            throw new IllegalArgumentException("Incorrect peer id: " + id);
        }
    }

    public static PeerID generateRandomPeerId(byte[] randomBytes) {
        return new PeerID(new SHA_256().digest(randomBytes));
    }

    public static Duple<PeerID, PeerEncryption> generateIdAndEncryptionKeys(List<Integer> sizes, int posForPeerIDGeneration, byte[] randomBytes) {
        PeerEncryption peerEncryption = PeerEncryption.generatePeerEncryption(sizes, randomBytes);
        return new Duple<>(generateRandomPeerId(peerEncryption.getSizedKeyPairs().get(posForPeerIDGeneration).keyPair.getPublic().getEncoded()), peerEncryption);
    }

    public static boolean isPeerID(String id) {
        try {
            new PeerID(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else //noinspection SimplifiableIfStatement
            if (!(obj instanceof PeerID)) {
                return false;
            } else {
                return Arrays.equals(id, ((PeerID) obj).id);
            }
    }

    public int compareTo(@NotNull PeerID o) {
        return SixBitSerializer.serialize(id).compareTo(SixBitSerializer.serialize(o.id));
    }

    @Override
    public int hashCode() {
        return SixBitSerializer.serialize(id).hashCode();
    }

    @Override
    public String toString() {
        return SixBitSerializer.serialize(id);
    }

    /**
     * This method evaluates if this ID has higher priority compared to a given ID. This is used to determine who has
     * priority in case of duplicate connections (two peers try to connect to each other at the same time), but has
     * no other practical uses and it does not transcend to the peer engine client
     *
     * @param anotherPeerID ID to which we must compare our own ID
     * @return true if our ID has higher priority that anotherPeerID, false otherwise
     */
    public boolean hasHigherPriorityThan(PeerID anotherPeerID) {
        return compareTo(anotherPeerID) < 0;
    }
}
