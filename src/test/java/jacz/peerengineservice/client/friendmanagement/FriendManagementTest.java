package jacz.peerengineservice.client.friendmanagement;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.*;
import jacz.peerengineservice.test.IntegrationTest;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.lists.tuple.SixTuple;
import org.junit.Assert;
import org.junit.experimental.categories.Category;

import java.util.HashMap;

/**
 * Class description
 * <p/>
 * User: Alberto<br>
 * Date: 5/05/12<br>
 * Last Modified: 5/05/12
 */
@Category(IntegrationTest.class)
public class FriendManagementTest {

    private static final long WARM_UP = 20000;

    private static final long CYCLE_LENGTH = 8000;

    @org.junit.Test
    public void friendManagement1() throws Exception {
        String config = "./etc/tests/clientConf_1_new.xml";
        String userDir = "./etc/tests/client1";
        SixTuple<PeerId, String, String, String, String, String> data = ConfigReader.readPeerClientData(config, userDir);
        PeerId ownPeerId = data.element1;
        String networkConfiguration = data.element2;
        String peersPersonalData = data.element3;
        String peerRelations = data.element4;
        String peerConnectionConfig = data.element5;
        String transferStatistics = data.element6;

        Client client = new Client(ownPeerId, networkConfiguration, peersPersonalData, peerRelations, peerConnectionConfig, transferStatistics, new GeneralEventsImpl(), new ConnectionEventsImpl(), new PeersEventsImpl(), new HashMap<String, PeerFSMFactory>());
        client.startClient();

        Assert.assertTrue(client.getPeerClient().isFavoritePeer(PeerIdGenerator.peerID(2)));
        Assert.assertFalse(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(2)));

        ThreadUtil.safeSleep(WARM_UP);
        Assert.assertTrue(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(2)));

        ThreadUtil.safeSleep(CYCLE_LENGTH);
        Assert.assertFalse(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(2)));

        ThreadUtil.safeSleep(CYCLE_LENGTH);
        Assert.assertFalse(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(2)));

        ThreadUtil.safeSleep(CYCLE_LENGTH);
        Assert.assertFalse(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(2)));

        ThreadUtil.safeSleep(CYCLE_LENGTH);
        Assert.assertTrue(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(2)));

        ThreadUtil.safeSleep(CYCLE_LENGTH / 2);
        client.stopClient();
    }

    @org.junit.Test
    public void friendManagement2() throws Exception {
        String config = "./etc/tests/clientConf_2_new.xml";
        String userDir = "./etc/tests/client2";
        SixTuple<PeerId, String, String, String, String, String> data = ConfigReader.readPeerClientData(config, userDir);
        PeerId ownPeerId = data.element1;
        String networkConfiguration = data.element2;
        String peersPersonalData = data.element3;
        String peerRelations = data.element4;
        String peerConnectionConfig = data.element5;
        String transferStatistics = data.element6;

        Client client = new Client(ownPeerId, networkConfiguration, peersPersonalData, peerRelations, peerConnectionConfig, transferStatistics, new GeneralEventsImpl(), new ConnectionEventsImpl(), new PeersEventsImpl(), new HashMap<String, PeerFSMFactory>());
        client.startClient();

        Assert.assertTrue(client.getPeerClient().isFavoritePeer(PeerIdGenerator.peerID(1)));
        Assert.assertFalse(client.getPeerClient().isBlockedPeer(PeerIdGenerator.peerID(1)));
        Assert.assertFalse(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(1)));

        ThreadUtil.safeSleep(WARM_UP);
        Assert.assertTrue(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(1)));

        System.out.println("2 removes 1");
        client.getPeerClient().removeFavoritePeer(PeerIdGenerator.peerID(1));
        ThreadUtil.safeSleep(CYCLE_LENGTH);
        Assert.assertFalse(client.getPeerClient().isFavoritePeer(PeerIdGenerator.peerID(1)));
        Assert.assertFalse(client.getPeerClient().isBlockedPeer(PeerIdGenerator.peerID(1)));
        Assert.assertFalse(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(1)));

        System.out.println("2 blocks 1");
        client.getPeerClient().addBlockedPeer(PeerIdGenerator.peerID(1));
        ThreadUtil.safeSleep(CYCLE_LENGTH);
        Assert.assertFalse(client.getPeerClient().isFavoritePeer(PeerIdGenerator.peerID(1)));
        Assert.assertTrue(client.getPeerClient().isBlockedPeer(PeerIdGenerator.peerID(1)));
        Assert.assertFalse(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(1)));

        System.out.println("2 removes block to 1");
        client.getPeerClient().removeBlockedPeer(PeerIdGenerator.peerID(1));
        ThreadUtil.safeSleep(CYCLE_LENGTH);
        Assert.assertFalse(client.getPeerClient().isFavoritePeer(PeerIdGenerator.peerID(1)));
        Assert.assertFalse(client.getPeerClient().isBlockedPeer(PeerIdGenerator.peerID(1)));
        Assert.assertFalse(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(1)));

        System.out.println("2 adds 1");
        client.getPeerClient().addFavoritePeer(PeerIdGenerator.peerID(1));
        ThreadUtil.safeSleep(CYCLE_LENGTH);
        Assert.assertTrue(client.getPeerClient().isFavoritePeer(PeerIdGenerator.peerID(1)));
        Assert.assertFalse(client.getPeerClient().isBlockedPeer(PeerIdGenerator.peerID(1)));
        Assert.assertTrue(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(1)));

        ThreadUtil.safeSleep(CYCLE_LENGTH / 2);
        client.stopClient();
    }
}
