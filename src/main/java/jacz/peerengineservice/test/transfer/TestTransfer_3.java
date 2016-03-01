package jacz.peerengineservice.test.transfer;

import jacz.peerengineservice.PeerId;
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

/**
 * test
 */
public class TestTransfer_3 {

    public static void main(String args[]) throws Exception {
        String config = "./etc/tests/clientConf_3_new.xml";
        Four_Tuple<PeerId, NetworkConfiguration, PeersPersonalData, PeerRelations> data = PeerClientConfigSerializer.readPeerClientData(config);
        PeerId ownPeerId = data.element1;
        NetworkConfiguration networkConfiguration = data.element2;
        PeersPersonalData peersPersonalData = data.element3;
        PeerRelations peerRelations = data.element4;

        Client client = new Client(ownPeerId, networkConfiguration, peersPersonalData, peerRelations, new GeneralEventsImpl(), new ConnectionEventsImpl(), new ResourceTransferEventsPlus(), new HashMap<String, PeerFSMFactory>());
        client.getPeerClient().addLocalResourceStore("files", new ResourceStoreImpl());
//        client.getPeerClient().setMaxDesiredUploadSpeed(50000f);
        client.startClient();
    }
}
