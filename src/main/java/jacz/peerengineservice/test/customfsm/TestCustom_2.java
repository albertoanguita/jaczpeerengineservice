package jacz.peerengineservice.test.customfsm;

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
import java.util.Map;

/**
 *
 */
public class TestCustom_2 {

    public static void main(String args[]) throws Exception {
        String config = "./etc/tests/clientConf_2_new.xml";
        Four_Tuple<PeerID, NetworkConfiguration, PeersPersonalData, PeerRelations> data = PeerClientConfigSerializer.readPeerClientData(config);
        PeerID ownPeerID = data.element1;
        NetworkConfiguration networkConfiguration = data.element2;
        PeersPersonalData peersPersonalData = data.element3;
        PeerRelations peerRelations = data.element4;
        Map<String, PeerFSMFactory> customFSMs = new HashMap<>();
        customFSMs.put(ProvideFilesFSM.SERVER_FSM, new ProvideFilesFSMFactory());

        Client client = new Client(ownPeerID, networkConfiguration, peersPersonalData, peerRelations, new GeneralEventsImpl(), new ConnectionEventsImpl(), customFSMs);
        client.startClient();
    }
}
