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
 * Simple connection, no actions
 */
public class TestConnection_1 {

    public static void main(String args[]) throws Exception {
        String config = "./src/main/java/jacz/peerengineservice/test/clientConf_1.xml";
        List<Object> data = PeerClientConfigIO.readPeerClientData(XMLDom.parse(config));
        PersonalData personalData = (PersonalData) data.get(0);
        PeerClientData peerClientData = (PeerClientData) data.get(1);
        PeerRelations peerRelations = (PeerRelations) data.get(2);

        Client client = new Client(personalData, peerClientData, peerRelations, new SimplePeerClientActionImpl(), new HashMap<String, PeerFSMFactory>());
        client.startClient();
    }
}
