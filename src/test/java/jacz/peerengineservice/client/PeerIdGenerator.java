package jacz.peerengineservice.client;

import jacz.peerengineservice.PeerId;

/**
 * Easy generation of peer ids
 */
public class PeerIdGenerator {

    public static PeerId peerID(int b) {
        String pid = "" + b;
        while (pid.length() < 43) {
            pid = "0" + pid;
        }
        return new PeerId(pid);
    }
}
