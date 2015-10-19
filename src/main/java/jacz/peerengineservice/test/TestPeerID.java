package jacz.peerengineservice.test;

import jacz.peerengineservice.PeerID;
import jacz.util.stochastic.MouseToRandom;

import java.util.Random;

/**
 *
 */
public class TestPeerID {

    public static void main(String args[]) throws Exception {
//        Random random = new Random(1);

        byte[] randomBytes;



        for (int i = 0; i < 2; i++) {
            MouseToRandom mouseToRandom = new MouseToRandom(10, false);
            mouseToRandom.mouseCoords(8, 1);
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
            PeerID peerID = PeerID.generateIdAndEncryptionKeys(randomBytes).element1;
            String s = peerID.toString();
            PeerID peerID2 = new PeerID(s);
            System.out.print("-" + i + ": ");
            System.out.println(s);
            if (!peerID.toString().equals(peerID2.toString())) {
                System.out.println("ERROR!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
        }
        System.out.println("FIN");
    }
}
