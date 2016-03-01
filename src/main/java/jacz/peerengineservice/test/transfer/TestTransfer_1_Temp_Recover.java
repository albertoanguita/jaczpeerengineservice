package jacz.peerengineservice.test.transfer;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeersPersonalData;
import jacz.peerengineservice.client.connection.NetworkConfiguration;
import jacz.peerengineservice.test.*;
import jacz.peerengineservice.util.ForeignStoreShare;
import jacz.peerengineservice.util.datatransfer.master.MasterResourceStreamer;
import jacz.peerengineservice.util.datatransfer.resource_accession.TempFileWriter;
import jacz.peerengineservice.util.tempfile_api.TempFileManager;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.lists.tuple.Four_Tuple;

import java.util.HashMap;

/**
 * Created by Alberto on 10/10/2015.
 */
public class TestTransfer_1_Temp_Recover {

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

        TempFileManager tempFileManager = new TempFileManager("./etc/temp", new TempFileManagerEventsImpl());


        for (String tempFile : tempFileManager.getExistingTempFiles()) {
            TempFileWriter tempFileWriter = new TempFileWriter(tempFileManager, tempFile);
            System.out.println(tempFile);
            System.out.println("to download recovered file...");
            System.out.println(tempFileWriter.getUserDictionary());
            String storeName = (String) tempFileWriter.getSystemDictionary().get(MasterResourceStreamer.RESOURCE_WRITER_STORE_NAME_FIELD);
            String resourceId = (String) tempFileWriter.getSystemDictionary().get(MasterResourceStreamer.RESOURCE_WRITER_RESOURCE_ID_FIELD);
            String totalHash = (String) tempFileWriter.getSystemDictionary().get(MasterResourceStreamer.RESOURCE_WRITER_TOTAL_HASH_FIELD);
            String hashAlgorithm = (String) tempFileWriter.getSystemDictionary().get(MasterResourceStreamer.RESOURCE_WRITER_HASH_ALGORITHM_FIELD);
//            client.getPeerClient().downloadResource("files", "file_1", tempFileWriter, new DownloadProgressNotificationHandlerImpl(client.getPeerClient().getOwnPeerId()), 0.1f, (String) tempFileWriter.getUserDictionary().get("hash"), "MD5");
            client.getPeerClient().downloadResource(storeName, resourceId, tempFileWriter, new DownloadProgressNotificationHandlerImpl(client.getPeerClient().getOwnPeerId()), 0.1f, totalHash, hashAlgorithm);
        }


        ThreadUtil.safeSleep(20000);
        System.out.println("STOP!!!");

        System.out.println("STATISTICS");
        System.out.println("----------");
        System.out.println(client.getTransferStatistics());

//        downloadManager1.stop();
//        downloadManager2.stop();
        client.stopClient();
//        ThreadUtil.safeSleep(8000);
//        System.out.println("RESTART!!!");
//        DownloadManager downloadManager2 = client.getPeerClient().downloadResource("files", "aaa", new TempFileWriter(tempFileManager, tempFile, "custom"), new DownloadProgressNotificationHandlerImpl(client.getPeerClientData().getOwnPeerId()), 0.1f, null, null, null);

        System.out.println("END");
    }
}
