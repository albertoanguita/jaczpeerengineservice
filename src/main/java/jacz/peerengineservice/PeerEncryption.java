package jacz.peerengineservice;

import jacz.util.io.serialization.*;

import java.io.IOException;
import java.io.Serializable;
import java.security.*;
import java.util.HashMap;
import java.util.Map;

/**
 * This class acts as an in-memory key-store, storing a set of asymmetric RSA keys of variable size.
 * <p/>
 * The sizes generated are 1024, 2048, 4096, 8192
 */
public final class PeerEncryption implements VersionedObject {

//    public static final class SizedKeyPair implements Serializable {
//
//        public final int size;
//
//        public final KeyPair keyPair;
//
//        public SizedKeyPair(int size, KeyPair keyPair) {
//            this.size = size;
//            this.keyPair = keyPair;
//        }
//    }

    private static final String VERSION_0_1 = "0.1";

    private static final String CURRENT_VERSION = VERSION_0_1;

    private static final String ALGORITHM = "RSA";

    private static final String RAND_ALGORITHM = "SHA1PRNG";

    private static final String RAND_PROVIDER = "SUN";

//    private static final int KEY_SIZE_COUNT = 4;

    private static final int KEY_SIZE = 2048;

//    private static final List<Integer> KEY_SIZES;

//    static {
//        KEY_SIZES = new ArrayList<>();
//        for (int i = 1; i <= KEY_SIZE_COUNT; i++) {
//            KEY_SIZES.add(i * INITIAL_KEY_SIZE);
//        }
//    }

//    private byte[] originalSeed;

//    private ArrayList<SizedKeyPair> sizedKeyPairs;

    private KeyPair keyPair;

    public PeerEncryption(byte[] seed) {
        this.keyPair = generateKeyPair(KEY_SIZE, seed);
    }

    public PeerEncryption(String path, String... backupPaths) throws VersionedSerializationException, IOException {
        VersionedObjectSerializer.deserialize(this, path, backupPaths);
    }

//    public static PeerEncryption generatePeerEncryption(byte[] originalSeed) {
//        KeyPair pair = generateKeyPair(KEY_SIZE, originalSeed);
//        byte[] seed = originalSeed;
//        ArrayList<SizedKeyPair> sizedKeyPairs = new ArrayList<>();
//        for (Integer size : KEY_SIZES) {
//            System.out.println("Generating key of size " + size);
//            SizedKeyPair pair = generateKeyPair(size, seed);
//            sizedKeyPairs.add(pair);
//            // add generated private key bytes to randomBytes, for further shuffling
//            seed = Serializer.addArrays(seed, pair.keyPair.getPrivate().getEncoded());
//        }
//        return new PeerEncryption(originalSeed, sizedKeyPairs);
//    }

    private static KeyPair generateKeyPair(int size, byte[] randomBytes) {
        try {
            KeyPairGenerator keyGen;
            SecureRandom random;
            keyGen = KeyPairGenerator.getInstance(ALGORITHM);
            random = SecureRandom.getInstance(RAND_ALGORITHM, RAND_PROVIDER);
            random.setSeed(randomBytes);
            keyGen.initialize(size, random);
            return keyGen.generateKeyPair();
//            return new SizedKeyPair(size, pair);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            // todo fatal error
            return null;
        }
    }

//    public byte[] getOriginalSeed() {
//        return originalSeed;
//    }

//    public List<SizedKeyPair> getSizedKeyPairs() {
//        return sizedKeyPairs;
//    }

    public byte[] getPublicDigest() {
        return keyPair.getPublic().getEncoded();
//        FragmentedByteArray publicDigest = new FragmentedByteArray();
//        for (SizedKeyPair sizedKeyPair : getSizedKeyPairs()) {
//            publicDigest.add(sizedKeyPair.keyPair.getPublic().getEncoded());
//        }
//        return publicDigest.generateArray();
    }

    @Override
    public VersionStack getCurrentVersion() {
        return new VersionStack(CURRENT_VERSION);
    }

    @Override
    public Map<String, Serializable> serialize() {
        Map<String, Serializable> attributes = new HashMap<>();
        attributes.put("keyPair", keyPair);
//        attributes.put("sizedKeyPairs", sizedKeyPairs);
        return attributes;
    }

    @Override
    public void deserialize(String version, Map<String, Object> attributes, VersionStack parentVersions) throws UnrecognizedVersionException {
        if (version.equals(CURRENT_VERSION)) {
//            originalSeed = (byte[]) attributes.get("originalSeed");
            keyPair = (KeyPair) attributes.get("keyPair");
        } else {
            throw new UnrecognizedVersionException();
        }
    }
}
