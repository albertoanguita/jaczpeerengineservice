package jacz.peerengine.test.customfsm;

import jacz.peerengine.client.PeerClientData;
import jacz.peerengine.client.PeerFSMFactory;
import jacz.peerengine.client.PeerRelations;
import jacz.peerengine.test.Client;
import jacz.peerengine.test.PeerClientConfigIO;
import jacz.peerengine.test.PersonalData;
import jacz.util.io.xml.XMLDom;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class TestCustom_2 {

    public static void main(String args[]) {
        String config = ".\\trunk\\src\\com.jacuzzi.peerengine\\test\\customfsm\\clientConf_2.xml";
        try {
            Map<String, PeerFSMFactory> customFSMs = new HashMap<String, PeerFSMFactory>();
            customFSMs.put(ProvideFilesFSM.SERVER_FSM, new ProvideFilesFSMFactory());

            List<Object> data = PeerClientConfigIO.readPeerClientData(XMLDom.parse(config));
            PersonalData personalData = (PersonalData) data.get(0);
            PeerClientData peerClientData = (PeerClientData) data.get(1);
            PeerRelations peerRelations = (PeerRelations) data.get(2);

            Client client = new Client(personalData, peerClientData, peerRelations, new SimplePeerClientActionImplCustom(), customFSMs, true);
            client.startClient();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }
}
