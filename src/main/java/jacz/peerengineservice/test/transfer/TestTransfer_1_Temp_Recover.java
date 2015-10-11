package jacz.peerengineservice.test.transfer;

import jacz.peerengineservice.client.PeerClientData;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeersPersonalData;
import jacz.peerengineservice.test.Client;
import jacz.peerengineservice.test.DownloadProgressNotificationHandlerImpl;
import jacz.peerengineservice.test.PeerClientConfigSerializer;
import jacz.peerengineservice.test.PeerIDGenerator;
import jacz.peerengineservice.util.ForeignStoreShare;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.peerengineservice.util.datatransfer.resource_accession.TempFileWriter;
import jacz.peerengineservice.util.tempfile_api.TempFileManager;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.lists.Triple;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by Alberto on 10/10/2015.
 */
public class TestTransfer_1_Temp_Recover {

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

        TempFileManager tempFileManager = new TempFileManager("./etc/temp");


        for (String tempFile : tempFileManager.getExistingTempFiles()) {
            TempFileWriter tempFileWriter = new TempFileWriter(tempFileManager, tempFile);
            System.out.println(tempFile);
            System.out.println("to download recovered file...");
            System.out.println(tempFileWriter.getUserDictionary());
            client.getPeerClient().downloadResource("files", "file_1", tempFileWriter, new DownloadProgressNotificationHandlerImpl(client.getPeerClient().getOwnPeerID()), 0.1f, (String) tempFileWriter.getUserDictionary().get("hash"), "MD5");
        }


//        ThreadUtil.safeSleep(15000);
//        System.out.println("STOP!!!");
//        downloadManager1.stop();
//        downloadManager2.stop();
//        client.getPeerClient().stop();
//        ThreadUtil.safeSleep(8000);
//        System.out.println("RESTART!!!");
//        DownloadManager downloadManager2 = client.getPeerClient().downloadResource("files", "aaa", new TempFileWriter(tempFileManager, tempFile, "custom"), new DownloadProgressNotificationHandlerImpl(client.getPeerClientData().getOwnPeerID()), 0.1f, null, null, null);

        System.out.println("END");
    }
}