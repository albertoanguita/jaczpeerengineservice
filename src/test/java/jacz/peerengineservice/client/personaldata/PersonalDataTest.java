package jacz.peerengineservice.client.personaldata;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.*;
import jacz.peerengineservice.test.IntegrationTest;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.lists.tuple.Four_Tuple;
import org.junit.Assert;
import org.junit.experimental.categories.Category;

import java.util.HashMap;

/**
 * Nick tests
 */
@Category(IntegrationTest.class)
public class PersonalDataTest {

    private static final long WARM_UP = 20000;

    private static final long CYCLE_LENGTH = 8000;

    @org.junit.Test
    public void personalDataTest1() throws Exception {
        String config = "./etc/tests/clientConf_1_new.xml";
        String userDir = "./etc/tests/client1";
        Four_Tuple<PeerId, String, String, String> data = ConfigReader.readPeerClientData(config, userDir);
        PeerId ownPeerId = data.element1;
        String networkConfiguration = data.element2;
        String peersPersonalData = data.element3;
        String peerRelations = data.element4;

        Client client = new Client(ownPeerId, networkConfiguration, peersPersonalData, peerRelations, new GeneralEventsImpl(), new ConnectionEventsImpl(), new HashMap<>());
        client.startClient();
        int cycle = 1;

        Assert.assertEquals("jor", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(2)));
        Assert.assertEquals("miki", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(3)));

        ThreadUtil.safeSleep(WARM_UP);
        System.out.println("end warm up");
        Assert.assertEquals("jor", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(2)));
        Assert.assertEquals("miki265", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(3)));

        cycle = waitCycle(cycle);
        client.getPeerClient().setOwnNick("albertooooo");

        cycle = waitCycle(cycle);
        Assert.assertEquals("jorgi", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(2)));
        Assert.assertEquals("mik", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(3)));

        cycle = waitCycle(cycle);
        client.getPeerClient().setOwnNick("alberto2");

        cycle = waitCycle(cycle);
        Assert.assertEquals("jorgi2", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(2)));
        Assert.assertEquals("mik", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(3)));

        cycle = waitCycle(cycle);
        client.getPeerClient().setOwnNick("alberto3");

        waitCycle(cycle);
        Assert.assertEquals("jorgi2", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(2)));
        Assert.assertEquals("mik2", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(3)));

        client.stopClient();
    }

    @org.junit.Test
    public void personalDataTest2() throws Exception {
        String config = "./etc/tests/clientConf_2_new.xml";
        String userDir = "./etc/tests/client2";
        Four_Tuple<PeerId, String, String, String> data = ConfigReader.readPeerClientData(config, userDir);
        PeerId ownPeerId = data.element1;
        String networkConfiguration = data.element2;
        String peersPersonalData = data.element3;
        String peerRelations = data.element4;

        Client client = new Client(ownPeerId, networkConfiguration, peersPersonalData, peerRelations, new GeneralEventsImpl(), new ConnectionEventsImpl(), new HashMap<>());
        client.startClient();
        int cycle = 1;

        Assert.assertEquals("alber", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(1)));
        Assert.assertEquals("mikia", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(3)));

        ThreadUtil.safeSleep(WARM_UP);
        System.out.println("end warm up");
        Assert.assertEquals("alb", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(1)));
        Assert.assertEquals("miki265", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(3)));

        cycle = waitCycle(cycle);
        client.getPeerClient().setOwnNick("jorgi");

        cycle = waitCycle(cycle);
        Assert.assertEquals("albertooooo", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(1)));
        Assert.assertEquals("mik", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(3)));

        cycle = waitCycle(cycle);
        client.getPeerClient().setOwnNick("jorgi2");

        cycle = waitCycle(cycle);
        Assert.assertEquals("alberto2", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(1)));
        Assert.assertEquals("mik", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(3)));

        cycle = waitCycle(cycle);
        // no nick change

        waitCycle(cycle);
        Assert.assertEquals("alberto3", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(1)));
        Assert.assertEquals("mik2", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(3)));

        client.stopClient();
    }

    @org.junit.Test
    public void personalDataTest3() throws Exception {
        String config = "./etc/tests/clientConf_3_new.xml";
        String userDir = "./etc/tests/client3";
        Four_Tuple<PeerId, String, String, String> data = ConfigReader.readPeerClientData(config, userDir);
        PeerId ownPeerId = data.element1;
        String networkConfiguration = data.element2;
        String peersPersonalData = data.element3;
        String peerRelations = data.element4;

        Client client = new Client(ownPeerId, networkConfiguration, peersPersonalData, peerRelations, new GeneralEventsImpl(), new ConnectionEventsImpl(), new HashMap<>());
        client.startClient();
        int cycle = 1;

        Assert.assertEquals("alb", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(1)));
        Assert.assertEquals("jor", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(2)));

        ThreadUtil.safeSleep(WARM_UP);
        System.out.println("end warm up");
        Assert.assertEquals("alb", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(1)));
        Assert.assertEquals("jor", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(2)));

        cycle = waitCycle(cycle);
        client.getPeerClient().setOwnNick("mik");

        cycle = waitCycle(cycle);
        Assert.assertEquals("albertooooo", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(1)));
        Assert.assertEquals("jorgi", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(2)));

        cycle = waitCycle(cycle);
        // no nick change

        cycle = waitCycle(cycle);
        Assert.assertEquals("alberto2", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(1)));
        Assert.assertEquals("jorgi2", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(2)));

        cycle = waitCycle(cycle);
        client.getPeerClient().setOwnNick("mik2");

        waitCycle(cycle);
        Assert.assertEquals("alberto3", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(1)));
        Assert.assertEquals("jorgi2", client.getPeerClient().getPeerNick(PeerIdGenerator.peerID(2)));

        client.stopClient();
    }

    private int waitCycle(int cycle) {
        ThreadUtil.safeSleep(CYCLE_LENGTH);
        System.out.println("cycle " + cycle);
        return cycle + 1;
    }
}
