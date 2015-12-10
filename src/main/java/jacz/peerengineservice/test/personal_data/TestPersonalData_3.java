package jacz.peerengineservice.test.personal_data;

import jacz.peerengineservice.client.PeerClientData;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeersPersonalData;
import jacz.peerengineservice.test.*;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.lists.Triple;

import java.util.HashMap;

/**
 * Created by Alberto on 08/10/2015.
 */
public class TestPersonalData_3 {

    public static void main(String args[]) throws Exception {
        String config = "./etc/tests/clientConf_3_new.xml";
        Triple<PeersPersonalData, PeerClientData, PeerRelations> data = PeerClientConfigSerializer.readPeerClientData(config);
        PeersPersonalData peersPersonalData = data.element1;
        PeerClientData peerClientData = data.element2;
        PeerRelations peerRelations = data.element3;

        Client client = new Client(peersPersonalData, peerClientData, peerRelations, new GeneralEventsImpl(), new ConnectionEventsImpl(), new HashMap<String, PeerFSMFactory>());
        client.startClient();

        ThreadUtil.safeSleep(11000);
        client.getPeerClient().setNick("mik");

        ThreadUtil.safeSleep(4000);
        client.getPeerClient().setNick("mik2");
    }
}
