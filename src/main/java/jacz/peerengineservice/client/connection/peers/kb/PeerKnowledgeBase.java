package jacz.peerengineservice.client.connection.peers.kb;

import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.connection.PeerAddress;
import org.aanguita.jacuzzi.io.serialization.activejdbcsupport.ActiveJDBCController;
import org.aanguita.jacuzzi.lists.tuple.Duple;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class gives access to the peer knowledge base. We can retrieve specific peers fulfilling certain conditions,
 * or count peers.
 */
public class PeerKnowledgeBase {

    public enum ConnectedQuery {
        ALL,
        DISCONNECTED,
        CONNECTED;

        public boolean producesQuery() {
            return this == DISCONNECTED || this == CONNECTED;
        }

        public Duple<String, Object> buildQuery() {
            switch (this) {

                case DISCONNECTED:
                    return new Duple<>(Management.IS_CONNECTED.name + " = ?", false);
                case CONNECTED:
                    return new Duple<>(Management.IS_CONNECTED.name + " = ?", true);
                default:
                    throw new RuntimeException();
            }
        }
    }

    /**
     * 2-week threshold, in milliseconds
     */
    private static final long ELDERLY_THRESHOLD = 1000L * 60L * 60L * 24L * 14L;

    static final String DATABASE = "peerKnowledgeBase";

    private final String dbPath;

    public PeerKnowledgeBase(String dbPath) {
        this.dbPath = dbPath;
        newSession();
    }

    public static PeerKnowledgeBase createNew(String dbPath) {
        Management.dropAndCreateKBDatabase(dbPath);
        return new PeerKnowledgeBase(dbPath);
    }

    private void newSession() {
        try {
            ActiveJDBCController.connect(DATABASE, dbPath);
            // todo check null
            Long nullLong = null;
            PeerEntry.updateAll(Management.LAST_CONNECTION_ATTEMPT.name + " = ?", nullLong);
            PeerEntry.update(Management.WISH_REGULAR_CONNECTIONS.name + " = ?", Management.WISH_REGULAR_CONNECTIONS.name + " = ?", Management.ConnectionWish.YES, Management.ConnectionWish.NOT_NOW);
            PeerEntry.updateAll(Management.IS_CONNECTED.name + " = ?", false);
            cleanOldEntries();
        } finally {
            ActiveJDBCController.disconnect();
        }
    }

    public int getPeerCount(ConnectedQuery connectedQuery) {
        ActiveJDBCController.connect(DATABASE, dbPath);
        int count;
        if (connectedQuery.producesQuery()) {
            Duple<String, Object> buildQuery = connectedQuery.buildQuery();
            count = PeerEntry.count(buildQuery.element1, buildQuery.element2).intValue();
        } else {
            count = PeerEntry.count().intValue();
        }
        ActiveJDBCController.disconnect();
        return count;
    }

    public List<PeerEntryFacade> getFavoritePeers(ConnectedQuery connectedQuery) {
        return getPeers(Management.Relationship.FAVORITE, connectedQuery, null);
    }

    public int getFavoritePeersCount(ConnectedQuery connectedQuery) {
        return getPeersCount(Management.Relationship.FAVORITE, connectedQuery, null);
    }

    public List<PeerEntryFacade> getRegularPeers(ConnectedQuery connectedQuery) {
        return getPeers(Management.Relationship.REGULAR, connectedQuery, null);
    }

    public List<PeerEntryFacade> getRegularPeers(ConnectedQuery connectedQuery, CountryCode country) {
        return getPeers(Management.Relationship.REGULAR, connectedQuery, country.toString());
    }

    public int getRegularPeersCount(ConnectedQuery connectedQuery) {
        return getPeersCount(Management.Relationship.REGULAR, connectedQuery, null);
    }

    public int getRegularPeersCount(ConnectedQuery connectedQuery, CountryCode country) {
        return getPeersCount(Management.Relationship.REGULAR, connectedQuery, country.toString());
    }

    public List<PeerEntryFacade> getBlockedPeers(ConnectedQuery connectedQuery) {
        return getPeers(Management.Relationship.BLOCKED, connectedQuery, null);
    }

