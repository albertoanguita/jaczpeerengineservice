package jacz.peerengineservice.client.connection.peers.kb;

import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.connection.PeerAddress;
import jacz.storage.ActiveJDBCController;
import org.javalite.activejdbc.Model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Interface to the entries of the peer knowledge base. Objects of this class give access to a single entry,
 * corresponding to a single peer
 */
public class PeerEntryFacade {

    private final PeerEntry peerEntry;

    private final String dbPath;

    PeerEntryFacade(Model peerEntry, String dbPath) {
        this.peerEntry = (PeerEntry) peerEntry;
        this.dbPath = dbPath;
    }

    PeerEntryFacade(PeerId peerId, String dbPath) {
        ActiveJDBCController.getDB().openTransaction();
        peerEntry = new PeerEntry();
        peerEntry.setString(Management.PEER_ID.name, peerId.toString());
        init();
        peerEntry.insert();
        ActiveJDBCController.getDB().commitTransaction();
        this.dbPath = dbPath;
    }

    private void init() {
        // set all default fields
        peerEntry.setString(Management.RELATIONSHIP.name, Management.Relationship.REGULAR.name());
        peerEntry.setString(Management.RELATIONSHIP_TO_US.name, Management.Relationship.REGULAR.name());
        peerEntry.setString(Management.WISH_REGULAR_CONNECTIONS.name, Management.ConnectionWish.YES.name());
        peerEntry.setBoolean(Management.IS_CONNECTED.name, false);
        peerEntry.setInteger(Management.AFFINITY.name, 0);
    }

    public void openTransaction() {
        ActiveJDBCController.connect(PeerKnowledgeBase.DATABASE, dbPath);
        ActiveJDBCController.getDB().openTransaction();
    }

    public void commitTransaction() {
        ActiveJDBCController.getDB().commitTransaction();
        ActiveJDBCController.disconnect();
    }

    static List<PeerEntryFacade> buildList(List<? extends Model> peerEntries, String dbPath) {
        List<PeerEntryFacade> peerEntryFacades = new ArrayList<>();
        for (Model peerEntry : peerEntries) {
            if (peerEntry != null) {
                peerEntryFacades.add(new PeerEntryFacade(peerEntry, dbPath));
            }
        }
        return peerEntryFacades;
    }

    public PeerId getPeerId() {
        ActiveJDBCController.connect(PeerKnowledgeBase.DATABASE, dbPath);
        PeerId peerId = new PeerId(peerEntry.getString(Management.PEER_ID.name));
        ActiveJDBCController.disconnect();
        return peerId;
    }

    public CountryCode getMainCountry() {
        ActiveJDBCController.connect(PeerKnowledgeBase.DATABASE, dbPath);
        String mainCountry = peerEntry.getString(Management.MAIN_COUNTRY.name);
        ActiveJDBCController.disconnect();
        return mainCountry != null ? CountryCode.valueOf(mainCountry) : null;
    }

    public void setMainCountry(CountryCode mainCountry) {
        ActiveJDBCController.connect(PeerKnowledgeBase.DATABASE, dbPath);
        peerEntry.setString(Management.MAIN_COUNTRY.name, mainCountry.toString());
        peerEntry.saveIt();
        ActiveJDBCController.disconnect();
    }

    public Management.Relationship getRelationship() {
        ActiveJDBCController.connect(PeerKnowledgeBase.DATABASE, dbPath);
        String relationshipValue = peerEntry.getString(Management.RELATIONSHIP.name);
        Management.Relationship relationship = relationshipValue != null ? Management.Relationship.valueOf(relationshipValue) : null;
        ActiveJDBCController.disconnect();
        return relationship;
    }

    public void setRelationship(Management.Relationship relationship) {
        ActiveJDBCController.connect(PeerKnowledgeBase.DATABASE, dbPath);
        peerEntry.setString(Management.RELATIONSHIP.name, relationship.name());
        peerEntry.saveIt();
        ActiveJDBCController.disconnect();
    }

    public Management.Relationship getRelationshipToUs() {
        ActiveJDBCController.connect(PeerKnowledgeBase.DATABASE, dbPath);
        String relationshipValue = peerEntry.getString(Management.RELATIONSHIP_TO_US.name);
        Management.Relationship relationship = relationshipValue != null ? Management.Relationship.valueOf(relationshipValue) : null;
        ActiveJDBCController.disconnect();
        return relationship;
    }

    public void setRelationshipToUs(Management.Relationship relationship) {
        ActiveJDBCController.connect(PeerKnowledgeBase.DATABASE, dbPath);
        peerEntry.setString(Management.RELATIONSHIP_TO_US.name, relationship.name());
        peerEntry.saveIt();
        ActiveJDBCController.disconnect();
    }

