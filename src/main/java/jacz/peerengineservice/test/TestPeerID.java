package jacz.peerengineservice.test;

import jacz.peerengineservice.PeerID;
import jacz.util.stochastic.MouseToRandom;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 */
public class TestPeerID {

    public static void main(String args[]) {
        Random random = new Random(1);

        byte[] randomBytes;

        MouseToRandom mouseToRandom = new MouseToRandom(10);
        mouseToRandom.mouseCoords(7, 1);
        mouseToRandom.mouseCoords(2, 1);
        mouseToRandom.mouseCoords(3, 1);
        mouseToRandom.mouseCoords(4, 1);
        mouseToRandom.mouseCoords(5, 1);
        mouseToRandom.mouseCoords(500, 1);
        randomBytes = mouseToRandom.getRandomBytes();

        List<Integer> sizes = new ArrayList<>();
        sizes.add(1024);
        sizes.add(2048);
//        sizes.add(3072);
//        sizes.add(4096);
//        sizes.add(5120);
//        sizes.add(6144);

        for (int i = 0; i < 5; i++) {
//            mouseToRandom.mouseCoords(0, i);
//            randomBytes = mouseToRandom.getRandomBytes();
            random.nextBytes(randomBytes);
            PeerID peerID = PeerID.generateIdAndEncryptionKeys(sizes, 1, randomBytes).element1;
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
