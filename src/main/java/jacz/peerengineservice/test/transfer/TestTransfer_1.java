package jacz.peerengineservice.test.transfer;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeersPersonalData;
import jacz.peerengineservice.client.connection.NetworkConfiguration;
import jacz.peerengineservice.test.*;
import jacz.peerengineservice.util.ForeignStoreShare;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.peerengineservice.util.datatransfer.resource_accession.BasicFileWriter;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.io.IOUtil;
import jacz.util.lists.tuple.Four_Tuple;

import java.util.HashMap;

/**
 * Simple connection, no actions
 */
public class TestTransfer_1 {

    public static void main(String args[]) throws Exception {
        String config = "./etc/tests/clientConf_1_new.xml";
        Four_Tuple<PeerID, NetworkConfiguration, PeersPersonalData, PeerRelations> data = PeerClientConfigSerializer.readPeerClientData(config);
        PeerID ownPeerID = data.element1;
        NetworkConfiguration networkConfiguration = data.element2;
        PeersPersonalData peersPersonalData = data.element3;
        PeerRelations peerRelations = data.element4;

        Client client = new Client(ownPeerID, networkConfiguration, peersPersonalData, peerRelations, new GeneralEventsImpl(), new ConnectionEventsImpl(), new ResourceTransferEventsPlus(), new HashMap<String, PeerFSMFactory>());
        ForeignStoreShare foreignStoreShare = new ForeignStoreShare(client.getPeerClient());
        foreignStoreShare.addResourceProvider("file_1", PeerIDGenerator.peerID(2));
        foreignStoreShare.addResourceProvider("file_2", PeerIDGenerator.peerID(2));
        foreignStoreShare.addResourceProvider("file_3", PeerIDGenerator.peerID(2));
        foreignStoreShare.addResourceProvider("file_4", PeerIDGenerator.peerID(2));
        foreignStoreShare.addResourceProvider("file_5", PeerIDGenerator.peerID(2));
        foreignStoreShare.addResourceProvider("file_6", PeerIDGenerator.peerID(2));
        foreignStoreShare.addResourceProvider("file_7", PeerIDGenerator.peerID(2));
//        foreignStoreShare.addResourceProvider("file_1", PeerIDGenerator.peerID(3));
//        foreignStoreShare.addResourceProvider("file_2", PeerIDGenerator.peerID(3));
        foreignStoreShare.addResourceProvider("file_3", PeerIDGenerator.peerID(3));
        foreignStoreShare.addResourceProvider("file_4", PeerIDGenerator.peerID(3));
//        foreignStoreShare.addResourceProvider("file_5", PeerIDGenerator.peerID(3));
//        foreignStoreShare.addResourceProvider("file_6", PeerIDGenerator.peerID(3));
        foreignStoreShare.addResourceProvider("file_7", PeerIDGenerator.peerID(3));
        client.getPeerClient().addForeignResourceStore("files", foreignStoreShare);
        client.startClient();

//        client.getPeerClient().setMaxDesiredDownloadSpeed(750000f);

        ThreadUtil.safeSleep(1000);


        client.getPeerClient().setVisibleDownloadsTimer(3000);
//            client.getPeerClient().downloadResource(new PeerID("pid{0000000000000000000000000000000000000000002}"), "files", "aaa", new BasicFileWriter(".\\aaa_transfer.txt"), true, new DownloadProgressNotificationHandlerImpl(client.getPeerClientData().getOwnPeerID()), 0.1f);
        DownloadManager downloadManager1 = client.getPeerClient().downloadResource("files", "file_1", new BasicFileWriter("./etc/basic_transfer/file.rar"), new DownloadProgressNotificationHandlerImpl(client.getPeerClient().getOwnPeerID()), 0.1f, ResourceStoreImpl.getHash("file_1"), "MD5");
//        client.getPeerClient().downloadResource("files", "file_2", new BasicFileWriter("./etc/basic_transfer/file.rar"), new DownloadProgressNotificationHandlerImpl(client.getPeerClientData().getOwnPeerID()), 0.1f, ResourceStoreImpl.getHash("file_2"), "MD5", 1000000L);

        client.getPeerClient().setMaxDesiredDownloadSpeed(450000f);


        ThreadUtil.safeSleep(15000);


        System.out.println("STOP!!!");
        IOUtil.pauseEnter();
        client.stopClient();

        System.out.println("STATISTICS");
        System.out.println("----------");
        System.out.println(client.getTransferStatistics());

//        downloadManager1.stop();
//        downloadManager2.stop();
//        client.stopClient();


//        ThreadUtil.safeSleep(50000);
//        System.out.println("slow");
//        client.getPeerClient().setMaxDesiredDownloadSpeed(50000f);
//        ThreadUtil.safeSleep(50000);
//        System.out.println("fast");
//        client.getPeerClient().setMaxDesiredDownloadSpeed(1500000f);

//        ThreadUtil.safeSleep(15000);
//        downloadManager.pause();
//        ThreadUtil.safeSleep(120000);
//        System.out.println("GOOOOO");
//        downloadManager.resume();

//        DownloadManager downloadManager2 = client.getPeerClient().downloadResource("files", "bbb", new BasicFileWriter(".\\bbb_transfer.txt"), new DownloadProgressNotificationHandlerImpl(client.getPeerClientData().getOwnPeerID()), 0.1f, "c66d218320ea4b454d0531d09d13792c", "MD5", 1000000L);
//
//        downloadManager2.setPriority(5);
//            client.getPeerClient().setMaxDesiredDownloadSpeed(1500000f);

//            ThreadUtil.safeSleep(300000);
//            downloadManager.pause();
//            ThreadUtil.safeSleep(10000);
//            downloadManager.cancel();

        System.out.println("END");
    }
}
