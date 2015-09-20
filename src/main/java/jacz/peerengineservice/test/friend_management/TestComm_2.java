package jacz.peerengineservice.test.friend_management;

import jacz.peerengineservice.client.PeerClientData;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.test.*;
import jacz.util.io.xml.XMLDom;
import jacz.util.lists.Triple;

import java.util.HashMap;
import java.util.List;

/**
 * Class description
 * <p/>
 * User: Alberto<br>
 * Date: 5/05/12<br>
 * Last Modified: 5/05/12
 */
public class TestComm_2 {

    public static void main(String args[]) throws Exception {
        String config = "./src/main/java/jacz/peerengineservice/test/clientConf_2_new.xml";
        Triple<PersonalData, PeerClientData, PeerRelations> data = PeerClientConfigSerializer.readPeerClientData(config);
        PersonalData personalData = data.element1;
        PeerClientData peerClientData = data.element2;
        PeerRelations peerRelations = data.element3;

        Client client = new Client(personalData, peerClientData, peerRelations, new SimplePeerClientActionImplComm(), new HashMap<String, PeerFSMFactory>());
        client.startClient();

        Thread.sleep(3000);
        System.out.println("2 elimina a 1");
        client.getPeerClient().removeFriendPeer(PeerIDGenerator.peerID(1));

        Thread.sleep(10000);
        System.out.println("2 bloquea a 1");
        client.getPeerClient().addBlockedPeer(PeerIDGenerator.peerID(1));

        Thread.sleep(3000);
        System.out.println("2 quita bloqueo a 1");
        client.getPeerClient().removeBlockedPeer(PeerIDGenerator.peerID(1));

        Thread.sleep(15000);
        System.out.println("2 añade a 1");
        client.getPeerClient().addFriendPeer(PeerIDGenerator.peerID(1));
        System.out.println("2: el estado de 1 es ahora " + client.getPeerClient().getPeerConnectionStatus(PeerIDGenerator.peerID(1)));
        Thread.sleep(5000);
        System.out.println("2: el estado de 1 es ahora " + client.getPeerClient().getPeerConnectionStatus(PeerIDGenerator.peerID(1)));
    }
}
