package jacz.peerengineservice.test.transfer;

import jacz.peerengineservice.client.PeerClientData;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.test.*;
import jacz.peerengineservice.util.ForeignStoreShare;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.peerengineservice.util.datatransfer.resource_accession.BasicFileWriter;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.lists.Triple;

import java.util.HashMap;

/**
 * Simple connection, no actions
 */
public class TestTransfer_1 {

    public static void main(String args[]) throws Exception {
        String config = "./src/main/java/jacz/peerengineservice/test/clientConf_1_new.xml";
        Triple<PersonalData, PeerClientData, PeerRelations> data = PeerClientConfigSerializer.readPeerClientData(config);
        PersonalData personalData = data.element1;
        PeerClientData peerClientData = data.element2;
        PeerRelations peerRelations = data.element3;

        Client client = new Client(personalData, peerClientData, peerRelations, new SimplePeerClientActionImplTransfer(), new HashMap<String, PeerFSMFactory>());
        ForeignStoreShare foreignStoreShare = new ForeignStoreShare();
        foreignStoreShare.addResourceProvider("aaa", PeerIDGenerator.peerID(2));
        foreignStoreShare.addResourceProvider("bbb", PeerIDGenerator.peerID(2));
//            foreignStoreShare.addResourceProvider("aaa", PeerIDGenerator.peerID(3));
        client.getPeerClient().addForeignResourceStore("files", foreignStoreShare);
        client.startClient();

        ThreadUtil.safeSleep(1000);


        client.getPeerClient().setVisibleDownloadsTimer(5000);
//            client.getPeerClient().downloadResource(new PeerID("pid{0000000000000000000000000000000000000000002}"), "files", "aaa", new BasicFileWriter(".\\aaa_transfer.txt"), true, new DownloadProgressNotificationHandlerImpl(client.getPeerClientData().getOwnPeerID()), 0.1f);
        DownloadManager downloadManager = client.getPeerClient().downloadResource("files", "aaa", new BasicFileWriter(".\\aaa_transfer.txt"), new DownloadProgressNotificationHandlerImpl(client.getPeerClientData().getOwnPeerID()), 0.1f, "5de7d6b2edd3e6b4c0f75562293904bc", "MD5", 1000000L);
        DownloadManager downloadManager2 = client.getPeerClient().downloadResource("files", "bbb", new BasicFileWriter(".\\bbb_transfer.txt"), new DownloadProgressNotificationHandlerImpl(client.getPeerClientData().getOwnPeerID()), 0.1f, "c66d218320ea4b454d0531d09d13792c", "MD5", 1000000L);

        downloadManager2.setPriority(5);
//            client.getPeerClient().setMaxDesiredDownloadSpeed(1500000f);

//            ThreadUtil.safeSleep(300000);
//            downloadManager.pause();
//            ThreadUtil.safeSleep(10000);
//            downloadManager.cancel();

        System.out.println("GO!");
    }
}
