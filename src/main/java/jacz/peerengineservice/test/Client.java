package jacz.peerengineservice.test;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.client.PeerClientData;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.premade_lists.old.ForeignPeerDataAction;
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

    private ForeignPeerDataAction foreignPeerDataAction;

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
        /*if (peerID.equals(new PeerID(2))) {
            //System.out.println("Ask files registering...");
            //peerClient.registerCustomFSM(peerID, new AskFilesFSM(), "ProvideFilesFSM");

            System.out.println("Sending new nick to 2...");
            peerClient.getCustomPeerData().setValue(0, "Alb");
            //peerClient.sendChatMessage(peerID, "holaaa");
        }*/
//        Map<String, ListAccessor> readingLists = new HashMap<String, ListAccessor>(0);
//        Map<String, ListAccessor> writingLists = new HashMap<String, ListAccessor>(1);
//        writingLists.put(SimplePersonalDataWithAvatar.getListName(), new PeerPersonalData(peerID, "", PeerPersonalData.State.UNDEFINED, "", foreignPeerDataAction));
//        //writingLists.put("SampleList", createEmptyList());
//        basicListContainer.addPeer(peerID, readingLists, writingLists);
//
//        byte[] peeridbyte = new byte[1];
//        peeridbyte[0] = 11;
//        PeerID peerID11 = new PeerID(peeridbyte);
//        peeridbyte[0] = 12;
//        PeerID peerID12 = new PeerID(peeridbyte);

//        if (peerID.equals(peerID11)) {
//            /*System.out.println("Requesting custom list to 2...");*/
//            peerClient.sendChatMessage(peerID, "hola 1, soy 2...");
//            peerClient.sendObjectMessage(peerID, "mensaje personalizado... TEST!");
//            //ListSynchronizer.getInstance().synchronizeList(peerID, SimplePersonalDataWithAvatar.getListName(), 0, 2, 10000, new ListSynchProgress(peerID, SimplePersonalDataWithAvatar.getListName(), true, basicListContainer));
//        }
//
//        if (peerID.equals(peerID12)) {
//            peerClient.sendChatMessage(peerID, "hola 2, soy 1... te voy a pedir los datos personales");
//            peerClient.getListSynchronizer().synchronizeList(peerID, SimplePersonalDataWithAvatar.getListName(), 0, 3, 10000, new ListSynchProgress(peerID, SimplePersonalDataWithAvatar.getListName(), 0, true));
//            List<Integer> levelList = new ArrayList<Integer>();
//            levelList.add(0);
//            levelList.add(1);
//            //levelList.add(2);
//            levelList.add(4);
//            //ListSynchronizer.getInstance().synchronizeList(peerID, "SampleList", levelList, 60000, new ListSynchProgress(peerID, "SampleList", true, basicListContainer));
//            //ListSynchronizer.getInstance().synchronizeElement(peerID, "SampleList", "0", 0, levelList, 60000, new ListSynchProgress(peerID, "SampleList", true, basicListContainer));
//        }

    }

    public PeerClientData getPeerClientData() {
        return peerClientData;
    }


//    private static SampleList_0 createEmptyList() {
//        return new SampleList_0();
//    }
//
//    private static SampleList_0 createList() {
//        SampleList_2 sl2_0 = new SampleList_2();
//        SampleList_2 sl2_1 = new SampleList_2();
//        SampleList_2 sl2_2 = new SampleList_2();
//        SampleList_2 sl2_3 = new SampleList_2();
//        sl2_0.add(0, 1, 2, 3);
//        sl2_1.add(4, 5, 6, 7);
//        sl2_2.add(8, 9, 10, 11);
//        sl2_3.add(12, 13, 14, 15);
//
//        SampleList_1 sl1_0 = new SampleList_1();
//        SampleList_1 sl1_1 = new SampleList_1();
//        sl1_0.add("0", new SampleList_1.Datum("a", "aa", 30, sl2_0));
//        sl1_0.add("1", new SampleList_1.Datum("b", "bb", 35, sl2_1));
//        sl1_1.add("0", new SampleList_1.Datum("c", "dd", 40, sl2_2));
//        sl1_1.add("1", new SampleList_1.Datum("d", "dd", 45, sl2_3));
//
//        SampleList_0 sampleList_0 = new SampleList_0();
//        sampleList_0.add("0", new SampleList_0.Datum("matrix", null, sl1_0));
//        sampleList_0.add("1", new SampleList_0.Datum("star wars", null, sl1_1));
//
//        return sampleList_0;
//    }
}
