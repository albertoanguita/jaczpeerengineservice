package jacz.peerengineservice.client.startstop;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.*;
import jacz.peerengineservice.client.connection.ConnectionState;
import jacz.peerengineservice.test.IntegrationTest;
import jacz.util.lists.tuple.SixTuple;
import org.junit.Assert;
import org.junit.experimental.categories.Category;

import java.util.HashMap;

/**
 * Test with a client that starts, connects, and then stops, to see if it properly closes all its resources
 */
@Category(IntegrationTest.class)
public class StartStopTest {

    @org.junit.Test
    public void startStopTest() throws Exception {

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
        client.startClient();

        Thread.sleep(35000);
        Assert.assertEquals(ConnectionState.NetworkTopologyState.ALL_FETCHED, client.getPeerClient().getConnectionState().getNetworkTopologyState());
        Assert.assertEquals(ConnectionState.LocalServerConnectionsState.LISTENING, client.getPeerClient().getConnectionState().getLocalServerConnectionsState());
        Assert.assertEquals(ConnectionState.ConnectionToServerState.CONNECTED, client.getPeerClient().getConnectionState().getConnectionToServerState());

        System.out.println("Client stopping...");
        client.stopClient();
        System.out.println("Client stopped! END!!!");
        Assert.assertEquals(ConnectionState.ConnectionToServerState.DISCONNECTED, client.getPeerClient().getConnectionState().getConnectionToServerState());
    }
}
