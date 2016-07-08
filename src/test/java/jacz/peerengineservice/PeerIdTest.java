package jacz.peerengineservice;


import org.aanguita.jacuzzi.stochastic.MouseToRandom;
import org.junit.Assert;

import java.util.Arrays;

/**
 * Created by Alberto on 11/03/2016.
 */
public class PeerIdTest {

    @org.junit.Test
    public void test() {

        // missing just 1 char
        PeerId peerId = PeerId.buildTestPeerId("000000000000000000000000000000000000000095");
        Assert.assertEquals("0000000000000000000000000000000000000000095", peerId.toString());




        byte[] randomBytes;
        for (int i = 0; i < 2; i++) {
            MouseToRandom mouseToRandom = new MouseToRandom(10, false);
            mouseToRandom.mouseCoords(18, 1);
            mouseToRandom.mouseCoords(2, 1);
            mouseToRandom.mouseCoords(5, 1);
            mouseToRandom.mouseCoords(4, 1);
            mouseToRandom.mouseCoords(5, 1);
            mouseToRandom.mouseCoords(500, 1);
            mouseToRandom.mouseCoords(8, 1);
            mouseToRandom.mouseCoords(2, 1);
            mouseToRandom.mouseCoords(5, 1);
            mouseToRandom.mouseCoords(4, 1);
            mouseToRandom.mouseCoords(5, 1);
            randomBytes = mouseToRandom.getRandomBytes();
//            mouseToRandom.mouseCoords(0, i);
//            randomBytes = mouseToRandom.getRandomBytes();
//            random.nextBytes(randomBytes);
            PeerId peerId1 = PeerId.generateIdAndEncryptionKeys(randomBytes).element1;
            String s = peerId1.toString();
            PeerId peerId2 = new PeerId(s);
            System.out.print("-" + i + ": ");
            System.out.println(s);

            Assert.assertEquals(peerId1, peerId2);
        }

    }

}