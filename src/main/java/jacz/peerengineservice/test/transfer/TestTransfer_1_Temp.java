package jacz.peerengineservice.test.transfer;

import jacz.peerengineservice.client.PeerClientData;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeersPersonalData;
import jacz.peerengineservice.test.*;
import jacz.peerengineservice.util.ForeignStoreShare;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.peerengineservice.util.datatransfer.resource_accession.TempFileWriter;
import jacz.peerengineservice.util.tempfile_api.TempFileManager;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.lists.Triple;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Simple connection, no actions
 */
public class TestTransfer_1_Temp {

    public static void main(String args[]) throws Exception {
        String config = "./src/main/java/jacz/peerengineservice/test/clientConf_1_new.xml";
        Triple<PeersPersonalData, PeerClientData, PeerRelations> data = PeerClientConfigSerializer.readPeerClientData(config);
        PeersPersonalData peersPersonalData = data.element1;
        PeerClientData peerClientData = data.element2;
        PeerRelations peerRelations = data.element3;

        Client client = new Client(peersPersonalData, peerClientData, peerRelations, new SimplePeerClientActionImplTransfer(), new HashMap<String, PeerFSMFactory>());
        ForeignStoreShare foreignStoreShare = new ForeignStoreShare();
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

        DownloadManager downloadManager1 = client.getPeerClient().downloadResource("files", "file_1", tempFileWriter, new DownloadProgressNotificationHandlerImpl(client.getPeerClient().getOwnPeerID()), 0.1f, ResourceStoreImpl.getHash("file_1"), "MD5");
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
////            client.getPeerClient().downloadResource(new PeerID("pid{0000000000000000000000000000000000000000002}"), "files", "aaa", new BasicFileWriter(".\\aaa_transfer.txt"), true, new DownloadProgressNotificationHandlerImpl(client.getPeerClientData().getOwnPeerID()), 0.1f);
//        DownloadManager downloadManager2 = client.getPeerClient().downloadResource("files", "file_2", tempFileWriter2, new DownloadProgressNotificationHandlerImpl(client.getPeerClient().getOwnPeerID()), 0.1f, ResourceStoreImpl.getHash("file_2"), "MD5");

        ThreadUtil.safeSleep(15000);
        System.out.println("STOP!!!");
//        downloadManager1.stop();
//        downloadManager2.stop();
        client.stopClient();
//        ThreadUtil.safeSleep(8000);
//        System.out.println("RESTART!!!");
//        DownloadManager downloadManager2 = client.getPeerClient().downloadResource("files", "aaa", new TempFileWriter(tempFileManager, tempFile, "custom"), new DownloadProgressNotificationHandlerImpl(client.getPeerClientData().getOwnPeerID()), 0.1f, null, null, null);

        System.out.println("END");
    }
}
