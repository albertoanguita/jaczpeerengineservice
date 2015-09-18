package jacz.peerengineservice.test.friend_management;

import jacz.peerengineservice.client.PeerClientData;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.test.Client;
import jacz.peerengineservice.test.PeerClientConfigIO;
import jacz.peerengineservice.test.PersonalData;
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
public class TestComm_1 {

    public static void main(String args[]) throws Exception {
        String config = "./src/main/java/jacz/peerengineservice/test/clientConf_1.xml";
        List<Object> data = PeerClientConfigIO.readPeerClientData(XMLDom.parse(config));
        PersonalData personalData = (PersonalData) data.get(0);
        PeerClientData peerClientData = (PeerClientData) data.get(1);
        PeerRelations peerRelations = (PeerRelations) data.get(2);

        Client client = new Client(personalData, peerClientData, peerRelations, new SimplePeerClientActionImplComm(), new HashMap<String, PeerFSMFactory>());
        client.startClient();
    }
}
