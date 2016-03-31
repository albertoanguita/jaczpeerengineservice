package jacz.peerengineservice.client.connection.peers.kb;

import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineservice.PeerId;
import jacz.storage.ActiveJDBCController;
import org.javalite.activejdbc.Base;
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
        Base.openTransaction();
        peerEntry = new PeerEntry();
        peerEntry.setString(Management.PEER_ID.name, peerId.toString());
        init();
        peerEntry.insert();
        Base.commitTransaction();
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
        ActiveJDBCController.connect(dbPath);
        PeerId peerId = new PeerId(peerEntry.getString(Management.PEER_ID.name));
        ActiveJDBCController.disconnect(dbPath);
        return peerId;
    }

    public CountryCode getMainCountry() {
        ActiveJDBCController.connect(dbPath);
        String mainCountry = peerEntry.getString(Management.MAIN_COUNTRY.name);
        ActiveJDBCController.disconnect(dbPath);
        return mainCountry != null ? CountryCode.valueOf(mainCountry) : null;
    }

    public void setMainCountry(CountryCode mainCountry) {
        ActiveJDBCController.connect(dbPath);
        peerEntry.setString(Management.MAIN_COUNTRY.name, mainCountry.toString());
        peerEntry.saveIt();
        ActiveJDBCController.disconnect(dbPath);
    }

    public Management.Relationship getRelationship() {
        ActiveJDBCController.connect(dbPath);
        String relationshipValue = peerEntry.getString(Management.RELATIONSHIP.name);
        Management.Relationship relationship = relationshipValue != null ? Management.Relationship.valueOf(relationshipValue) : null;
        ActiveJDBCController.disconnect(dbPath);
        return relationship;
    }

    public void setRelationship(Management.Relationship relationship) {
        ActiveJDBCController.connect(dbPath);
        peerEntry.setString(Management.RELATIONSHIP.name, relationship.name());
        peerEntry.saveIt();
        ActiveJDBCController.disconnect(dbPath);
    }

    public Management.Relationship getRelationshipToUs() {
        ActiveJDBCController.connect(dbPath);
        String relationshipValue = peerEntry.getString(Management.RELATIONSHIP_TO_US.name);
        Management.Relationship relationship = relationshipValue != null ? Management.Relationship.valueOf(relationshipValue) : null;
        ActiveJDBCController.disconnect(dbPath);
        return relationship;
    }

    public void setRelationshipToUs(Management.Relationship relationship) {
        ActiveJDBCController.connect(dbPath);
        peerEntry.setString(Management.RELATIONSHIP_TO_US.name, relationship.name());
        peerEntry.saveIt();
        ActiveJDBCController.disconnect(dbPath);
    }

    public Management.ConnectionWish getWishForRegularConnections() {
        ActiveJDBCController.connect(dbPath);
        String wishValue = peerEntry.getString(Management.WISH_REGULAR_CONNECTIONS.name);
        Management.ConnectionWish wish = wishValue != null ? Management.ConnectionWish.valueOf(wishValue) : null;
        ActiveJDBCController.disconnect(dbPath);
        return wish;
    }

    public Boolean isWishForRegularConnections() {
        Management.ConnectionWish connectionWish = getWishForRegularConnections();
        return connectionWish != null ? connectionWish.isConnectionWish() : null;
    }

    public void setWishForRegularConnections(Management.ConnectionWish wish) {
        ActiveJDBCController.connect(dbPath);
        peerEntry.setString(Management.WISH_REGULAR_CONNECTIONS.name, wish.name());
        peerEntry.saveIt();
        ActiveJDBCController.disconnect(dbPath);
    }

    public boolean isConnected() {
        ActiveJDBCController.connect(dbPath);
        boolean connected = peerEntry.getBoolean(Management.IS_CONNECTED.name);
        ActiveJDBCController.disconnect(dbPath);
        return connected;
    }

    public void setConnected(boolean connected) {
        ActiveJDBCController.connect(dbPath);
        peerEntry.setBoolean(Management.IS_CONNECTED.name, connected);
        if (!connected) {
            // also update last session
            peerEntry.setLong(Management.LAST_SESSION.name, new Date().getTime());
        }
        peerEntry.saveIt();
        ActiveJDBCController.disconnect(dbPath);
    }

    public Date getLastSession() {
        ActiveJDBCController.connect(dbPath);
        Long date = peerEntry.getLong(Management.LAST_SESSION.name);
        ActiveJDBCController.disconnect(dbPath);
        return date != null ? new Date(date) : null;
    }

    public Date getLastConnectionAttempt() {
        ActiveJDBCController.connect(dbPath);
        Long date = peerEntry.getLong(Management.LAST_CONNECTION_ATTEMPT.name);
        ActiveJDBCController.disconnect(dbPath);
        return date != null ? new Date(date) : null;
    }

    public void updateConnectionAttempt() {
        ActiveJDBCController.connect(dbPath);
        peerEntry.setLong(Management.LAST_CONNECTION_ATTEMPT.name, new Date().getTime());
        peerEntry.saveIt();
        ActiveJDBCController.disconnect(dbPath);
    }

    public int getAffinity() {
        ActiveJDBCController.connect(dbPath);
        int affinity = peerEntry.getInteger(Management.AFFINITY.name);
        ActiveJDBCController.disconnect(dbPath);
        return affinity;
    }

    public void setAffinity(int affinity) {
        ActiveJDBCController.connect(dbPath);
        peerEntry.setInteger(Management.AFFINITY.name, affinity);
        peerEntry.saveIt();
        ActiveJDBCController.disconnect(dbPath);
    }

    public PeerAddress getPeerAddress() {
        ActiveJDBCController.connect(dbPath);
        try {
            String peerAddressString = peerEntry.getString(Management.ADDRESS.name);
            return peerAddressString != null ? new PeerAddress(peerAddressString) : new PeerAddress();
        } catch (IOException e) {
            // delete stored peer address and return unknown address;
            peerEntry.setString(Management.ADDRESS.name, null);
            return null;
        } finally {
            ActiveJDBCController.disconnect(dbPath);
        }
    }

    public void setPeerAddress(PeerAddress peerAddress) {
        ActiveJDBCController.connect(dbPath);
        peerEntry.setString(Management.ADDRESS.name, peerAddress.serialize());
        peerEntry.saveIt();
        ActiveJDBCController.disconnect(dbPath);
    }
}
