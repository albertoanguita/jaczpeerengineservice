package jacz.peerengineservice.test.customfsm;

import jacz.peerengineservice.client.PeerClientData;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.test.Client;
import jacz.peerengineservice.test.PeerClientConfigSerializer;
import jacz.peerengineservice.test.PersonalData;
import jacz.util.lists.Triple;

import java.util.HashMap;

/**
 * Simple connection, no actions
 */
public class TestCustom_1 {

    public static void main(String args[]) throws Exception {
        String config = "./src/main/java/jacz/peerengineservice/test/clientConf_1_new.xml";
        Triple<PersonalData, PeerClientData, PeerRelations> data = PeerClientConfigSerializer.readPeerClientData(config);
        PersonalData personalData = data.element1;
        PeerClientData peerClientData = data.element2;
        PeerRelations peerRelations = data.element3;

        Client client = new Client(personalData, peerClientData, peerRelations, new SimplePeerClientActionImplCustom(), new HashMap<String, PeerFSMFactory>());
        client.startClient();
    }
}
