package jacz.peerengineservice;

import jacz.util.io.object_serialization.*;

import java.io.IOException;
import java.io.Serializable;
import java.security.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class acts as an in-memory key-store, storing a set of asymmetric RSA keys of variable size.
 * <p/>
 * The sizes generated are 1024, 2048, ... 12288
 */
public final class PeerEncryption implements VersionedObject {

    public static final class SizedKeyPair implements Serializable {

        public final int size;

        public final KeyPair keyPair;

        public SizedKeyPair(int size, KeyPair keyPair) {
            this.size = size;
            this.keyPair = keyPair;
        }
    }

    private static final String VERSION = "0.1";

    private static final String CURRENT_VERSION = VERSION;

    private static final String ALGORITHM = "RSA";

    private static final String RAND_ALGORITHM = "SHA1PRNG";

    private static final String RAND_PROVIDER = "SUN";

    private static final int KEY_SIZE_COUNT = 4;

    private static final int INITIAL_KEY_SIZE = 1024;

    private static final List<Integer> KEY_SIZES;

    static {
        KEY_SIZES = new ArrayList<>();
        for (int i = 1; i <= KEY_SIZE_COUNT; i++) {
            KEY_SIZES.add(i * INITIAL_KEY_SIZE);
        }
    }

    private byte[] originalSeed;

    private ArrayList<SizedKeyPair> sizedKeyPairs;

    public PeerEncryption(byte[] originalSeed, ArrayList<SizedKeyPair> sizedKeyPairs) {
        this.originalSeed = originalSeed;
        this.sizedKeyPairs = sizedKeyPairs;
    }

    public PeerEncryption(String path) throws VersionedSerializationException, IOException {
        VersionedObjectSerializer.deserialize(this, path);
    }

    public static PeerEncryption generatePeerEncryption(byte[] originalSeed) throws NoSuchProviderException, NoSuchAlgorithmException {
        byte[] seed = originalSeed;
        ArrayList<SizedKeyPair> sizedKeyPairs = new ArrayList<>();
        for (Integer size : KEY_SIZES) {
            System.out.println("Generating key of size " + size);
            SizedKeyPair pair = generateKeyPair(size, seed);
            sizedKeyPairs.add(pair);
            // add generated private key bytes to randomBytes, for further shuffling
            seed = Serializer.addArrays(seed, pair.keyPair.getPrivate().getEncoded());
        }
        return new PeerEncryption(originalSeed, sizedKeyPairs);
    }

    private static SizedKeyPair generateKeyPair(int size, byte[] randomBytes) throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyGen;
        SecureRandom random;
        keyGen = KeyPairGenerator.getInstance(ALGORITHM);
        random = SecureRandom.getInstance(RAND_ALGORITHM, RAND_PROVIDER);
        random.setSeed(randomBytes);
        keyGen.initialize(size, random);
        KeyPair pair = keyGen.generateKeyPair();
        return new SizedKeyPair(size, pair);
    }

    public byte[] getOriginalSeed() {
        return originalSeed;
    }

    public List<SizedKeyPair> getSizedKeyPairs() {
        return sizedKeyPairs;
    }

    public byte[] getPublicDigest() {
        FragmentedByteArray publicDigest = new FragmentedByteArray();
        for (SizedKeyPair sizedKeyPair : getSizedKeyPairs()) {
            publicDigest.addArrays(sizedKeyPair.keyPair.getPublic().getEncoded());
        }
        return publicDigest.generateArray();
    }

    @Override
    public String getCurrentVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public Map<String, Serializable> serialize() {
        Map<String, Serializable> attributes = new HashMap<>();
        attributes.put("originalSeed", originalSeed);
        attributes.put("sizedKeyPairs", sizedKeyPairs);
        return attributes;
    }

    @Override
    public void deserialize(Map<String, Object> attributes) {
        originalSeed = (byte[]) attributes.get("originalSeed");
        sizedKeyPairs = (ArrayList<SizedKeyPair>) attributes.get("sizedKeyPairs");
    }

    @Override
    public void deserializeOldVersion(String version, Map<String, Object> attributes) throws UnrecognizedVersionException {
        throw new UnrecognizedVersionException();
    }
}
