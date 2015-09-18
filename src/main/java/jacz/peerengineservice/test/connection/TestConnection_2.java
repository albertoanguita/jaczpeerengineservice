package jacz.peerengineservice.test.connection;

import jacz.peerengineservice.client.PeerClientData;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.test.Client;
import jacz.peerengineservice.test.PeerClientConfigIO;
import jacz.peerengineservice.test.PersonalData;
import jacz.peerengineservice.test.SimplePeerClientActionImpl;
import jacz.util.io.xml.XMLDom;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Class description
 * <p/>
 * User: Alberto<br>
 * Date: 4/05/12<br>
 * Last Modified: 4/05/12
 */
public class TestConnection_2 {

    public static void main(String args[]) throws Exception {
        String config = "./src/main/java/jacz/peerengineservice/test/clientConf_2.xml";
        List<Object> data = PeerClientConfigIO.readPeerClientData(XMLDom.parse(config));
        PersonalData personalData = (PersonalData) data.get(0);
        PeerClientData peerClientData = (PeerClientData) data.get(1);
        PeerRelations peerRelations = (PeerRelations) data.get(2);

        Client client = new Client(personalData, peerClientData, peerRelations, new SimplePeerClientActionImpl(), new HashMap<String, PeerFSMFactory>());
        client.startClient();
    }
}
