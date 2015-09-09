package jacz.peerengine;

import java.io.Serializable;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * This class acts as an in-memory key-store, storing a set of asymmetric RSA keys of variable size. Number of keys and sizes are generic
 */
public final class PeerEncryption implements Serializable {

    public static final class SizedKeyPair implements Serializable {

        public final int size;

        public final KeyPair keyPair;

        public SizedKeyPair(int size, KeyPair keyPair) {
            this.size = size;
            this.keyPair = keyPair;
        }
    }

    private final List<SizedKeyPair> sizedKeyPairs;

    public PeerEncryption(List<SizedKeyPair> sizedKeyPairs) {
        this.sizedKeyPairs = sizedKeyPairs;
    }

    public static PeerEncryption generatePeerEncryption(List<Integer> sizes, byte[] randomBytes) {
        List<SizedKeyPair> sizedKeyPairs = new ArrayList<>();
        for (Integer size : sizes) {
            sizedKeyPairs.add(generateKeyPair(size, randomBytes));
        }
        return new PeerEncryption(sizedKeyPairs);
    }

    private static SizedKeyPair generateKeyPair(int size, byte[] randomBytes) {
        KeyPairGenerator keyGen;
        SecureRandom random;
        try {
            keyGen = KeyPairGenerator.getInstance("RSA");
            random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            random.setSeed(randomBytes);
        } catch (Exception e) {
            // ignore, RSA and SUN are always available out of the box
            return null;
        }
        keyGen.initialize(size, random);
        KeyPair pair = keyGen.generateKeyPair();
        return new SizedKeyPair(size, pair);
    }

    public List<SizedKeyPair> getSizedKeyPairs() {
        return sizedKeyPairs;
    }
}
