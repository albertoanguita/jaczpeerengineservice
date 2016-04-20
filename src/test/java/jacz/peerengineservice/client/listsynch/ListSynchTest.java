package jacz.peerengineservice.client.listsynch;

import jacz.peerengineservice.PeerEncryption;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.*;
import jacz.peerengineservice.test.IntegrationTest;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.lists.tuple.SixTuple;
import jacz.util.lists.tuple.Triple;
import org.junit.Assert;
import org.junit.experimental.categories.Category;

import java.util.*;

/**
 * List synch tests
 */
@Category(IntegrationTest.class)
public class ListSynchTest {

    private static final long WARM_UP = 15000;

    @org.junit.Test
    public void listSynchTest1() throws Exception {
        String config = "./etc/tests/clientConf_1_new.xml";
        String userDir = "./etc/tests/client1";
        SixTuple<PeerId, String, String, String, String, String> data = ConfigReader.readPeerClientData(config, userDir);
        PeerId ownPeerId = data.element1;
        String networkConfiguration = data.element2;
        String peersPersonalData = data.element3;
        String peerRelations = data.element4;
        String peerConnectionConfig = data.element5;
        String transferStatistics = data.element6;

        Triple<Map<String, DataAccessor>, Map<String, DataAccessor>, DataAccessor> lists = readingWritingListsTest1();

        PeersEventsSynch peersEventsSynch = new PeersEventsSynch(lists.element3);
        Client client = new Client(ownPeerId, new PeerEncryption(new byte[0]), networkConfiguration, peersPersonalData, peerRelations, peerConnectionConfig, transferStatistics, new GeneralEventsImpl(), new ConnectionEventsImpl(), peersEventsSynch, new ResourceTransferEventsImpl(), new HashMap<>(), lists.element1, lists.element2);
        client.startClient();

        ThreadUtil.safeSleep(WARM_UP);
        Assert.assertTrue(peersEventsSynch.getClientProgress().isSuccess());

        client.stopClient();
    }

    @org.junit.Test
    public void listSynchTest2() throws Exception {
        String config = "./etc/tests/clientConf_2_new.xml";
        String userDir = "./etc/tests/client2";
        SixTuple<PeerId, String, String, String, String, String> data = ConfigReader.readPeerClientData(config, userDir);
        PeerId ownPeerId = data.element1;
        String networkConfiguration = data.element2;
        String peersPersonalData = data.element3;
        String peerRelations = data.element4;
        String peerConnectionConfig = data.element5;
        String transferStatistics = data.element6;

        Triple<Map<String, DataAccessor>, Map<String, DataAccessor>, DataAccessor> lists = readingWritingListsTest2();

        Client client = new Client(ownPeerId, new PeerEncryption(new byte[0]), networkConfiguration, peersPersonalData, peerRelations, peerConnectionConfig, transferStatistics, new GeneralEventsImpl(), new ConnectionEventsImpl(), new PeersEventsImpl(), new ResourceTransferEventsImpl(), new HashMap<>(), lists.element1, lists.element2);
        client.startClient();

        ThreadUtil.safeSleep(WARM_UP);
        TestData_0 testData_0 = (TestData_0) lists.element3;
        Assert.assertTrue(testData_0.getServerProgress().isSuccess());

        client.stopClient();
    }


    private static Triple<Map<String, DataAccessor>, Map<String, DataAccessor>, DataAccessor> readingWritingListsTest1() {
        Set<TestData_0.Movie> movies = new HashSet<>();
        movies.add(new TestData_0.Movie("0", false, 1L, "aaa", actors("a1", "a2")));
        movies.add(new TestData_0.Movie("4", false, 5L, "eee", actors("e1", "e3")));
        DataAccessor testData_0 = new TestData_0("1", movies);

        Map<String, DataAccessor> readingLists = new HashMap<>();
        Map<String, DataAccessor> writingLists = new HashMap<>();
        writingLists.put("list0", testData_0);
        return new Triple<>(readingLists, writingLists, testData_0);
    }

    private static Triple<Map<String, DataAccessor>, Map<String, DataAccessor>, DataAccessor> readingWritingListsTest2() {
        Set<TestData_0.Movie> movies = new HashSet<>();
        movies.add(new TestData_0.Movie("0", false, 6L, "aaa", actors("a1", "aa2", "aa3")));
        movies.add(new TestData_0.Movie("1", false, 7L, "bbb", actors("bb1", "bb2", "bb3")));
        movies.add(new TestData_0.Movie("2", false, 8L, "ccc", actors("c1")));
        movies.add(new TestData_0.Movie("3", false, 9L, "ddd", actors("d1", "d2")));
        movies.add(new TestData_0.Movie("4", true, 10L, null, null));
        DataAccessor testData_0 = new TestData_0("2", movies);

        Map<String, DataAccessor> readingLists = new HashMap<>();
        Map<String, DataAccessor> writingLists = new HashMap<>();
        readingLists.put("list0", testData_0);
        return new Triple<>(readingLists, writingLists, testData_0);
    }



    private static Set<String> actors(String... actors) {
        Set<String> set = new HashSet<>();
        Collections.addAll(set, actors);
        return set;
    }
}
