package jacz.peerengine.test.list_synch;

import jacz.peerengine.client.PeerClientData;
import jacz.peerengine.client.PeerFSMFactory;
import jacz.peerengine.client.PeerRelations;
import jacz.peerengine.test.Client;
import jacz.peerengine.test.PeerClientConfigIO;
import jacz.peerengine.test.PersonalData;
import jacz.peerengine.util.data_synchronization.old.ListAccessor;
import jacz.peerengine.util.data_synchronization.old.NonIndexedListAccessor;
import jacz.peerengine.util.data_synchronization.old.NonIndexedListAccessorBridge;
import jacz.peerengine.util.data_synchronization.premade_lists.old.TestList_0;
import jacz.peerengine.util.data_synchronization.premade_lists.old.TestList_1;
import jacz.peerengine.util.data_synchronization.premade_lists.old.TestList_2;
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
public class TestListSynch_1 {


    public static void main(String args[]) {
        String config = ".\\trunk\\src\\com\\jacuzzi\\peerengine\\test\\clientConf_1.xml";
        try {
            List<Object> data = PeerClientConfigIO.readPeerClientData(XMLDom.parse(config));
            PersonalData personalData = (PersonalData) data.get(0);
            PeerClientData peerClientData = (PeerClientData) data.get(1);
            PeerRelations peerRelations = (PeerRelations) data.get(2);

            Triple<Map<String, ListAccessor>, Map<String, ListAccessor>, ListAccessor> lists = readingWritingListsTest0();

            Client client = new Client(personalData, peerClientData, peerRelations, new SimplePeerClientActionSynch(lists.element3), new HashMap<String, PeerFSMFactory>(), true, lists.element1, lists.element2);
            client.startClient();



        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static Triple<Map<String, ListAccessor>, Map<String, ListAccessor>, ListAccessor> readingWritingListsTest0() {
        Map<String, TestList_0.Movie> movies = new HashMap<>();
        movies.put("a", new TestList_0.Movie("aaa", actors("a1", "a2")));
        movies.put("e", new TestList_0.Movie("eee", actors("e1", "e3")));
        ListAccessor testList_0 = new TestList_0(movies);

        Map<String, ListAccessor> readingLists = new HashMap<>();
        Map<String, ListAccessor> writingLists = new HashMap<>();
        writingLists.put("list0", testList_0);
        return new Triple<>(readingLists, writingLists, testList_0);
    }

    private static Triple<Map<String, ListAccessor>, Map<String, ListAccessor>, ListAccessor> readingWritingListsTest1() {
        Set<String> values = new HashSet<>();
        values.add("aaa");
        values.add("eee");
        ListAccessor testList_1 = new TestList_1(values);

        Map<String, ListAccessor> readingLists = new HashMap<>();
        Map<String, ListAccessor> writingLists = new HashMap<>();
        writingLists.put("list0", testList_1);
        return new Triple<>(readingLists, writingLists, testList_1);
    }

    private static Triple<Map<String, ListAccessor>, Map<String, ListAccessor>, ListAccessor> readingWritingListsTest2() {
        List<String> values = new ArrayList<>();
        values.add("aaa");
        values.add("aaa");
        values.add("aaa");
        values.add("bbb");
        values.add("eee");
        values.add("eee");
        NonIndexedListAccessor testList_2 = new TestList_2(values);
        ListAccessor listAccessor = new NonIndexedListAccessorBridge(testList_2);

        Map<String, ListAccessor> readingLists = new HashMap<>();
        Map<String, ListAccessor> writingLists = new HashMap<>();
        writingLists.put("list0", listAccessor);
        return new Triple<>(readingLists, writingLists, listAccessor);
    }

    static Set<String> actors(String... actors) {
        Set<String> set = new HashSet<>();
        Collections.addAll(set, actors);
        return set;
    }
}
