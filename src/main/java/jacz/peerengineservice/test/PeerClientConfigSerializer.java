package jacz.peerengineservice.test;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeersPersonalData;
import jacz.peerengineservice.client.connection.NetworkConfiguration;
import jacz.util.io.serialization.StrCast;
import jacz.util.io.xml.XMLReader;
import jacz.util.lists.tuple.Four_Tuple;

import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;

/**
 * Created by Alberto on 20/09/2015.
 */
public class PeerClientConfigSerializer {

    public static Four_Tuple<PeerID, NetworkConfiguration, PeersPersonalData, PeerRelations> readPeerClientData(String path) throws FileNotFoundException, XMLStreamException, IllegalArgumentException, NumberFormatException {
        XMLReader xmlReader = new XMLReader(path);

        PeerID ownPeerID = new PeerID(xmlReader.getFieldValue("peer-id"));
        NetworkConfiguration networkConfiguration = new NetworkConfiguration(
                StrCast.asInteger(xmlReader.getFieldValue("port")),
                StrCast.asInteger(xmlReader.getFieldValue("external-port")));
        PeersPersonalData peersPersonalData = new PeersPersonalData("UNNAMED_PEER", xmlReader.getFieldValue("nick"));

        PeerRelations peerRelations = new PeerRelations();
        xmlReader.getStruct("friend-peers");
        while (xmlReader.hasMoreChildren()) {
            xmlReader.getNextStruct();
            PeerID peerID = new PeerID(xmlReader.getFieldValue("peer-id"));
            String nick = xmlReader.getFieldValue("nick");
            peersPersonalData.setPeersNicks(peerID, nick);
            peerRelations.addFriendPeer(peerID);
            xmlReader.gotoParent();
        }
        xmlReader.getStruct("blocked-peers");
        while (xmlReader.hasMoreChildren()) {
            xmlReader.getNextStruct();
            PeerID peerID = new PeerID(xmlReader.getFieldValue("peer-id"));
            String nick = xmlReader.getFieldValue("nick");
            peersPersonalData.setPeersNicks(peerID, nick);
            peerRelations.addBlockedPeer(peerID);
            xmlReader.gotoParent();
        }
        return new Four_Tuple<>(ownPeerID, networkConfiguration, peersPersonalData, peerRelations);
    }
}
