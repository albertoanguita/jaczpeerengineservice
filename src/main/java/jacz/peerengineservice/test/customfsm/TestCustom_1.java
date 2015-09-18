package jacz.peerengineservice.test.customfsm;

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
 * Simple connection, no actions
 */
public class TestCustom_1 {

    public static void main(String args[]) {
        String config = ".\\trunk\\src\\com.jacuzzi.peerengineservice\\test\\customfsm\\clientConf_1.xml";
        try {
            List<Object> data = PeerClientConfigIO.readPeerClientData(XMLDom.parse(config));
            PersonalData personalData = (PersonalData) data.get(0);
            PeerClientData peerClientData = (PeerClientData) data.get(1);
            PeerRelations peerRelations = (PeerRelations) data.get(2);

            Client client = new Client(personalData, peerClientData, peerRelations, new SimplePeerClientActionImplCustom(), new HashMap<String, PeerFSMFactory>());
            client.startClient();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }
}
