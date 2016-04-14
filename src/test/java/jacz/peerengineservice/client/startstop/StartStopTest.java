package jacz.peerengineservice.client.startstop;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.Client;
import jacz.peerengineservice.client.ConfigReader;
import jacz.peerengineservice.client.ConnectionEventsImpl;
import jacz.peerengineservice.client.GeneralEventsImpl;
import jacz.peerengineservice.client.connection.State;
import jacz.peerengineservice.test.IntegrationTest;
import jacz.util.lists.tuple.Four_Tuple;
import org.junit.Assert;
import org.junit.experimental.categories.Category;

import java.util.HashMap;

/**
 * Test with a client that starts, connects, and then stops, to see if it properly closes all its resources
 */
@Category(IntegrationTest.class)
public class StartStopTest {

    @org.junit.Test
    public void test() throws Exception {

        String config = "./etc/tests/clientConf_1_new.xml";
        String userDir = "./etc/tests/client1";
        Four_Tuple<PeerId, String, String, String> data = ConfigReader.readPeerClientData(config, userDir);
        PeerId ownPeerId = data.element1;
        String networkConfiguration = data.element2;
        String peersPersonalData = data.element3;
        String peerRelations = data.element4;

        Client client = new Client(ownPeerId, networkConfiguration, peersPersonalData, peerRelations, new GeneralEventsImpl(), new ConnectionEventsImpl(), new HashMap<>());
        client.startClient();

        Thread.sleep(25000);
        Assert.assertEquals(State.NetworkTopologyState.ALL_FETCHED, client.getPeerClient().getConnectionState().getNetworkTopologyState());
        Assert.assertEquals(State.LocalServerConnectionsState.LISTENING, client.getPeerClient().getConnectionState().getLocalServerConnectionsState());
        Assert.assertEquals(State.ConnectionToServerState.CONNECTED, client.getPeerClient().getConnectionState().getConnectionToServerState());

        System.out.println("Client stopping...");
        client.stopClient();
        System.out.println("Client stopped! END!!!");
        Assert.assertEquals(State.ConnectionToServerState.DISCONNECTED, client.getPeerClient().getConnectionState().getConnectionToServerState());
    }
}
