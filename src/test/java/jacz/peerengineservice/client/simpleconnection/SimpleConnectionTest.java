package jacz.peerengineservice.client.simpleconnection;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.*;
import jacz.peerengineservice.test.IntegrationTest;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.lists.tuple.SixTuple;
import org.junit.Assert;
import org.junit.experimental.categories.Category;

import java.util.HashMap;

/**
 * Created by Alberto on 27/04/2016.
 */
@Category(IntegrationTest.class)
public class SimpleConnectionTest {

    private static final long WARM_UP = 20000;

    private static final long CYCLE_LENGTH = 5000;

    @org.junit.Test
    public void simpleConnection1() throws Exception {
        String config = "./etc/tests/clientConf_1_new.xml";
        String userDir = "./etc/tests/client1";
        SixTuple<PeerId, String, String, String, String, String> data = ConfigReader.readPeerClientData(config, userDir);
        PeerId ownPeerId = data.element1;
        String networkConfiguration = data.element2;
        String peersPersonalData = data.element3;
        String peerRelations = data.element4;
        String peerConnectionConfig = data.element5;
        String transferStatistics = data.element6;

        Client client = new Client(ownPeerId, networkConfiguration, peersPersonalData, peerRelations, peerConnectionConfig, transferStatistics, new GeneralEventsImpl(), new ConnectionEventsImpl(), new PeersEventsImpl(), new HashMap<>());
        client.getPeerClient().removeFavoritePeer(PeerIdGenerator.peerID(2));
        client.getPeerClient().removeFavoritePeer(PeerIdGenerator.peerID(3));
        client.getPeerClient().removeFavoritePeer(PeerIdGenerator.peerID(4));
        client.getPeerClient().addFavoritePeer(PeerIdGenerator.peerID(2));
        System.out.println("----------------------Connecting...");
        client.startClient();

        Assert.assertFalse(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(2)));

        ThreadUtil.safeSleep(WARM_UP);
        System.out.println("----------------------Finished warm up");
        Assert.assertTrue(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(2)));

        ThreadUtil.safeSleep(CYCLE_LENGTH);
        client.stopClient();
    }

    @org.junit.Test
    public void simpleConnection2() throws Exception {
        String config = "./etc/tests/clientConf_2_new.xml";
        String userDir = "./etc/tests/client2";
        SixTuple<PeerId, String, String, String, String, String> data = ConfigReader.readPeerClientData(config, userDir);
        PeerId ownPeerId = data.element1;
        String networkConfiguration = data.element2;
        String peersPersonalData = data.element3;
        String peerRelations = data.element4;
        String peerConnectionConfig = data.element5;
        String transferStatistics = data.element6;

        Client client = new Client(ownPeerId, networkConfiguration, peersPersonalData, peerRelations, peerConnectionConfig, transferStatistics, new GeneralEventsImpl(), new ConnectionEventsImpl(), new PeersEventsImpl(), new HashMap<>());
        client.getPeerClient().removeFavoritePeer(PeerIdGenerator.peerID(1));
        client.getPeerClient().removeFavoritePeer(PeerIdGenerator.peerID(3));
        client.getPeerClient().removeFavoritePeer(PeerIdGenerator.peerID(4));
        client.getPeerClient().addFavoritePeer(PeerIdGenerator.peerID(1));
        System.out.println("----------------------Connecting...");
        client.startClient();

        Assert.assertFalse(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(1)));

        ThreadUtil.safeSleep(WARM_UP);
        System.out.println("----------------------Finished warm up");
        Assert.assertTrue(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(1)));

        ThreadUtil.safeSleep(CYCLE_LENGTH);

        System.out.println("----------------------peer 1 should now be disconnected...");
        Assert.assertFalse(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(1)));

        ThreadUtil.safeSleep(CYCLE_LENGTH);
        client.stopClient();
    }
}
