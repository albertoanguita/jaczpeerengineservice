package jacz.peerengineservice;


import org.junit.Assert;

/**
 * Created by Alberto on 11/03/2016.
 */
public class PeerIdTest {

    @org.junit.Test
    public void test() {

        // missing just 1 char
        PeerId peerId = PeerId.buildTestPeerId("000000000000000000000000000000000000000095");

        Assert.assertEquals("0000000000000000000000000000000000000000095", peerId.toString());
    }

}