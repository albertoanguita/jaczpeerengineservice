package jacz.peerengineservice.client.connection.peers.kb;

import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineservice.PeerId;
import jacz.storage.ActiveJDBCController;
import jacz.util.lists.tuple.Duple;

import java.util.ArrayList;
import java.util.List;

/**
 * todo cache written data, like in datastore
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
        // todo clear connection attempts
        // todo change wish_connection from not_now to yes
        // todo change is_connected to false
        cleanOldEntries();
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
        // todo: upon same affinity, older connection attempts are favored.
        // todo: never connected peers are prioritized. Do I really want this??? We should first try peers that have been connected at least once, and then new peers
        // todo but that will make it very difficult for new peers to have a chance. Think about this...
        ActiveJDBCController.connect(DATABASE, dbPath);
        try {
            Duple<String, Object[]> queryAndParams = buildQuery(relationship, connectedQuery, country);
            return PeerEntryFacade.buildList(
                    PeerEntry.where(
                            queryAndParams.element1, queryAndParams.element2)
                            .orderBy(Management.AFFINITY.name + " desc, " + Management.LAST_CONNECTION_ATTEMPT.name),
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

    public static void cleanOldEntries() {
        // todo
    }
}
