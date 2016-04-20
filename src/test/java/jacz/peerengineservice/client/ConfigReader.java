package jacz.peerengineservice.client;

import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.connection.NetworkConfiguration;
import jacz.peerengineservice.client.connection.peers.PeerConnectionConfig;
import jacz.peerengineservice.client.connection.peers.kb.Management;
import jacz.peerengineservice.client.connection.peers.kb.PeerKnowledgeBase;
import jacz.peerengineservice.util.datatransfer.TransferStatistics;
import jacz.util.io.serialization.StrCast;
import jacz.util.io.xml.XMLReader;
import jacz.util.lists.tuple.SixTuple;
import org.apache.commons.io.FileUtils;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Alberto on 13/04/2016.
 */
public class ConfigReader {

    private static final String NETWORK_CONFIGURATION = "networkConfig";

    private static final String PEERS_PERSONAL_DATA = "peersPersonalData";

    private static final String PEER_KNOWLEDGE_BASE = "peerKnowledgeBase";

    private static final String PEER_CONNECTION_CONFIG = "peerConnectionConfig";

    private static final String TRANSFER_STATISTICS = "transferStatistics";

    public static SixTuple<PeerId, String, String, String, String, String> readPeerClientData(
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

        String peerConnectionConfigPath = FileUtils.getFile(userDir, PEER_CONNECTION_CONFIG + ".db").getPath();
        buildPeerConnectionConfig(peerConnectionConfigPath);

        String transferStatisticsPath = FileUtils.getFile(userDir, TRANSFER_STATISTICS + ".db").getPath();
        if (!new File(transferStatisticsPath).isFile()) {
            TransferStatistics.createNew(transferStatisticsPath);
        }

        return new SixTuple<>(ownPeerId, networkConfigurationPath, peersPersonalDataPath, peerKnowledgeBasePath, peerConnectionConfigPath, transferStatisticsPath);
    }

    private static void buildPeerConnectionConfig(String peerConnectionConfigPath) throws IOException {
        new PeerConnectionConfig(peerConnectionConfigPath, 100, false, CountryCode.ES, new ArrayList<>(), 10);
    }
}
