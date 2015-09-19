package jacz.peerengineservice.test.list_synch;

import jacz.peerengineservice.client.PeerClientData;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.test.Client;
import jacz.peerengineservice.test.PeerClientConfigIO;
import jacz.peerengineservice.test.PersonalData;
import jacz.peerengineservice.test.SimplePeerClientActionImpl;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.util.io.xml.XMLDom;
import jacz.util.lists.Triple;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Alberto
 * Date: 31/05/14
 * Time: 13:02
 * To change this template use File | Settings | File Templates.
 */
public class TestListSynch_2 {

    public static void main(String args[]) throws Exception {
        String config = "./src/main/java/jacz/peerengineservice/test/clientConf_2.xml";
        List<Object> data = PeerClientConfigIO.readPeerClientData(XMLDom.parse(config));
        PersonalData personalData = (PersonalData) data.get(0);
        PeerClientData peerClientData = (PeerClientData) data.get(1);
        PeerRelations peerRelations = (PeerRelations) data.get(2);


        Triple<Map<String, DataAccessor>, Map<String, DataAccessor>, DataAccessor> lists = readingWritingListsTest0();

        Client client = new Client(personalData, peerClientData, peerRelations, new SimplePeerClientActionImpl(), new HashMap<String, PeerFSMFactory>(), lists.element1, lists.element2);
        client.startClient();
    }


    private static Triple<Map<String, DataAccessor>, Map<String, DataAccessor>, DataAccessor> readingWritingListsTest0() {
        Set<TestData_0.Movie> movies = new HashSet<>();
        movies.add(new TestData_0.Movie("0", false, 6, "aaa", TestListSynch_1.actors("a1", "aa2", "aa3")));
        movies.add(new TestData_0.Movie("1", false, 7, "bbb", TestListSynch_1.actors("bb1", "bb2", "bb3")));
        movies.add(new TestData_0.Movie("2", false, 8, "ccc", TestListSynch_1.actors("c1")));
        movies.add(new TestData_0.Movie("3", false, 9, "ddd", TestListSynch_1.actors("d1", "d2")));
        movies.add(new TestData_0.Movie("4", true, 10, null, null));
        DataAccessor testData_0 = new TestData_0(movies);

        Map<String, DataAccessor> readingLists = new HashMap<>();
        Map<String, DataAccessor> writingLists = new HashMap<>();
        readingLists.put("list0", testData_0);
        return new Triple<>(readingLists, writingLists, testData_0);
    }


}
