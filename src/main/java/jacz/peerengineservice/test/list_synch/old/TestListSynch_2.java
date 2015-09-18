package jacz.peerengineservice.test.list_synch.old;

import jacz.peerengineservice.client.PeerClientData;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.test.Client;
import jacz.peerengineservice.test.PeerClientConfigIO;
import jacz.peerengineservice.test.PersonalData;
import jacz.peerengineservice.test.SimplePeerClientActionImpl;
import jacz.peerengineservice.util.data_synchronization.old.ListAccessor;
import jacz.peerengineservice.util.data_synchronization.old.NonIndexedListAccessor;
import jacz.peerengineservice.util.data_synchronization.old.NonIndexedListAccessorBridge;
import jacz.peerengineservice.util.data_synchronization.premade_lists.old.TestList_0;
import jacz.peerengineservice.util.data_synchronization.premade_lists.old.TestList_1;
import jacz.peerengineservice.util.data_synchronization.premade_lists.old.TestList_2;
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

    public static void main(String args[]) {
        String config = ".\\trunk\\src\\com\\jacuzzi\\peerengineservice\\test\\clientConf_2.xml";
        try {
            List<Object> data = PeerClientConfigIO.readPeerClientData(XMLDom.parse(config));
            PersonalData personalData = (PersonalData) data.get(0);
            PeerClientData peerClientData = (PeerClientData) data.get(1);
            PeerRelations peerRelations = (PeerRelations) data.get(2);


            Triple<Map<String, ListAccessor>, Map<String, ListAccessor>, ListAccessor> lists = readingWritingListsTest0();

//            Client client = new Client(personalData, peerClientData, peerRelations, new SimplePeerClientActionImpl(), new HashMap<String, PeerFSMFactory>(), lists.element1, lists.element2);
//            client.startClient();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static Triple<Map<String, ListAccessor>, Map<String, ListAccessor>, ListAccessor> readingWritingListsTest0() {
        Map<String, TestList_0.Movie> movies = new HashMap<>();
        movies.put("a", new TestList_0.Movie("aaa", TestListSynch_1.actors("a1", "aa2", "aa3")));
        movies.put("b", new TestList_0.Movie("bbb", TestListSynch_1.actors("bb1", "bb2", "bb3")));
        movies.put("c", new TestList_0.Movie("ccc", TestListSynch_1.actors("c1")));
        movies.put("d", new TestList_0.Movie("ddd", TestListSynch_1.actors("d1", "d2")));
        ListAccessor testList_0 = new TestList_0(movies);

        Map<String, ListAccessor> readingLists = new HashMap<>();
        Map<String, ListAccessor> writingLists = new HashMap<>();
        readingLists.put("list0", testList_0);
        return new Triple<>(readingLists, writingLists, testList_0);
    }

    private static Triple<Map<String, ListAccessor>, Map<String, ListAccessor>, ListAccessor> readingWritingListsTest1() {
        Set<String> values = new HashSet<>();
        values.add("aaa");
        values.add("bbb");
        values.add("ccc");
        values.add("ddd");
        ListAccessor testList_1 = new TestList_1(values);

        Map<String, ListAccessor> readingLists = new HashMap<>();
        Map<String, ListAccessor> writingLists = new HashMap<>();
        readingLists.put("list0", testList_1);
        return new Triple<>(readingLists, writingLists, testList_1);
    }

    private static Triple<Map<String, ListAccessor>, Map<String, ListAccessor>, ListAccessor> readingWritingListsTest2() {
        List<String> values = new ArrayList<>();
        values.add("aaa");
        values.add("bbb");
        values.add("bbb");
        values.add("ccc");
        values.add("ddd");
        values.add("ddd");
        values.add("ddd");
        NonIndexedListAccessor testList_2 = new TestList_2(values);
        ListAccessor listAccessor = new NonIndexedListAccessorBridge(testList_2);

        Map<String, ListAccessor> readingLists = new HashMap<>();
        Map<String, ListAccessor> writingLists = new HashMap<>();
        readingLists.put("list0", listAccessor);
        return new Triple<>(readingLists, writingLists, listAccessor);
    }

}
