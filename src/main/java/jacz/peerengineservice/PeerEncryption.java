package jacz.peerengineservice;

import jacz.peerengineservice.client.PeerClient;
import jacz.util.io.serialization.*;

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
 * The sizes generated are 1024, 2048, 4096, 8192
 */
public final class PeerEncryption implements VersionedObject {

    private static final String VERSION_0_1 = "0.1";

    private static final String CURRENT_VERSION = VERSION_0_1;

    private static final String ALGORITHM = "RSA";

    private static final String RAND_ALGORITHM = "SHA1PRNG";

    private static final String RAND_PROVIDER = "SUN";

    private static final int KEY_SIZE = 2048;

    private KeyPair keyPair;

    private final List<String> repairedFiles;

    public PeerEncryption(byte[] seed) {
        this.keyPair = generateKeyPair(KEY_SIZE, seed);
        repairedFiles = new ArrayList<>();
    }

    public PeerEncryption(String path, String... backupPaths) throws VersionedSerializationException, IOException {
        this(path, false, backupPaths);
    }

    public PeerEncryption(String path, boolean repairIfBroken, String... backupPaths) throws VersionedSerializationException, IOException {
        repairedFiles = VersionedObjectSerializer.deserialize(this, path, repairIfBroken, backupPaths);
    }

    public List<String> getRepairedFiles() {
        return repairedFiles;
    }

    private static KeyPair generateKeyPair(int size, byte[] randomBytes) {
        try {
            KeyPairGenerator keyGen;
            SecureRandom random;
            keyGen = KeyPairGenerator.getInstance(ALGORITHM);
            random = SecureRandom.getInstance(RAND_ALGORITHM, RAND_PROVIDER);
            random.setSeed(randomBytes);
            keyGen.initialize(size, random);
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            PeerClient.reportFatalError(e.toString(), ALGORITHM, RAND_ALGORITHM, RAND_PROVIDER);
            return null;
        }
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    public byte[] getPublicDigest() {
        return keyPair.getPublic().getEncoded();
    }

    @Override
    public VersionStack getCurrentVersion() {
        return new VersionStack(CURRENT_VERSION);
    }

    @Override
    public Map<String, Serializable> serialize() {
        Map<String, Serializable> attributes = new HashMap<>();
        attributes.put("keyPair", keyPair);
        return attributes;
    }

    @Override
    public void deserialize(String version, Map<String, Object> attributes, VersionStack parentVersions) throws UnrecognizedVersionException {
        if (version.equals(CURRENT_VERSION)) {
            keyPair = (KeyPair) attributes.get("keyPair");
        } else {
            throw new UnrecognizedVersionException();
        }
    }
}
