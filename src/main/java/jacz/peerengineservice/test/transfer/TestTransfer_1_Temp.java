package jacz.peerengineservice.test.transfer;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeersPersonalData;
import jacz.peerengineservice.client.connection.NetworkConfiguration;
import jacz.peerengineservice.test.*;
import jacz.peerengineservice.util.ForeignStoreShare;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.peerengineservice.util.datatransfer.resource_accession.TempFileWriter;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.lists.tuple.Four_Tuple;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Simple connection, no actions
 */
public class TestTransfer_1_Temp {

    public static void main(String args[]) throws Exception {
        String config = "./etc/tests/clientConf_1_new.xml";
        Four_Tuple<PeerId, NetworkConfiguration, PeersPersonalData, PeerRelations> data = PeerClientConfigSerializer.readPeerClientData(config);
        PeerId ownPeerId = data.element1;
        NetworkConfiguration networkConfiguration = data.element2;
        PeersPersonalData peersPersonalData = data.element3;
        PeerRelations peerRelations = data.element4;

        Client client = new Client(ownPeerId, networkConfiguration, peersPersonalData, peerRelations, new GeneralEventsImpl(), new ConnectionEventsImpl(), new ResourceTransferEventsPlus(), new HashMap<String, PeerFSMFactory>());
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
//        foreignStoreShare.addResourceProvider("file_3", PeerIDGenerator.peerID(3));
//        foreignStoreShare.addResourceProvider("file_4", PeerIDGenerator.peerID(3));
//        foreignStoreShare.addResourceProvider("file_5", PeerIDGenerator.peerID(3));
//        foreignStoreShare.addResourceProvider("file_6", PeerIDGenerator.peerID(3));
//        foreignStoreShare.addResourceProvider("file_7", PeerIDGenerator.peerID(3));
        client.getPeerClient().addForeignResourceStore("files", foreignStoreShare);
        client.startClient();

        ThreadUtil.safeSleep(1000);
        client.getPeerClient().setVisibleDownloadsTimer(3000);

        HashMap<String, Serializable> customDictionary = new HashMap<>();
        customDictionary.put("hash", ResourceStoreImpl.getHash("file_1"));
        TempFileWriter tempFileWriter = new TempFileWriter(client.getTempFileManager(), customDictionary);
        String tempFile = tempFileWriter.getTempFile();
        System.out.println(tempFile);

        System.out.println("to download first file...");

        DownloadManager downloadManager1 = client.getPeerClient().downloadResource("files", "file_1", tempFileWriter, new DownloadProgressNotificationHandlerImpl(client.getPeerClient().getOwnPeerId()), 0.1f, ResourceStoreImpl.getHash("file_1"), "MD5");
        ThreadUtil.safeSleep(10000);

//        System.out.println("to download second file...");
//
//        HashMap<String, Serializable> customDictionary2 = new HashMap<>();
//        customDictionary2.put("hash", "file_2");
//        TempFileWriter tempFileWriter2 = new TempFileWriter(tempFileManager, customDictionary2);
//        String tempFile2 = tempFileWriter2.getTempFile();
//        System.out.println(tempFile2);
//
//
////            client.getPeerClient().downloadResource(new PeerId("pid{0000000000000000000000000000000000000000002}"), "files", "aaa", new BasicFileWriter(".\\aaa_transfer.txt"), true, new DownloadProgressNotificationHandlerImpl(client.getPeerClientData().getOwnPeerId()), 0.1f);
//        DownloadManager downloadManager2 = client.getPeerClient().downloadResource("files", "file_2", tempFileWriter2, new DownloadProgressNotificationHandlerImpl(client.getPeerClient().getOwnPeerId()), 0.1f, ResourceStoreImpl.getHash("file_2"), "MD5");

        ThreadUtil.safeSleep(15000);
        System.out.println("STOP!!!");
//        downloadManager1.stop();
//        downloadManager2.stop();
        client.stopClient();

        System.out.println("STATISTICS");
        System.out.println("----------");
        System.out.println(client.getTransferStatistics());

//        ThreadUtil.safeSleep(8000);
//        System.out.println("RESTART!!!");
//        DownloadManager downloadManager2 = client.getPeerClient().downloadResource("files", "aaa", new TempFileWriter(tempFileManager, tempFile, "custom"), new DownloadProgressNotificationHandlerImpl(client.getPeerClientData().getOwnPeerId()), 0.1f, null, null, null);

        System.out.println("END");
    }
}
