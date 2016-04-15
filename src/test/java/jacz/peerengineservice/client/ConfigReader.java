package jacz.peerengineservice.client;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.connection.NetworkConfiguration;
import jacz.peerengineservice.client.connection.peers.kb.Management;
import jacz.peerengineservice.client.connection.peers.kb.PeerKnowledgeBase;
import jacz.util.io.serialization.StrCast;
import jacz.util.io.xml.XMLReader;
import jacz.util.lists.tuple.Four_Tuple;
import org.apache.commons.io.FileUtils;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;

/**
 * Created by Alberto on 13/04/2016.
 */
public class ConfigReader {

    private static final String NETWORK_CONFIGURATION = "networkConfig";

    private static final String PEERS_PERSONAL_DATA = "peersPersonalData";

    private static final String PEER_KNOWLEDGE_BASE = "peerKnowledgeBase";

    public static Four_Tuple<PeerId, String, String, String> readPeerClientData(
            String path,
            String userDir
    ) throws IOException, XMLStreamException, IllegalArgumentException {
        XMLReader xmlReader = new XMLReader(path);
        FileUtils.forceMkdir(new File(userDir));

        PeerId ownPeerId = new PeerId(xmlReader.getFieldValue("peer-id"));

        String networkConfigurationPath = FileUtils.getFile(userDir, NETWORK_CONFIGURATION + ".db").getPath();
        new NetworkConfiguration(
                networkConfigurationPath,
                StrCast.asInteger(xmlReader.getFieldValue("port")),
                StrCast.asInteger(xmlReader.getFieldValue("external-port")));

        String peersPersonalDataPath = FileUtils.getFile(userDir, PEERS_PERSONAL_DATA + ".db").getPath();
        PeersPersonalData peersPersonalData = new PeersPersonalData(
                peersPersonalDataPath,
                "UNNAMED_PEER",
                xmlReader.getFieldValue("nick"));

        String peerKnowledgeBasePath = FileUtils.getFile(userDir, PEER_KNOWLEDGE_BASE + ".db").getPath();
        PeerKnowledgeBase peerKnowledgeBase = PeerKnowledgeBase.createNew(peerKnowledgeBasePath);

        xmlReader.getStruct("friend-peers");
        while (xmlReader.hasMoreChildren()) {
            xmlReader.getNextStruct();
            PeerId peerId = new PeerId(xmlReader.getFieldValue("peer-id"));
            String nick = xmlReader.getFieldValue("nick");
            peersPersonalData.setPeerNick(peerId, nick);
            peerKnowledgeBase.getPeerEntryFacade(peerId).setRelationship(Management.Relationship.FAVORITE);
            xmlReader.gotoParent();
        }
        xmlReader.getStruct("blocked-peers");
        while (xmlReader.hasMoreChildren()) {
            xmlReader.getNextStruct();
            PeerId peerId = new PeerId(xmlReader.getFieldValue("peer-id"));
            String nick = xmlReader.getFieldValue("nick");
            peersPersonalData.setPeerNick(peerId, nick);
            peerKnowledgeBase.getPeerEntryFacade(peerId).setRelationship(Management.Relationship.BLOCKED);
            xmlReader.gotoParent();
        }
        return new Four_Tuple<>(ownPeerId, networkConfigurationPath, peersPersonalDataPath, peerKnowledgeBasePath);
    }
}
