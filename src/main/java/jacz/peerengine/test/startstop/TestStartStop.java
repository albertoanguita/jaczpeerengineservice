package jacz.peerengine.test.startstop;

import jacz.peerengine.client.PeerClientData;
import jacz.peerengine.client.PeerFSMFactory;
import jacz.peerengine.client.PeerRelations;
import jacz.peerengine.test.Client;
import jacz.peerengine.test.PeerClientConfigIO;
import jacz.peerengine.test.PersonalData;
import jacz.peerengine.test.SimplePeerClientActionImpl;
import jacz.util.io.xml.XMLDom;

import java.util.HashMap;
import java.util.List;

/**
 * Test with a client that starts, connects, and then stops, to see if it properly closes all its resources
 */
public class TestStartStop {

    public static void main(String args[]) {
        String config = ".\\trunk\\src\\com\\jacuzzi\\peerengine\\test\\clientConf_1.xml";
        try {
            List<Object> data = PeerClientConfigIO.readPeerClientData(XMLDom.parse(config));
            PersonalData personalData = (PersonalData) data.get(0);
            PeerClientData peerClientData = (PeerClientData) data.get(1);
            PeerRelations peerRelations = (PeerRelations) data.get(2);

            Client client = new Client(personalData, peerClientData, peerRelations, new SimplePeerClientActionImpl(), new HashMap<String, PeerFSMFactory>(), true);
            client.startClient();


            Thread.sleep(8000);
//            client.getPeerClient().setListeningPort(11000);
//
//            Thread.sleep(6000);
            System.out.println("Client stopping...");
            client.stopClient();

            System.out.println("Client stopped! END!!!");

//            Thread.sleep(4000);
//            ThreadUtil.printThreadStacks(System.out, false);
            System.out.println("END");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
