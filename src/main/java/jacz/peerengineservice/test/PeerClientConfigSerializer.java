package jacz.peerengineservice.test;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeersPersonalData;
import jacz.peerengineservice.client.connection.NetworkConfiguration;
import jacz.util.io.serialization.StrCast;
import jacz.util.io.xml.XMLReader;
import jacz.util.lists.tuple.Four_Tuple;

import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by Alberto on 20/09/2015.
 */
public class PeerClientConfigSerializer {

    public static Four_Tuple<PeerId, NetworkConfiguration, PeersPersonalData, PeerRelations> readPeerClientData(String path) throws IOException, XMLStreamException, IllegalArgumentException, NumberFormatException {
        XMLReader xmlReader = new XMLReader(path);

        PeerId ownPeerId = new PeerId(xmlReader.getFieldValue("peer-id"));
        NetworkConfiguration networkConfiguration = new NetworkConfiguration(
                Client.NETWORK_CONFIGURATION_PATH,
                StrCast.asInteger(xmlReader.getFieldValue("port")),
                StrCast.asInteger(xmlReader.getFieldValue("external-port")));
        PeersPersonalData peersPersonalData = new PeersPersonalData(
                Client.PEERS_PERSONAL_DATA_PATH,
                "UNNAMED_PEER",
                xmlReader.getFieldValue("nick"));

        PeerRelations peerRelations = new PeerRelations();
        xmlReader.getStruct("friend-peers");
        while (xmlReader.hasMoreChildren()) {
            xmlReader.getNextStruct();
            PeerId peerId = new PeerId(xmlReader.getFieldValue("peer-id"));
            String nick = xmlReader.getFieldValue("nick");
            peersPersonalData.setPeerNick(peerId, nick);
            peerRelations.addFriendPeer(peerId);
            xmlReader.gotoParent();
        }
        xmlReader.getStruct("blocked-peers");
        while (xmlReader.hasMoreChildren()) {
            xmlReader.getNextStruct();
            PeerId peerId = new PeerId(xmlReader.getFieldValue("peer-id"));
            String nick = xmlReader.getFieldValue("nick");
            peersPersonalData.setPeerNick(peerId, nick);
            peerRelations.addBlockedPeer(peerId);
            xmlReader.gotoParent();
        }
        return new Four_Tuple<>(ownPeerId, networkConfiguration, peersPersonalData, peerRelations);
    }
}
