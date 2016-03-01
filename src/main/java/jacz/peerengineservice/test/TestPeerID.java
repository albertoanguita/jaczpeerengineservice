package jacz.peerengineservice.test;

import jacz.peerengineservice.PeerId;
import jacz.util.stochastic.MouseToRandom;

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
            PeerId peerId = PeerId.generateIdAndEncryptionKeys(randomBytes).element1;
            String s = peerId.toString();
            PeerId peerId2 = new PeerId(s);
            System.out.print("-" + i + ": ");
            System.out.println(s);
            if (!peerId.toString().equals(peerId2.toString())) {
                System.out.println("ERROR!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
        }
        System.out.println("FIN");
    }
}
