package jacz.peerengineservice.test.list_synch;

import jacz.peerengineservice.client.PeerClientData;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeersPersonalData;
import jacz.peerengineservice.test.Client;
import jacz.peerengineservice.test.PeerClientConfigSerializer;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.util.lists.Triple;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Alberto
 * Date: 31/05/14
 * Time: 13:02
 * To change this template use File | Settings | File Templates.
 */
public class TestListSynch_1 {


    public static void main(String args[]) throws Exception {
        String config = "./src/main/java/jacz/peerengineservice/test/clientConf_1_new.xml";
        Triple<PeersPersonalData, PeerClientData, PeerRelations> data = PeerClientConfigSerializer.readPeerClientData(config);
        PeersPersonalData peersPersonalData = data.element1;
        PeerClientData peerClientData = data.element2;
        PeerRelations peerRelations = data.element3;

        Triple<Map<String, DataAccessor>, Map<String, DataAccessor>, DataAccessor> lists = readingWritingListsTest0();

        Client client = new Client(peersPersonalData, peerClientData, peerRelations, new SimplePeerClientActionSynch(lists.element3), new HashMap<String, PeerFSMFactory>(), lists.element1, lists.element2);
        client.startClient();

    }


    private static Triple<Map<String, DataAccessor>, Map<String, DataAccessor>, DataAccessor> readingWritingListsTest0() {
        Set<TestData_0.Movie> movies = new HashSet<>();
        movies.add(new TestData_0.Movie("0", false, 1, "aaa", actors("a1", "a2")));
        movies.add(new TestData_0.Movie("4", false, 5, "eee", actors("e1", "e3")));
        DataAccessor testData_0 = new TestData_0(movies);

        Map<String, DataAccessor> readingLists = new HashMap<>();
        Map<String, DataAccessor> writingLists = new HashMap<>();
        writingLists.put("list0", testData_0);
        return new Triple<>(readingLists, writingLists, testData_0);
    }


    static Set<String> actors(String... actors) {
        Set<String> set = new HashSet<>();
        Collections.addAll(set, actors);
        return set;
    }
}
