package jacz.peerengineservice.test;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.client.PeerClientData;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.datatransfer.GlobalDownloadStatistics;
import jacz.peerengineservice.util.datatransfer.GlobalUploadStatistics;
import jacz.peerengineservice.util.datatransfer.PeerStatistics;

import java.io.IOException;
import java.util.Map;

/**
 * Generic test client
 */
public class Client {

    private SimplePeerClientActionImpl peerClientActionImpl;

    private PersonalData personalData;

    private PeerClientData peerClientData;

    private PeerRelations peerRelations;

    private Map<String, PeerFSMFactory> customFSMs;

    private GlobalDownloadStatistics globalDownloadStatistics;

    private GlobalUploadStatistics globalUploadStatistics;

    private PeerStatistics peerStatistics;

    private PeerClient peerClient;

    private TestListContainer testListContainer;

//    private PeerPersonalData ownData;

    public Client(PersonalData personalData, PeerClientData peerClientData, PeerRelations peerRelations, SimplePeerClientActionImpl peerClientActionImpl, Map<String, PeerFSMFactory> customFSMs) throws IOException {
        this(personalData, peerClientData, peerRelations, peerClientActionImpl, customFSMs, null, null);
    }

    public Client(PersonalData personalData, PeerClientData peerClientData, PeerRelations peerRelations, SimplePeerClientActionImpl peerClientActionImpl, Map<String, PeerFSMFactory> customFSMs, Map<String, DataAccessor> readingLists, Map<String, DataAccessor> writingLists) throws IOException {
        this.personalData = personalData;
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
        peerStatistics = new PeerStatistics();
        peerClient = new PeerClient(peerClientData, peerClientActionImpl, globalDownloadStatistics, globalUploadStatistics, peerStatistics, peerRelations, customFSMs, testListContainer);
    }

    public void startClient() throws IOException {
        peerClient.connect();
    }

    public void stopClient() {
        peerClient.stop();
    }

    public PeerClient getPeerClient() {
        return peerClient;
    }

    public void disconnect() {
        peerClient.disconnect();
    }

//    public PeerPersonalData getOwnData() {
//        return ownData;
//    }

    void newFriendConnected(PeerID peerID) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public PeerClientData getPeerClientData() {
        return peerClientData;
    }
}
