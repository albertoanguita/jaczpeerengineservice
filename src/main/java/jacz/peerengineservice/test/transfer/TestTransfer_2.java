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
import jacz.util.io.IOUtil;
import jacz.util.lists.tuple.Four_Tuple;

import java.util.HashMap;

/**
 * test
 */
public class TestTransfer_2 {

    public static void main(String args[]) throws Exception {
        String config = "./etc/tests/clientConf_2_new.xml";
        Four_Tuple<PeerId, NetworkConfiguration, PeersPersonalData, PeerRelations> data = PeerClientConfigSerializer.readPeerClientData(config);
        PeerId ownPeerId = data.element1;
        NetworkConfiguration networkConfiguration = data.element2;
        PeersPersonalData peersPersonalData = data.element3;
        PeerRelations peerRelations = data.element4;

        Client client = new Client(ownPeerId, networkConfiguration, peersPersonalData, peerRelations, new GeneralEventsImpl(), new ConnectionEventsImpl(), new ResourceTransferEventsPlus(), new HashMap<String, PeerFSMFactory>());
        client.getPeerClient().addLocalResourceStore("files", new ResourceStoreImpl());
//        client.getPeerClient().setMaxDesiredUploadSpeed(25000f);
        client.startClient();


        System.out.println("STOP!!!");
        IOUtil.pauseEnter();
        client.stopClient();

        System.out.println("STATISTICS");
        System.out.println("----------");
        System.out.println(client.getTransferStatistics());

    }
}
