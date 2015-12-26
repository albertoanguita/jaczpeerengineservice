package jacz.peerengineservice.test.friend_management;

import jacz.peerengineservice.PeerID;
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
 * Class description
 * <p/>
 * User: Alberto<br>
 * Date: 5/05/12<br>
 * Last Modified: 5/05/12
 */
public class TestComm_2 {

    public static void main(String args[]) throws Exception {
        String config = "./etc/tests/clientConf_2_new.xml";
        Four_Tuple<PeerID, NetworkConfiguration, PeersPersonalData, PeerRelations> data = PeerClientConfigSerializer.readPeerClientData(config);
        PeerID ownPeerID = data.element1;
        NetworkConfiguration networkConfiguration = data.element2;
        PeersPersonalData peersPersonalData = data.element3;
        PeerRelations peerRelations = data.element4;

        Client client = new Client(ownPeerID, networkConfiguration, peersPersonalData, peerRelations, new GeneralEventsImpl(), new ConnectionEventsImpl(), new HashMap<String, PeerFSMFactory>());
        client.startClient();

//        Thread.sleep(3000);
//        System.out.println("2 elimina a 1");
//        client.getPeerClient().removeFriendPeer(PeerIDGenerator.peerID(1));
//
//        Thread.sleep(10000);
//        System.out.println("2 bloquea a 1");
//        client.getPeerClient().addBlockedPeer(PeerIDGenerator.peerID(1));
//
//        Thread.sleep(3000);
//        System.out.println("2 quita bloqueo a 1");
//        client.getPeerClient().removeBlockedPeer(PeerIDGenerator.peerID(1));
//
//        Thread.sleep(15000);
//        System.out.println("2 añade a 1");
//        client.getPeerClient().addFriendPeer(PeerIDGenerator.peerID(1));
//        System.out.println("2: el estado de 1 es ahora " + client.getPeerClient().getPeerConnectionStatus(PeerIDGenerator.peerID(1)));
//        Thread.sleep(5000);
//        System.out.println("2: el estado de 1 es ahora " + client.getPeerClient().getPeerConnectionStatus(PeerIDGenerator.peerID(1)));
    }
}