    public Management.ConnectionWish getWishForRegularConnections() {
        ActiveJDBCController.connect(PeerKnowledgeBase.DATABASE, dbPath);
        String wishValue = peerEntry.getString(Management.WISH_REGULAR_CONNECTIONS.name);
        Management.ConnectionWish wish = wishValue != null ? Management.ConnectionWish.valueOf(wishValue) : null;
        ActiveJDBCController.disconnect();
        return wish;
    }

    public Boolean isWishForRegularConnections() {
        Management.ConnectionWish connectionWish = getWishForRegularConnections();
        return connectionWish != null ? connectionWish.isConnectionWish() : null;
    }

    public void setWishForRegularConnections(Management.ConnectionWish wish) {
        ActiveJDBCController.connect(PeerKnowledgeBase.DATABASE, dbPath);
        peerEntry.setString(Management.WISH_REGULAR_CONNECTIONS.name, wish.name());
        peerEntry.saveIt();
        ActiveJDBCController.disconnect();
    }

    public boolean isConnected() {
        ActiveJDBCController.connect(PeerKnowledgeBase.DATABASE, dbPath);
        boolean connected = peerEntry.getBoolean(Management.IS_CONNECTED.name);
        ActiveJDBCController.disconnect();
        return connected;
    }

    public void setConnected(boolean connected) {
        ActiveJDBCController.connect(PeerKnowledgeBase.DATABASE, dbPath);
        peerEntry.setBoolean(Management.IS_CONNECTED.name, connected);
        if (!connected) {
            // also update last session and "clear" last connection attempt
            peerEntry.setLong(Management.LAST_SESSION.name, new Date().getTime());
            peerEntry.setLong(Management.LAST_CONNECTION_ATTEMPT.name, null);
        }
        peerEntry.saveIt();
        ActiveJDBCController.disconnect();
    }

    public Date getLastSession() {
        ActiveJDBCController.connect(PeerKnowledgeBase.DATABASE, dbPath);
        Long date = peerEntry.getLong(Management.LAST_SESSION.name);
        ActiveJDBCController.disconnect();
        return date != null ? new Date(date) : null;
    }

    public Date getLastConnectionAttempt() {
        ActiveJDBCController.connect(PeerKnowledgeBase.DATABASE, dbPath);
        Long date = peerEntry.getLong(Management.LAST_CONNECTION_ATTEMPT.name);
        ActiveJDBCController.disconnect();
        return date != null ? new Date(date) : null;
    }

    public void updateConnectionAttempt() {
        ActiveJDBCController.connect(PeerKnowledgeBase.DATABASE, dbPath);
        peerEntry.setLong(Management.LAST_CONNECTION_ATTEMPT.name, new Date().getTime());
        peerEntry.saveIt();
        ActiveJDBCController.disconnect();
    }

    public int getAffinity() {
        ActiveJDBCController.connect(PeerKnowledgeBase.DATABASE, dbPath);
        int affinity = peerEntry.getInteger(Management.AFFINITY.name);
        ActiveJDBCController.disconnect();
        return affinity;
    }

    public void setAffinity(int affinity) {
        ActiveJDBCController.connect(PeerKnowledgeBase.DATABASE, dbPath);
        peerEntry.setInteger(Management.AFFINITY.name, affinity);
        peerEntry.saveIt();
        ActiveJDBCController.disconnect();
    }

    public PeerAddress getPeerAddress() {
        ActiveJDBCController.connect(PeerKnowledgeBase.DATABASE, dbPath);
        try {
            String peerAddressString = peerEntry.getString(Management.ADDRESS.name);
            return peerAddressString != null ? new PeerAddress(peerAddressString) : PeerAddress.nullPeerAddress();
        } catch (IOException e) {
            // delete stored peer address and return unknown address;
            peerEntry.setString(Management.ADDRESS.name, null);
            return null;
        } finally {
            ActiveJDBCController.disconnect();
        }
    }

    public void setPeerAddress(PeerAddress peerAddress) {
        ActiveJDBCController.connect(PeerKnowledgeBase.DATABASE, dbPath);
        peerEntry.setString(Management.ADDRESS.name, peerAddress.serialize());
        peerEntry.saveIt();
        ActiveJDBCController.disconnect();
    }

    @Override
    public String toString() {
        return "PeerEntryFacade{" +
                "peerId=" + getPeerId() +
                "mainCountry=" + getMainCountry() +
                "relationship=" + getRelationship() +
                "relationshipToUs=" + getRelationshipToUs() +
                "wishRegularConnections=" + getWishForRegularConnections() +
                "isConnected=" + isConnected() +
                "lastSession=" + getLastSession() +
                "lastConnectionAttempt=" + getLastConnectionAttempt() +
                "affinity=" + getAffinity() +
                "address=" + getPeerAddress() +
                '}';
    }
}
