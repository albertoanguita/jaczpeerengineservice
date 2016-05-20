package jacz.peerengineservice.client.transfer;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.*;
import jacz.peerengineservice.test.IntegrationTest;
import jacz.peerengineservice.util.ForeignStoreShare;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.peerengineservice.util.datatransfer.master.DownloadState;
import jacz.peerengineservice.util.datatransfer.resource_accession.BasicFileWriter;
import jacz.peerengineservice.util.datatransfer.resource_accession.TempFileWriter;
import jacz.peerengineservice.util.tempfile_api.TempFileManager;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.files.FileReaderWriter;
import jacz.util.lists.tuple.SixTuple;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

/**
 * Transfer tests
 */
@Category(IntegrationTest.class)
public class TransferTest {

    private static final long WARM_UP = 20000;

    private static final long CYCLE_LENGTH = 10000;

    private static final long DOWNLOAD_LENGTH = 65000;

    private static final String filesPath = "./etc/files";
    private static final String tempPath = "./etc/files/temp";
    private static final String down1Path = "./etc/files/down1";
    private static final String down1File = "./etc/files/down1/file.dat";
    private static final String path = "./etc/files/file.dat";
    private static final String hash = getFileHash();


    @org.junit.Test
    public void transfer1() throws Exception {
        String config = "./etc/tests/clientConf_1_new.xml";
        String userDir = "./etc/tests/client1";
        SixTuple<PeerId, String, String, String, String, String> data = ConfigReader.readPeerClientData(config, userDir);
        PeerId ownPeerId = data.element1;
        String networkConfiguration = data.element2;
        String peersPersonalData = data.element3;
        String peerRelations = data.element4;
        String peerConnectionConfig = data.element5;
        String transferStatistics = data.element6;

        Client client = new Client(ownPeerId, networkConfiguration, peersPersonalData, peerRelations, peerConnectionConfig, transferStatistics, new GeneralEventsImpl(), new ConnectionEventsImpl(), new PeersEventsImpl(), new HashMap<>());
        ForeignStoreShare foreignStoreShare = new ForeignStoreShare(client.getPeerClient());
        foreignStoreShare.addResourceProvider(hash, PeerIdGenerator.peerID(2));
        foreignStoreShare.addResourceProvider(hash, PeerIdGenerator.peerID(3));
        client.getPeerClient().addForeignResourceStore("files", foreignStoreShare);
        client.startClient();

        ThreadUtil.safeSleep(WARM_UP);
        Assert.assertFalse(new File(down1File).isFile());

        System.out.println("DOWNLOADING FILE...");
        HashMap<String, Serializable> customInfo = new HashMap<>();
        customInfo.put("one", 1);
        customInfo.put("two", "hello");

        client.getPeerClient().setVisibleDownloadsTimer(1000);
        DownloadManager downloadManager = client.getPeerClient().downloadResource("files", hash, new BasicFileWriter(down1File, customInfo), new DownloadProgressNotificationHandlerImpl(), 0.1f, hash, "MD5");
        client.getPeerClient().setMaxDownloadSpeed(250000f);

        ThreadUtil.safeSleep(DOWNLOAD_LENGTH);
        Assert.assertTrue(new File(down1File).isFile());
        Assert.assertEquals(DownloadState.COMPLETED, downloadManager.getState());
        Assert.assertEquals(1, downloadManager.getResourceWriter().getUserDictionary().get("one"));
        Assert.assertEquals("hello", downloadManager.getResourceWriter().getUserDictionary().get("two"));
        System.out.println("FIRST DOWNLOAD CORRECTLY CHECKED!");

        FileUtils.forceDelete(new File(down1File));

        System.out.println("START TEMPORARY FILE DOWNLOAD...");
        TempFileManager tempFileManager = client.getTempFileManager(tempPath);
        TempFileWriter tempFileWriter = new TempFileWriter(tempFileManager);
        downloadManager = client.getPeerClient().downloadResource("files", hash, tempFileWriter, new DownloadProgressNotificationHandlerImpl(), 0.1f, hash, "MD5");

        ThreadUtil.safeSleep(CYCLE_LENGTH);
        System.out.println("STOPPING DOWNLOAD...");
        downloadManager.stop();

        Assert.assertEquals(DownloadState.STOPPED, downloadManager.getState());
        Assert.assertTrue(downloadManager.getStatistics().getDownloadedSizeThisResource() > 0L);

        ThreadUtil.safeSleep(CYCLE_LENGTH);
        Assert.assertEquals(1, tempFileManager.getExistingTempFiles().size());
        tempFileWriter = new TempFileWriter(tempFileManager, tempFileManager.getExistingTempFiles().iterator().next());

        // todo test
        // delete temp files
        ThreadUtil.safeSleep(500);
        FileUtils.cleanDirectory(new java.io.File(tempPath));
        ThreadUtil.safeSleep(500);

        downloadManager = client.getPeerClient().downloadResource("files", hash, tempFileWriter, new DownloadProgressNotificationHandlerImpl(), 0.1f, hash, "MD5");
        Assert.assertTrue(downloadManager.getStatistics().getDownloadedSizeThisResource() > 0L);

        ThreadUtil.safeSleep(DOWNLOAD_LENGTH);
        System.out.println("SECOND DOWNLOAD CORRECTLY CHECKED!");

        Assert.assertEquals(DownloadState.COMPLETED, downloadManager.getState());

        FileUtils.cleanDirectory(new File(filesPath));

        client.stopClient();
    }

