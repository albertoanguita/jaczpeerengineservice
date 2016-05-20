package jacz.peerengineservice;

import org.junit.Test;

import java.util.Arrays;

/**
 * Created by Alberto on 12/05/2016.
 */
public class PeerEncryptionTest {

    @Test
    public void test() {

        byte[] seed = new byte[1];

        seed[0] = 5;
        PeerEncryption peerEncryption = new PeerEncryption(seed);
        System.out.println(peerEncryption.getPublicKey().toString());
        System.out.println(Arrays.toString(peerEncryption.getPublicDigest()));

        seed[0] = 7;
        peerEncryption = new PeerEncryption(seed);
        System.out.println(peerEncryption.getPublicKey().toString());
        System.out.println(Arrays.toString(peerEncryption.getPublicDigest()));
    }

}