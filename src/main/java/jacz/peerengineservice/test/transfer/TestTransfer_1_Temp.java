package jacz.peerengineservice.test.transfer;

import jacz.peerengineservice.client.PeerClientData;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.test.*;
import jacz.peerengineservice.util.ForeignStoreShare;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.peerengineservice.util.datatransfer.resource_accession.BasicFileWriter;
import jacz.peerengineservice.util.datatransfer.resource_accession.TempFileWriter;
import jacz.peerengineservice.util.tempfile_api.TempFileManager;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.io.xml.XMLDom;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Simple connection, no actions
 */
public class TestTransfer_1_Temp {

    public static void main(String args[]) {
        String config = ".\\trunk\\src\\com\\jacuzzi\\peerengineservice\\test\\transfer\\clientConf_1.xml";
        try {
            List<Object> data = PeerClientConfigIO.readPeerClientData(XMLDom.parse(config));
            PersonalData personalData = (PersonalData) data.get(0);
            PeerClientData peerClientData = (PeerClientData) data.get(1);
            PeerRelations peerRelations = (PeerRelations) data.get(2);

            Client client = new Client(personalData, peerClientData, peerRelations, new SimplePeerClientActionImplTransfer(), new HashMap<String, PeerFSMFactory>());
            ForeignStoreShare foreignStoreShare = new ForeignStoreShare();
            foreignStoreShare.addResourceProvider("aaa", PeerIDGenerator.peerID(2));
            foreignStoreShare.addResourceProvider("bbb", PeerIDGenerator.peerID(2));
//            foreignStoreShare.addResourceProvider("aaa", PeerIDGenerator.peerID(3));
            client.getPeerClient().addForeignResourceStore("files", foreignStoreShare);
            client.startClient();

            ThreadUtil.safeSleep(1000);

            TempFileManager tempFileManager = new TempFileManager(".");
            TempFileWriter tempFileWriter = new TempFileWriter(tempFileManager);
            String tempFile = tempFileWriter.getTempFile();
            System.out.println(tempFile);


            client.getPeerClient().setVisibleDownloadsTimer(5000);

            client.getPeerClient().downloadResource("files", "bbb", new BasicFileWriter(".\\bbb_transfer.txt"), new DownloadProgressNotificationHandlerImpl(client.getPeerClientData().getOwnPeerID()), 0.1f, null, null, null);
            ThreadUtil.safeSleep(60000);

            System.out.println("to download second file...");

//            client.getPeerClient().downloadResource(new PeerID("pid{0000000000000000000000000000000000000000002}"), "files", "aaa", new BasicFileWriter(".\\aaa_transfer.txt"), true, new DownloadProgressNotificationHandlerImpl(client.getPeerClientData().getOwnPeerID()), 0.1f);
            DownloadManager downloadManager = client.getPeerClient().downloadResource("files", "aaa", tempFileWriter, new DownloadProgressNotificationHandlerImpl(client.getPeerClientData().getOwnPeerID()), 0.1f, null, null, null);

            ThreadUtil.safeSleep(45000);
            System.out.println("STOP!!!");
            downloadManager.stop();
            ThreadUtil.safeSleep(8000);
            System.out.println("RESTART!!!");
            DownloadManager downloadManager2 = client.getPeerClient().downloadResource("files", "aaa", new TempFileWriter(tempFileManager, tempFile), new DownloadProgressNotificationHandlerImpl(client.getPeerClientData().getOwnPeerID()), 0.1f, null, null, null);

            System.out.println("GO!");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }
}
