package jacz.peerengineservice.test;

import jacz.peerengineservice.PeerID;

/**
 * Class description
 * <p/>
 * User: Alberto<br>
 * Date: 3/05/12<br>
 * Last Modified: 3/05/12
 */
public class PeerIDGenerator {

    public static PeerID peerID(int b) {
        String pid = "" + b;
        while (pid.length() < 43) {
            pid = "0" + pid;
        }
        pid = "pid{" + pid + "}";
        return new PeerID(pid);
    }
}