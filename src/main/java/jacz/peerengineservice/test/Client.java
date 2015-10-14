package jacz.peerengineservice.test;

import jacz.peerengineservice.client.*;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.datatransfer.GlobalDownloadStatistics;
import jacz.peerengineservice.util.datatransfer.GlobalUploadStatistics;
import jacz.peerengineservice.util.datatransfer.PeerBasedStatistics;
import jacz.peerengineservice.util.tempfile_api.TempFileManager;

import java.io.IOException;
import java.util.Map;

/**
 * Generic test client
 */
public class Client {

    private SimplePeerClientActionImpl peerClientActionImpl;

    private PeerClientData peerClientData;

    private PeerRelations peerRelations;

    private Map<String, PeerFSMFactory> customFSMs;

    private GlobalDownloadStatistics globalDownloadStatistics;

    private GlobalUploadStatistics globalUploadStatistics;

    private PeerBasedStatistics peerBasedStatistics;

    private PeerClient peerClient;

    private TestListContainer testListContainer;

    TempFileManager tempFileManager;


    public Client(
            PeersPersonalData peersPersonalData,
            PeerClientData peerClientData,
            PeerRelations peerRelations,
            SimplePeerClientActionImpl peerClientActionImpl,
            Map<String, PeerFSMFactory> customFSMs) throws IOException {
        this(peersPersonalData, peerClientData, peerRelations, peerClientActionImpl, customFSMs, null, null);
    }

    public Client(
            PeersPersonalData peersPersonalData,
            PeerClientData peerClientData,
            PeerRelations peerRelations,
            SimplePeerClientActionImpl peerClientActionImpl,
            Map<String, PeerFSMFactory> customFSMs,
            Map<String, DataAccessor> readingLists,
            Map<String, DataAccessor> writingLists) throws IOException {
        this.peerClientData = peerClientData;
        this.peerRelations = peerRelations;
        this.peerClientActionImpl = peerClientActionImpl;
        this.customFSMs = customFSMs;
        peerClientActionImpl.init(peerClientData.getOwnPeerID(), this);

//        ownData = new PeerPersonalData(peerClientData.getOwnPeerID(), "", PeerPersonalData.State.UNDEFINED, "", peerClientActionImpl);
//        basicReadingLists.put(PeerPersonalData.getListName(), ownData);
        testListContainer = new TestListContainer(readingLists, writingLists);
//        if (FileUtil.isFile("globalDownloads.txt")) {
//            VersionedObjectSerializer.deserializeVersionedObject(globalDownloadStatistics);
//            globalDownloadStatistics = new GlobalDownloadStatistics();
//        } else {
//            globalDownloadStatistics = new GlobalDownloadStatistics();
//        }
        globalDownloadStatistics = new GlobalDownloadStatistics();
        globalUploadStatistics = new GlobalUploadStatistics();
        peerBasedStatistics = new PeerBasedStatistics();
        peerClient = new PeerClient(peerClientData, peerClientActionImpl, new ResourceTransferEventsImpl(), peersPersonalData, globalDownloadStatistics, globalUploadStatistics, peerBasedStatistics, peerRelations, customFSMs, new DataSynchEventsImpl(), testListContainer);

        tempFileManager = new TempFileManager("./etc/temp", new TempFileManagerEventsImpl());
    }

    public void startClient() throws IOException {
        peerClient.connect();
    }

    public void stopClient() {
        peerClient.stop();
        tempFileManager.stop();
    }

    public PeerClient getPeerClient() {
        return peerClient;
    }

    public void disconnect() {
        peerClient.disconnect();
    }

    public TempFileManager getTempFileManager() {
        return tempFileManager;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("END!!!");
    }
}
