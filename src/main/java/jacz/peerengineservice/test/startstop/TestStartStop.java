package jacz.peerengineservice.test.startstop;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeersPersonalData;
import jacz.peerengineservice.client.connection.NetworkConfiguration;
import jacz.peerengineservice.test.Client;
import jacz.peerengineservice.test.ConnectionEventsImpl;
import jacz.peerengineservice.test.GeneralEventsImpl;
import jacz.peerengineservice.test.PeerClientConfigSerializer;
import jacz.util.lists.tuple.Four_Tuple;

import java.util.HashMap;

/**
 * Test with a client that starts, connects, and then stops, to see if it properly closes all its resources
 */
public class TestStartStop {

    public static void main(String args[]) throws Exception {

        String config = "./etc/tests/clientConf_1_new.xml";
        Four_Tuple<PeerId, NetworkConfiguration, PeersPersonalData, PeerRelations> data = PeerClientConfigSerializer.readPeerClientData(config);
        PeerId ownPeerId = data.element1;
        NetworkConfiguration networkConfiguration = data.element2;
        PeersPersonalData peersPersonalData = data.element3;
        PeerRelations peerRelations = data.element4;

        Client client = new Client(ownPeerId, networkConfiguration, peersPersonalData, peerRelations, new GeneralEventsImpl(), new ConnectionEventsImpl(), new HashMap<String, PeerFSMFactory>());
        client.startClient();

        Thread.sleep(25000);
        System.out.println("Client stopping...");
        client.stopClient();
        System.out.println("Client stopped! END!!!");
        Thread.sleep(2000);
        System.out.println("END");
    }
}