    @org.junit.Test
    public void transfer2() throws Exception {
        String config = "./etc/tests/clientConf_2_new.xml";
        String userDir = "./etc/tests/client2";
        SixTuple<PeerId, String, String, String, String, String> data = ConfigReader.readPeerClientData(config, userDir);
        PeerId ownPeerId = data.element1;
        String networkConfiguration = data.element2;
        String peersPersonalData = data.element3;
        String peerRelations = data.element4;
        String peerConnectionConfig = data.element5;
        String transferStatistics = data.element6;

        FileUtils.cleanDirectory(new File(filesPath));
        FileUtils.forceMkdir(new File(tempPath));
        FileUtils.forceMkdir(new File(down1Path));
        buildFile(path);

        Client client = new Client(ownPeerId, networkConfiguration, peersPersonalData, peerRelations, peerConnectionConfig, transferStatistics, new GeneralEventsImpl(), new ConnectionEventsImpl(), new PeersEventsImpl(), new HashMap<>());
        client.getPeerClient().addLocalResourceStore("files", new ResourceStoreImpl(hash, path));
        client.startClient();


        client.getPeerClient().setVisibleUploadsTimer(1000);
        ThreadUtil.safeSleep(WARM_UP);
        ThreadUtil.safeSleep(2 * DOWNLOAD_LENGTH);
        ThreadUtil.safeSleep(2 * CYCLE_LENGTH);

        client.stopClient();
    }

    @org.junit.Test
    public void transfer3() throws Exception {
        String config = "./etc/tests/clientConf_3_new.xml";
        String userDir = "./etc/tests/client3";
        SixTuple<PeerId, String, String, String, String, String> data = ConfigReader.readPeerClientData(config, userDir);
        PeerId ownPeerId = data.element1;
        String networkConfiguration = data.element2;
        String peersPersonalData = data.element3;
        String peerRelations = data.element4;
        String peerConnectionConfig = data.element5;
        String transferStatistics = data.element6;

        Client client = new Client(ownPeerId, networkConfiguration, peersPersonalData, peerRelations, peerConnectionConfig, transferStatistics, new GeneralEventsImpl(), new ConnectionEventsImpl(), new PeersEventsImpl(), new HashMap<>());
        client.getPeerClient().addLocalResourceStore("files", new ResourceStoreImpl(hash, path));
        client.startClient();

        client.getPeerClient().setVisibleUploadsTimer(1000);
        ThreadUtil.safeSleep(WARM_UP);
        ThreadUtil.safeSleep(2 * DOWNLOAD_LENGTH);
        ThreadUtil.safeSleep(2 * CYCLE_LENGTH);

        client.stopClient();
    }

    private static String buildFile(String path) throws IOException {
        // build a 1MB size file at the given path, return its hash
        byte[] data = new byte[10000000];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        FileReaderWriter.writeBytes(path, data);
        return getFileHash();
    }

    private static String getFileHash() {
        return "5363a72ab6c10777d8d62ff07b44592a";
    }
}