    public int getBlockedPeersCount(ConnectedQuery connectedQuery) {
        return getPeersCount(Management.Relationship.BLOCKED, connectedQuery, null);
    }

    private List<PeerEntryFacade> getPeers(Management.Relationship relationship, ConnectedQuery connectedQuery, String country) {
        // first, order by affinity (descending, higher affinity comes first)
        // upon same affinity, order by last connection attempt (ascending). Older last connection attempts come first,
        // with null values (no connection attempt this session) first of all
        // upon same last connection attempt (mainly, upon those with null last connection attempt), order by
        // last session, descending (newer last sessions come first, with null values (those we have never connected)
        // at the end of all. This way we favor those peers who we have contacted at least once in our lives
        ActiveJDBCController.connect(DATABASE, dbPath);
        try {
            Duple<String, Object[]> queryAndParams = buildQuery(relationship, connectedQuery, country);
            return PeerEntryFacade.buildList(
                    PeerEntry.where(
                            queryAndParams.element1, queryAndParams.element2)
                            .orderBy(Management.AFFINITY.name + " DESC")
                            .orderBy(Management.LAST_CONNECTION_ATTEMPT.name)
                            .orderBy(Management.LAST_SESSION.name + " DESC"),
                    dbPath);
        } finally {
            ActiveJDBCController.disconnect();
        }
    }

    private int getPeersCount(Management.Relationship relationship, ConnectedQuery connectedQuery, String country) {
        ActiveJDBCController.connect(DATABASE, dbPath);
        try {
            Duple<String, Object[]> queryAndParams = buildQuery(relationship, connectedQuery, country);
            return PeerEntry.count(queryAndParams.element1, queryAndParams.element2).intValue();
        } finally {
            ActiveJDBCController.disconnect();
        }
    }

    private Duple<String, Object[]> buildQuery(Management.Relationship relationship, ConnectedQuery connectedQuery, String country) {
        String query = Management.RELATIONSHIP.name + " = ?";
        List<Object> params = new ArrayList<>();
        params.add(relationship.name());
        if (connectedQuery.producesQuery()) {
            Duple<String, Object> buildQuery = connectedQuery.buildQuery();
            query += " AND " + buildQuery.element1;
            params.add(buildQuery.element2);
        }
        if (country != null) {
            query += " AND " + Management.MAIN_COUNTRY.name + " = ?";
            params.add(country);
        }
        Object[] paramsArray = new Object[params.size()];
        paramsArray = params.toArray(paramsArray);
        return new Duple<>(query, paramsArray);
    }

    public PeerEntryFacade getPeerEntryFacade(PeerId peerId) {
        ActiveJDBCController.connect(DATABASE, dbPath);
        PeerEntry peerEntry = getPeerEntry(peerId);
        PeerEntryFacade peerEntryFacade = peerEntry != null ? new PeerEntryFacade(peerEntry, dbPath) : new PeerEntryFacade(peerId, dbPath);
        ActiveJDBCController.disconnect();
        return peerEntryFacade;
    }

    private PeerEntry getPeerEntry(PeerId peerId) {
        ActiveJDBCController.connect(DATABASE, dbPath);
        try {
            return PeerEntry.findFirst(Management.PEER_ID.name + " = ?", peerId.toString());
        } finally {
            ActiveJDBCController.disconnect();
        }
    }

    private void cleanOldEntries() {
        // delete those peers to which we have never had a connection
        // then delete those peers whose last session is older than a given threshold
        // todo check null
        String nullString = null;
        PeerEntry.delete(Management.LAST_SESSION.name + " = ?", nullString);
        PeerEntry.delete(Management.LAST_SESSION.name + " < ?", new Date().getTime() - ELDERLY_THRESHOLD);
    }

    public void clearAllPeerAddresses() {
        try {
            ActiveJDBCController.connect(DATABASE, dbPath);
            PeerEntry.updateAll(Management.ADDRESS.name + " = ?", PeerAddress.nullPeerAddress());
        } finally {
            ActiveJDBCController.disconnect();
        }
    }

    public void clearAllData() {
        try {
            ActiveJDBCController.connect(DATABASE, dbPath);
            PeerEntry.deleteAll();
        } finally {
            ActiveJDBCController.disconnect();
        }
    }
}
