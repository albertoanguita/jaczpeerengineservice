package jacz.peerengineservice.test.transfer;

import jacz.peerengineservice.client.PeerClientData;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeersPersonalData;
import jacz.peerengineservice.test.Client;
import jacz.peerengineservice.test.PeerClientConfigSerializer;
import jacz.peerengineservice.test.SimplePeerClientActionImpl;
import jacz.util.lists.Triple;

import java.util.HashMap;

/**
 * test
 */
public class TestTransfer_3 {

    public static void main(String args[]) throws Exception {
        String config = "./src/main/java/jacz/peerengineservice/test/clientConf_3_new.xml";
        Triple<PeersPersonalData, PeerClientData, PeerRelations> data = PeerClientConfigSerializer.readPeerClientData(config);
        PeersPersonalData peersPersonalData = data.element1;
        PeerClientData peerClientData = data.element2;
        PeerRelations peerRelations = data.element3;

        Client client = new Client(peersPersonalData, peerClientData, peerRelations, new SimplePeerClientActionImpl(), new HashMap<String, PeerFSMFactory>());
        client.getPeerClient().addLocalResourceStore("files", new ResourceStoreImpl());
//        client.getPeerClient().setMaxDesiredUploadSpeed(50000f);
        client.startClient();
    }
}
