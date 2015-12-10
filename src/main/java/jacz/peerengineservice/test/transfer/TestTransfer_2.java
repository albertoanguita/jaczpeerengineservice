package jacz.peerengineservice.test.transfer;

import jacz.peerengineservice.client.PeerClientData;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeersPersonalData;
import jacz.peerengineservice.test.*;
import jacz.util.io.IOUtil;
import jacz.util.lists.Triple;

import java.util.HashMap;

/**
 * test
 */
public class TestTransfer_2 {

    public static void main(String args[]) throws Exception {
        String config = "./etc/tests/clientConf_2_new.xml";
        Triple<PeersPersonalData, PeerClientData, PeerRelations> data = PeerClientConfigSerializer.readPeerClientData(config);
        PeersPersonalData peersPersonalData = data.element1;
        PeerClientData peerClientData = data.element2;
        PeerRelations peerRelations = data.element3;

        Client client = new Client(peersPersonalData, peerClientData, peerRelations, new GeneralEventsImpl(), new ConnectionEventsImpl(), new ResourceTransferEventsPlus(), new HashMap<String, PeerFSMFactory>());
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
