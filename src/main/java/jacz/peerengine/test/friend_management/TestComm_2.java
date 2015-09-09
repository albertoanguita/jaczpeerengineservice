package jacz.peerengine.test.friend_management;

import jacz.peerengine.client.PeerClientData;
import jacz.peerengine.client.PeerFSMFactory;
import jacz.peerengine.client.PeerRelations;
import jacz.peerengine.test.Client;
import jacz.peerengine.test.PeerClientConfigIO;
import jacz.peerengine.test.PeerIDGenerator;
import jacz.peerengine.test.PersonalData;
import jacz.util.io.xml.XMLDom;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
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

    public static void main(String args[]) {
        String config = ".\\trunk\\src\\com.jacuzzi.peerengine\\test\\friend_management\\clientConf_2.xml";

        try {
            List<Object> data = PeerClientConfigIO.readPeerClientData(XMLDom.parse(config));
            PersonalData personalData = (PersonalData) data.get(0);
            PeerClientData peerClientData = (PeerClientData) data.get(1);
            PeerRelations peerRelations = (PeerRelations) data.get(2);

            Client client = new Client(personalData, peerClientData, peerRelations, new SimplePeerClientActionImplComm(), new HashMap<String, PeerFSMFactory>(), true);
            client.startClient();

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("2 elimina a 1");
            client.getPeerClient().removeFriendPeer(PeerIDGenerator.peerID(1));

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("2 bloquea a 1");
            client.getPeerClient().addBlockedPeer(PeerIDGenerator.peerID(1));

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("2 quita bloqueo a 1");
            client.getPeerClient().removeBlockedPeer(PeerIDGenerator.peerID(1));

            try {
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("2 añade a 1");
            client.getPeerClient().addFriendPeer(PeerIDGenerator.peerID(1));
            System.out.println("2: el estado de 1 es ahora " + client.getPeerClient().getPeerConnectionStatus(PeerIDGenerator.peerID(1)));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }
}
