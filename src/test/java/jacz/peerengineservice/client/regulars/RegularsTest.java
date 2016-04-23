package jacz.peerengineservice.client.regulars;

import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.*;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.lists.tuple.SixTuple;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Regulars test
 *
 * In this test we use 4 peers. There are no favorites:
 * - peer1: main peer, first peer to connect. Main country: ES. Only has 1 slot for regulars, and one additional
 * country: US. Next, will reduce slots for regulars to 0
 * - peer2: second peer to connect: Main country: ES. Will connect to peer1. No additional countries. Will disconnect
 * right after connecting.
 * - peer3: third peer to connect: Main country: AR, additional country: ES. Will connect to peer1
 * - peer4: fourth country to connect: will try to connect to peer1, but it will be full. It will though get info
 * about peer2, which will show up in the logs
 */
public class RegularsTest {

    private static final long WARM_UP = 40000;

    private static final long CYCLE_LENGTH = 8000;

    @org.junit.Test
    public void regularsTest1() throws Exception {
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
        client.getPeerClient().removeFavoritePeer(PeerIdGenerator.peerID(1));
        client.getPeerClient().removeFavoritePeer(PeerIdGenerator.peerID(2));
        client.getPeerClient().removeFavoritePeer(PeerIdGenerator.peerID(3));
        client.getPeerClient().removeFavoritePeer(PeerIdGenerator.peerID(4));
        client.getPeerClient().setWishForRegularsConnections(true);
        client.getPeerClient().setMainCountry(CountryCode.ES);
        client.getPeerClient().setMaxRegularConnections(1);
        List<CountryCode> additionalCountries = new ArrayList<>();
        additionalCountries.add(CountryCode.US);
        client.getPeerClient().setAdditionalCountries(additionalCountries);
        client.startClient();

        ThreadUtil.safeSleep(WARM_UP);
        Assert.assertTrue(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(2)));

//        ThreadUtil.safeSleep(CYCLE_LENGTH);
//        Assert.assertFalse(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(2)));
//
//        ThreadUtil.safeSleep(CYCLE_LENGTH);
//        Assert.assertFalse(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(2)));
//
//        ThreadUtil.safeSleep(CYCLE_LENGTH);
//        Assert.assertFalse(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(2)));
//
//        ThreadUtil.safeSleep(CYCLE_LENGTH);
//        Assert.assertTrue(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(2)));
//
//        ThreadUtil.safeSleep(CYCLE_LENGTH / 2);
        client.stopClient();
    }

    @org.junit.Test
    public void regularsTest2() throws Exception {
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
        client.getPeerClient().removeFavoritePeer(PeerIdGenerator.peerID(2));
        client.getPeerClient().removeFavoritePeer(PeerIdGenerator.peerID(3));
        client.getPeerClient().removeFavoritePeer(PeerIdGenerator.peerID(4));
        client.getPeerClient().setWishForRegularsConnections(true);
        client.getPeerClient().setMainCountry(CountryCode.ES);
        client.startClient();

        ThreadUtil.safeSleep(WARM_UP);
        Assert.assertTrue(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(1)));

//        System.out.println("2 removes 1");
//        client.getPeerClient().removeFavoritePeer(PeerIdGenerator.peerID(1));
//        ThreadUtil.safeSleep(CYCLE_LENGTH);
//        Assert.assertFalse(client.getPeerClient().isFavoritePeer(PeerIdGenerator.peerID(1)));
//        Assert.assertFalse(client.getPeerClient().isBlockedPeer(PeerIdGenerator.peerID(1)));
//        Assert.assertFalse(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(1)));
//
//        System.out.println("2 blocks 1");
//        client.getPeerClient().addBlockedPeer(PeerIdGenerator.peerID(1));
//        ThreadUtil.safeSleep(CYCLE_LENGTH);
//        Assert.assertFalse(client.getPeerClient().isFavoritePeer(PeerIdGenerator.peerID(1)));
//        Assert.assertTrue(client.getPeerClient().isBlockedPeer(PeerIdGenerator.peerID(1)));
//        Assert.assertFalse(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(1)));
//
//        System.out.println("2 removes block to 1");
//        client.getPeerClient().removeBlockedPeer(PeerIdGenerator.peerID(1));
//        ThreadUtil.safeSleep(CYCLE_LENGTH);
//        Assert.assertFalse(client.getPeerClient().isFavoritePeer(PeerIdGenerator.peerID(1)));
//        Assert.assertFalse(client.getPeerClient().isBlockedPeer(PeerIdGenerator.peerID(1)));
//        Assert.assertFalse(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(1)));
//
//        System.out.println("2 adds 1");
//        client.getPeerClient().addFavoritePeer(PeerIdGenerator.peerID(1));
//        ThreadUtil.safeSleep(CYCLE_LENGTH);
//        Assert.assertTrue(client.getPeerClient().isFavoritePeer(PeerIdGenerator.peerID(1)));
//        Assert.assertFalse(client.getPeerClient().isBlockedPeer(PeerIdGenerator.peerID(1)));
//        Assert.assertTrue(client.getPeerClient().isConnectedPeer(PeerIdGenerator.peerID(1)));
//
//        ThreadUtil.safeSleep(CYCLE_LENGTH / 2);
        client.stopClient();
    }

}
