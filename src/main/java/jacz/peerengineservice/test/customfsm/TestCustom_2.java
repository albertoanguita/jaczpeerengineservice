package jacz.peerengineservice.test.customfsm;

import jacz.peerengineservice.client.PeerClientData;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeersPersonalData;
import jacz.peerengineservice.test.Client;
import jacz.peerengineservice.test.PeerClientConfigSerializer;
import jacz.peerengineservice.test.PersonalData;
import jacz.util.lists.Triple;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class TestCustom_2 {

    public static void main(String args[]) throws Exception {
        String config = "./etc/tests/clientConf_2_new.xml";
        Triple<PeersPersonalData, PeerClientData, PeerRelations> data = PeerClientConfigSerializer.readPeerClientData(config);
        PeersPersonalData peersPersonalData = data.element1;
        PeerClientData peerClientData = data.element2;
        PeerRelations peerRelations = data.element3;
        Map<String, PeerFSMFactory> customFSMs = new HashMap<>();
        customFSMs.put(ProvideFilesFSM.SERVER_FSM, new ProvideFilesFSMFactory());

        Client client = new Client(peersPersonalData, peerClientData, peerRelations, new SimplePeerClientActionImplCustom(), customFSMs);
        client.startClient();
    }
}
