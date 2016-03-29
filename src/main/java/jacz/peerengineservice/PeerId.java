package jacz.peerengineservice;

import jacz.util.hash.SHA_256;
import jacz.util.io.SixBitSerializer;
import jacz.util.lists.tuple.Duple;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Arrays;

/**
 * This class represents an identifier for a peer in our network. The id consist on a 32-byte array (256 bits) obtained randomly through SHA-256
 */
public final class PeerId implements Comparable<PeerId>, Serializable {

    private static final int KEY_LENGTH = 32;

    private static final int SIX_BIT_LENGTH = 43;

    // 32-byte array (43 characters in six-bit serialization format)
    private final byte[] id;

    public PeerId(byte[] id) {
        if (id.length >= KEY_LENGTH) {
            // truncate the byte array (keep the first KEY_LENGTH bytes)
            this.id = Arrays.copyOf(id, KEY_LENGTH);
        } else {
            throw new IllegalArgumentException("Incorrect peer id. Received: " + Arrays.toString(id));
        }
    }

    public PeerId(String id) throws IllegalArgumentException {
        try {
            this.id = SixBitSerializer.deserialize(id, KEY_LENGTH);
        } catch (Exception e) {
            throw new IllegalArgumentException("Incorrect peer id: " + id);
        }
    }

    public static PeerId generateRandomPeerId(byte[] randomBytes) {
        return new PeerId(new SHA_256().digest(randomBytes));
    }

    public static PeerId buildTestPeerId(String postID) {
        while (postID.length() < SIX_BIT_LENGTH) {
            postID = "0" + postID;
        }
        return new PeerId(postID);
    }

    public static Duple<PeerId, PeerEncryption> generateIdAndEncryptionKeys(byte[] randomBytes) {
        PeerEncryption peerEncryption = new PeerEncryption(randomBytes);
        return new Duple<>(new PeerId(peerEncryption.getPublicDigest()), peerEncryption);
    }

    public static boolean isPeerId(String id) {
        try {
            new PeerId(id);
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
            if (!(obj instanceof PeerId)) {
                return false;
            } else {
                return Arrays.equals(id, ((PeerId) obj).id);
            }
    }

    public int compareTo(@NotNull PeerId o) {
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

    public byte[] toByteArray() {
        return id;
    }

    /**
     * This method evaluates if this ID has higher priority compared to a given ID. This is used to determine who has
     * priority in case of duplicate connections (two peers try to connect to each other at the same time), but has
     * no other practical uses and it does not transcend to the peer engine client
     *
     * @param anotherPeerId ID to which we must compare our own ID
     * @return true if our ID has higher priority that anotherPeerId, false otherwise
     */
    public boolean hasHigherPriorityThan(PeerId anotherPeerId) {
        return compareTo(anotherPeerId) < 0;
    }
}
