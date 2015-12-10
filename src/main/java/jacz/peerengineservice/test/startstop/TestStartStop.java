package jacz.peerengineservice.test.startstop;

import jacz.peerengineservice.client.PeerClientData;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeersPersonalData;
import jacz.peerengineservice.test.*;
import jacz.peerengineservice.test.list_synch.GeneralEventsSynch;
import jacz.util.lists.Triple;

import java.util.HashMap;

/**
 * Test with a client that starts, connects, and then stops, to see if it properly closes all its resources
 */
public class TestStartStop {

    public static void main(String args[]) throws Exception {
        String config = "./etc/tests/clientConf_1_new.xml";
        Triple<PeersPersonalData, PeerClientData, PeerRelations> data = PeerClientConfigSerializer.readPeerClientData(config);
        PeersPersonalData peersPersonalData = data.element1;
        PeerClientData peerClientData = data.element2;
        PeerRelations peerRelations = data.element3;

        Client client = new Client(peersPersonalData, peerClientData, peerRelations, new GeneralEventsImpl(), new ConnectionEventsImpl(), new HashMap<String, PeerFSMFactory>());
        client.startClient();

        Thread.sleep(25000);
        System.out.println("Client stopping...");
        client.stopClient();
        System.out.println("Client stopped! END!!!");
        Thread.sleep(2000);
        System.out.println("END");
    }
}
