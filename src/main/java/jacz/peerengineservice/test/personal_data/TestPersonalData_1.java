package jacz.peerengineservice.test.personal_data;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeersPersonalData;
import jacz.peerengineservice.client.connection.NetworkConfiguration;
import jacz.peerengineservice.test.Client;
import jacz.peerengineservice.test.ConnectionEventsImpl;
import jacz.peerengineservice.test.GeneralEventsImpl;
import jacz.peerengineservice.test.PeerClientConfigSerializer;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.lists.tuple.Four_Tuple;

import java.util.HashMap;

/**
 * Created by Alberto on 08/10/2015.
 */
public class TestPersonalData_1 {

    public static void main(String args[]) throws Exception {
        String config = "./etc/tests/clientConf_1_new.xml";
        Four_Tuple<PeerID, NetworkConfiguration, PeersPersonalData, PeerRelations> data = PeerClientConfigSerializer.readPeerClientData(config);
        PeerID ownPeerID = data.element1;
        NetworkConfiguration networkConfiguration = data.element2;
        PeersPersonalData peersPersonalData = data.element3;
        PeerRelations peerRelations = data.element4;

        Client client = new Client(ownPeerID, networkConfiguration, peersPersonalData, peerRelations, new GeneralEventsImpl(), new ConnectionEventsImpl(), new HashMap<String, PeerFSMFactory>());
        client.startClient();

        ThreadUtil.safeSleep(15000);
        client.getPeerClient().setNick("albertooooo");

        ThreadUtil.safeSleep(10000);
        client.getPeerClient().setNick("alberto2");

        ThreadUtil.safeSleep(5000);
        client.getPeerClient().setNick("alberto3");
    }
}
