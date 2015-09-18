package jacz.peerengineservice.test;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClientData;
import jacz.peerengineservice.client.PeerRelations;
import jacz.util.io.xml.Element;
import jacz.util.io.xml.XMLDom;
import jacz.util.network.IP4Port;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PeerClientConfigIO {

    static final String NICK = "nick";

    static final String STATE = "state";

    static final String MESSAGE = "message";

    static final String AVATAR = "avatar";

    static final String PORT = "port";

    static final String PEER_ID = "peer-id";

    static final String PEER_SERVER_DATA = "peer-server-data";

    static final String IP = "ip";

    static final String FRIEND_PEERS = "friend-peers";

    static final String BLOCKED_PEERS = "blocked-peers";

    static final String PEER = "peer";

    /*public static PeerClientConfig readPeerClientConfig(Document doc) {
        Element root = doc.getRootElement();
        String nick = root.getChild(NICK).getText();
        int port = Integer.parseInt(root.getChild(PORT).getText());
        PeerID peerID = new PeerID(Long.parseLong(root.getChild(PEER_ID).getText()));
        PeerServerData peerServerData = readPeerServerData(root.getChild(PEER_SERVER_DATA));
        List<FriendPeerData> friendPeerDatas = readFriendPeers(root.getChild(FRIEND_PEERS));

        return new PeerClientConfig(nick, port, peerID, peerServerData, friendPeerDatas);
    }*/

    public static List<Object> readPeerClientData(Element root) {
        PersonalData personalData = new PersonalData(readNick(root), readAvatar(root));

        int port = Integer.parseInt(root.getChild(PORT).getText());
        PeerID peerID = new PeerID(root.getChild(PEER_ID).getText());
        jacz.peerengineservice.client.PeerServerData peerServerData = readPeerServerData(root.getChild(PEER_SERVER_DATA));
        PeerClientData peerClientData = new PeerClientData(peerID, port, peerServerData);

        PeerRelations peerRelations = readFriendPeers(root.getChild(FRIEND_PEERS));

        List<Object> result = new ArrayList<Object>(3);
        result.add(personalData);
        result.add(peerClientData);
        result.add(peerRelations);
        return result;
    }

    private static String readNick(Element root) {
        return root.getChild(NICK).getText();
    }

    private static String readAvatar(Element root) {
        return root.getChild(AVATAR).getText();
    }

    private static jacz.peerengineservice.client.PeerServerData readPeerServerData(Element peerServerDataElement) {
        String ip = peerServerDataElement.getChild(IP).getText();
        int port = Integer.parseInt(peerServerDataElement.getChild(PORT).getText());
        return new jacz.peerengineservice.client.PeerServerData(new IP4Port(ip, port));
    }

    private static PeerRelations readPeerRelations(Element root) {
        PeerRelations peerRelations = new PeerRelations();
        Element friendPeersElement = root.getChild(FRIEND_PEERS);
        for (Object o : friendPeersElement.getChildren(PEER)) {
            Element friendPeerElement = (Element) o;
            PeerID peerID = new PeerID(friendPeerElement.getChild(PeerClientConfigIO.PEER_ID).getText());
            peerRelations.addFriendPeer(peerID);
        }
        Element blockedPeersElement = root.getChild(BLOCKED_PEERS);
        for (Object o : blockedPeersElement.getChildren(PEER)) {
            Element blockedPeerElement = (Element) o;
            PeerID peerID = new PeerID(blockedPeerElement.getChild(PeerClientConfigIO.PEER_ID).getText());
            peerRelations.addBlockedPeer(peerID);
        }
        return peerRelations;
    }

    private static PeerRelations readFriendPeers(Element friendPeersElement) {
        PeerRelations peerRelations = new PeerRelations();
        for (Object o : friendPeersElement.getChildren(PEER)) {
            Element friendPeerElement = (Element) o;
            PeerID peerID = new PeerID(friendPeerElement.getChild(PeerClientConfigIO.PEER_ID).getText());
            peerRelations.addFriendPeer(peerID);
            //FriendPeerData friendPeerData = FriendPeerIO.readFriendPeer(friendPeerElement);
            //friendPeerListData.put(friendPeerData.getPeerID(), friendPeerData);
        }
        return peerRelations;
    }

    public static void writePeerClientConfig(String path, Element element) throws IOException, XMLStreamException {
        XMLDom.write(path, element);
    }
}
