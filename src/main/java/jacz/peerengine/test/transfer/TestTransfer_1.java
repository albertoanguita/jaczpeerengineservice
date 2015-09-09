package jacz.peerengine.test.transfer;

import jacz.peerengine.client.PeerClientData;
import jacz.peerengine.client.PeerFSMFactory;
import jacz.peerengine.client.PeerRelations;
import jacz.peerengine.test.*;
import jacz.peerengine.util.ForeignStoreShare;
import jacz.peerengine.util.datatransfer.master.DownloadManager;
import jacz.peerengine.util.datatransfer.resource_accession.BasicFileWriter;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.io.xml.XMLDom;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Simple connection, no actions
 */
public class TestTransfer_1 {

    public static void main(String args[]) {
        String config = ".\\trunk\\src\\com\\jacuzzi\\peerengine\\test\\transfer\\clientConf_1.xml";
        try {
            List<Object> data = PeerClientConfigIO.readPeerClientData(XMLDom.parse(config));
            PersonalData personalData = (PersonalData) data.get(0);
            PeerClientData peerClientData = (PeerClientData) data.get(1);
            PeerRelations peerRelations = (PeerRelations) data.get(2);

            Client client = new Client(personalData, peerClientData, peerRelations, new SimplePeerClientActionImplTransfer(), new HashMap<String, PeerFSMFactory>(), true);
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
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }
}
