package jacz.peerengineservice.client.connection.peers;

import jacz.peerengineservice.PeerId;
import jacz.storage.ActiveJDBCController;

import java.util.Date;
import java.util.List;

/**
 * Created by Alberto on 02/03/2016.
 */
public class PeerKnowledgeBase {

    private final String dbPath;

    public PeerKnowledgeBase(String dbPath) {
        this.dbPath = dbPath;
    }

    public int size() {
        return getPeers().size();
    }

    public List<PeerEntryFacade> getFriendPeers(String dbPath) {
        return getPeers(Management.Relationship.FAVORITE);
    }

    public List<PeerEntryFacade> get2OrderPeers() {
        return getPeers(Management.Relationship.SECOND_ORDER);
    }

    public List<PeerEntryFacade> getEventualPeers() {
        return getPeers(Management.Relationship.EVENTUAL);
    }

    private List<PeerEntryFacade> getPeers() {
        ActiveJDBCController.connect(dbPath);
        try {
            return PeerEntryFacade.buildList(PeerEntry.findAll());
        } finally {
            ActiveJDBCController.disconnect(dbPath);
        }
    }

    private List<PeerEntryFacade> getPeers(Management.Relationship relationship) {
        ActiveJDBCController.connect(dbPath);
        try {
            return PeerEntryFacade.buildList(PeerEntry.where(Management.RELATIONSHIP + " = ?", relationship.name()));
        } finally {
            ActiveJDBCController.disconnect(dbPath);
        }
    }

    public PeerEntryFacade getPeerEntryFacade(PeerId peerId) {
        return new PeerEntryFacade(getPeerEntry(peerId));
    }

    private PeerEntry getPeerEntry(PeerId peerId) {
        ActiveJDBCController.connect(dbPath);
        try {
            return PeerEntry.findById(peerId);
        } finally {
            ActiveJDBCController.disconnect(dbPath);
        }
    }

    public void addPeerEntries() {

    }

    public void peerConnected(PeerId peerId) {
        PeerEntry peerEntry = getPeerEntry(peerId);
        peerEntry.setString(Management.CONNECTED.name, true);
        peerEntry.saveIt();
    }

    public void peerDisconnected(PeerId peerId) {
        PeerEntry peerEntry = getPeerEntry(peerId);
        peerEntry.setString(Management.CONNECTED.name, false);
        peerEntry.setString(Management.LAST_SESSION.name, Management.dateFormat.format(new Date()));
        peerEntry.saveIt();
    }

    public void connectionAttemptFailed() {

    }

    public void setPeerAffinity(PeerId peerId, int affinity) {
        PeerEntry peerEntry = getPeerEntry(peerId);
        peerEntry.setInteger(Management.AFFINITY.name, affinity);
        peerEntry.saveIt();
    }

    public static void cleanOldEntries() {

    }
}
