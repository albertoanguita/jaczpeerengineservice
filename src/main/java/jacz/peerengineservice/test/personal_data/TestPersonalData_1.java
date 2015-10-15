package jacz.peerengineservice.test.personal_data;

import jacz.peerengineservice.client.PeerClientData;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeersPersonalData;
import jacz.peerengineservice.test.Client;
import jacz.peerengineservice.test.PeerClientConfigSerializer;
import jacz.peerengineservice.test.SimplePeerClientActionImpl;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.lists.Triple;

import java.util.HashMap;

/**
 * Created by Alberto on 08/10/2015.
 */
public class TestPersonalData_1 {

    public static void main(String args[]) throws Exception {
        String config = "./etc/tests/clientConf_1_new.xml";
        Triple<PeersPersonalData, PeerClientData, PeerRelations> data = PeerClientConfigSerializer.readPeerClientData(config);
        PeersPersonalData peersPersonalData = data.element1;
        PeerClientData peerClientData = data.element2;
        PeerRelations peerRelations = data.element3;

        Client client = new Client(peersPersonalData, peerClientData, peerRelations, new SimplePeerClientActionImpl(), new HashMap<String, PeerFSMFactory>());
        client.startClient();

        ThreadUtil.safeSleep(15000);
        client.getPeerClient().setNick("albertooooo");

        ThreadUtil.safeSleep(10000);
        client.getPeerClient().setNick("alberto2");

        ThreadUtil.safeSleep(5000);
        client.getPeerClient().setNick("alberto3");
    }
}
