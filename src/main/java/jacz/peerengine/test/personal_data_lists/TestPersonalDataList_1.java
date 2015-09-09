package jacz.peerengine.test.personal_data_lists;

import jacz.peerengine.PeerID;
import jacz.peerengine.client.PeerClientData;
import jacz.peerengine.client.PeerFSMFactory;
import jacz.peerengine.client.PeerRelations;
import jacz.peerengine.test.Client;
import jacz.peerengine.test.PeerClientConfigIO;
import jacz.peerengine.test.PersonalData;
import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.io.xml.XMLDom;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Simple connection, no actions
 */
public class TestPersonalDataList_1 /*implements ParallelTask*/ {

//    public static void main(String args[]) {
//    }
//
//    @Override
//    public void performTask() {
//        String config = ".\\trunk\\src\\com.jacuzzi.peerengine\\test\\personal_data_lists\\clientConf_1.xml";
//        try {
//            List<Object> data = PeerClientConfigIO.readPeerClientData(XMLDom.parse(config));
//            PersonalData personalData = (PersonalData) data.get(0);
//            PeerClientData peerClientData = (PeerClientData) data.get(1);
//            PeerRelations peerRelations = (PeerRelations) data.get(2);
//
//            SimplePeerClientActionImplCustom simplePeerClientActionImplCustom = new SimplePeerClientActionImplCustom(new ForeignPeerDataActionImpl());
//            Client client = new Client(personalData, peerClientData, peerRelations, simplePeerClientActionImplCustom, new HashMap<String, PeerFSMFactory>(), false);
//            simplePeerClientActionImplCustom.setBasicListContainer(client.getBasicListContainer());
//            simplePeerClientActionImplCustom.setListSynchronizer(client.getPeerClient().getListSynchronizer());
//
//            client.startClient();
//
//
//            try {
//                Thread.sleep(15000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            System.out.println("1 añade a 2 como amigo");
//            client.getPeerClient().addFriendPeer(new PeerID("pid{0000000000000000000000000000000000000000002}"));
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (XMLStreamException e) {
//            e.printStackTrace();
//        }
//    }
}
