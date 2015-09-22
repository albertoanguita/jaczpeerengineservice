package jacz.peerengineservice.test;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClientData;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeerServerData;
import jacz.util.io.object_serialization.StrCast;
import jacz.util.io.object_serialization.XMLReader;
import jacz.util.io.xml.Element;
import jacz.util.lists.Triple;
import jacz.util.network.IP4Port;

import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;

/**
 * Created by Alberto on 20/09/2015.
 */
public class PeerClientConfigSerializer {

    public static Triple<PersonalData, PeerClientData, PeerRelations> readPeerClientData(String path) throws FileNotFoundException, XMLStreamException, IllegalArgumentException, NumberFormatException {
        XMLReader xmlReader = new XMLReader(path);

        PersonalData personalData = new PersonalData(xmlReader.getFieldValue("nick"), xmlReader.getFieldValue("avatar"));

        xmlReader.getStruct("peer-server-data");
        String ip = xmlReader.getFieldValue("ip");
        int port = StrCast.asInteger(xmlReader.getFieldValue("port"));
        PeerServerData peerServerData = new PeerServerData(new IP4Port(ip, port));
        xmlReader.gotoParent();
        PeerClientData peerClientData = new PeerClientData(new PeerID(xmlReader.getFieldValue("peer-id")), StrCast.asInteger(xmlReader.getFieldValue("port")), peerServerData);

        PeerRelations peerRelations = new PeerRelations();
        xmlReader.getStruct("friend-peers");
        while (xmlReader.hasMoreChildren()) {
            xmlReader.getNextStruct();
            PeerID peerID = new PeerID(xmlReader.getFieldValue("peer-id"));
            String nick = xmlReader.getFieldValue("nick");
            peerRelations.addFriendPeer(peerID);
            xmlReader.gotoParent();
        }
        xmlReader.getStruct("blocked-peers");
        while (xmlReader.hasMoreChildren()) {
            xmlReader.getNextStruct();
            PeerID peerID = new PeerID(xmlReader.getFieldValue("peer-id"));
            String nick = xmlReader.getFieldValue("nick");
            peerRelations.addBlockedPeer(peerID);
            xmlReader.gotoParent();
        }

        return new Triple<>(personalData, peerClientData, peerRelations);
    }
}
