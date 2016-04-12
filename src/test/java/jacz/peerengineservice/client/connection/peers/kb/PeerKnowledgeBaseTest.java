package jacz.peerengineservice.client.connection.peers.kb;

import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.connection.PeerAddress;
import jacz.util.network.IP4Port;
import org.junit.Assert;

import java.util.Date;
import java.util.List;

/**
 * Created by Alberto on 08/03/2016.
 */
public class PeerKnowledgeBaseTest {

    private static final String dbPath = "peerKB.db";

    @org.junit.Test
    public void createDatabase() {

        Management.dropAndCreateKBDatabase(dbPath);
    }

    @org.junit.Test
    public void testDatabase() {

        Management.dropAndCreateKBDatabase(dbPath);

        PeerKnowledgeBase pkb = new PeerKnowledgeBase(dbPath);
        PeerId peerId01 = PeerId.buildTestPeerId("01");
        PeerId peerId02 = PeerId.buildTestPeerId("02");
        PeerId peerId03 = PeerId.buildTestPeerId("03");
        PeerId peerId04 = PeerId.buildTestPeerId("04");
        PeerId peerId05 = PeerId.buildTestPeerId("05");
        PeerId peerId06 = PeerId.buildTestPeerId("06");
        PeerId peerId07 = PeerId.buildTestPeerId("07");
        PeerId peerId08 = PeerId.buildTestPeerId("08");
        PeerId peerId09 = PeerId.buildTestPeerId("09");
        PeerId peerId10 = PeerId.buildTestPeerId("10");

        Assert.assertEquals(0, pkb.getPeerCount(PeerKnowledgeBase.ConnectedQuery.ALL));
        Assert.assertEquals(0, pkb.getFavoritePeersCount(PeerKnowledgeBase.ConnectedQuery.ALL));
        Assert.assertEquals(0, pkb.getRegularPeersCount(PeerKnowledgeBase.ConnectedQuery.ALL));

        // build peer entry
        pkb.getPeerEntryFacade(peerId01);

        // retrieve just built entry
        PeerEntryFacade peerEntryFacade = pkb.getPeerEntryFacade(peerId01);
        Assert.assertEquals(peerId01, peerEntryFacade.getPeerId());
//        Assert.assertEquals(null, peerEntryFacade.getMainLanguage());
        Assert.assertEquals(null, peerEntryFacade.getMainCountry());
        Assert.assertEquals(Management.Relationship.REGULAR, peerEntryFacade.getRelationship());
        Assert.assertEquals(Management.Relationship.REGULAR, peerEntryFacade.getRelationshipToUs());
        Assert.assertFalse(peerEntryFacade.isConnected());
        Assert.assertEquals(null, peerEntryFacade.getLastSession());
        Assert.assertEquals(null, peerEntryFacade.getLastConnectionAttempt());
        Assert.assertEquals(Management.ConnectionWish.YES, peerEntryFacade.getWishForRegularConnections());
        Assert.assertEquals(0, peerEntryFacade.getAffinity());
        Assert.assertEquals(null, peerEntryFacade.getPeerAddress());

//        peerEntryFacade.setMainLanguage(LanguageCode.es);
        peerEntryFacade.setMainCountry(CountryCode.ES);
        peerEntryFacade.setRelationship(Management.Relationship.FAVORITE);
        peerEntryFacade.setRelationshipToUs(Management.Relationship.BLOCKED);
        peerEntryFacade.setConnected(false);
        peerEntryFacade.setConnected(true);
        peerEntryFacade.updateConnectionAttempt();
        peerEntryFacade.setWishForRegularConnections(Management.ConnectionWish.NOT_NOW);
        peerEntryFacade.setAffinity(23);
        peerEntryFacade.setPeerAddress(new PeerAddress(new IP4Port("205.103.101.94", 32000), new IP4Port("192.168.1.27", 50000)));

        Assert.assertEquals(peerId01, peerEntryFacade.getPeerId());
//        Assert.assertEquals(LanguageCode.es, peerEntryFacade.getMainLanguage());
        Assert.assertEquals(CountryCode.ES, peerEntryFacade.getMainCountry());
        Assert.assertEquals(Management.Relationship.FAVORITE, peerEntryFacade.getRelationship());
        Assert.assertEquals(Management.Relationship.BLOCKED, peerEntryFacade.getRelationshipToUs());
        Assert.assertTrue(peerEntryFacade.isConnected());
        Assert.assertTrue(new Date().getTime() - peerEntryFacade.getLastSession().getTime() < 100);
        Assert.assertTrue((new Date().getTime() - peerEntryFacade.getLastConnectionAttempt().getTime() < 100));
        Assert.assertEquals(Management.ConnectionWish.NOT_NOW, peerEntryFacade.getWishForRegularConnections());
        Assert.assertEquals(23, peerEntryFacade.getAffinity());
        Assert.assertEquals(new IP4Port("205.103.101.94", 32000).toString(), peerEntryFacade.getPeerAddress().getExternalAddress().toString());
        Assert.assertEquals(new IP4Port("192.168.1.27", 50000).toString(), peerEntryFacade.getPeerAddress().getLocalAddress().toString());

        // set up relations with rest of peers
        peerEntryFacade = pkb.getPeerEntryFacade(peerId02);
        peerEntryFacade.setRelationship(Management.Relationship.FAVORITE);
        peerEntryFacade.setAffinity(2);
        peerEntryFacade = pkb.getPeerEntryFacade(peerId03);
        peerEntryFacade.setRelationship(Management.Relationship.FAVORITE);
        peerEntryFacade.setAffinity(5);
        peerEntryFacade.setConnected(true);
        peerEntryFacade = pkb.getPeerEntryFacade(peerId04);
        peerEntryFacade.setRelationship(Management.Relationship.FAVORITE);
        peerEntryFacade.setAffinity(5);
        peerEntryFacade.updateConnectionAttempt();
        peerEntryFacade = pkb.getPeerEntryFacade(peerId05);
        peerEntryFacade.setRelationship(Management.Relationship.FAVORITE);
        peerEntryFacade = pkb.getPeerEntryFacade(peerId06);
        peerEntryFacade.setRelationship(Management.Relationship.REGULAR);
        peerEntryFacade.setAffinity(12);
        peerEntryFacade.setMainCountry(CountryCode.ES);
        peerEntryFacade = pkb.getPeerEntryFacade(peerId07);
        peerEntryFacade.setRelationship(Management.Relationship.BLOCKED);
        peerEntryFacade = pkb.getPeerEntryFacade(peerId08);
        peerEntryFacade.setRelationship(Management.Relationship.REGULAR);
        peerEntryFacade.setAffinity(25);
        peerEntryFacade.setMainCountry(CountryCode.AR);
        peerEntryFacade = pkb.getPeerEntryFacade(peerId09);
        peerEntryFacade.setRelationship(Management.Relationship.FAVORITE);
        peerEntryFacade.setConnected(true);
        peerEntryFacade.setAffinity(15);
        peerEntryFacade = pkb.getPeerEntryFacade(peerId10);
        peerEntryFacade.setRelationship(Management.Relationship.REGULAR);
        peerEntryFacade.setConnected(true);
        peerEntryFacade.setAffinity(20);
//        peerEntryFacade.setMainLanguage(LanguageCode.es);
        peerEntryFacade.setMainCountry(CountryCode.ES);

        assessFavoritePeers(pkb, PeerKnowledgeBase.ConnectedQuery.DISCONNECTED, 4, 2, 5);
        assessFavoritePeers(pkb, PeerKnowledgeBase.ConnectedQuery.CONNECTED, 1, 9, 3);
        assessFavoritePeers(pkb, PeerKnowledgeBase.ConnectedQuery.ALL, 1, 9, 3, 4, 2, 5);

        assessRegularPeers(pkb, PeerKnowledgeBase.ConnectedQuery.DISCONNECTED, 8, 6);
        assessRegularPeersInCountry(pkb, PeerKnowledgeBase.ConnectedQuery.DISCONNECTED, CountryCode.ES, 6);
        assessRegularPeersInCountry(pkb, PeerKnowledgeBase.ConnectedQuery.DISCONNECTED, CountryCode.AR, 8);
        assessRegularPeers(pkb, PeerKnowledgeBase.ConnectedQuery.CONNECTED, 10);
        assessRegularPeersInCountry(pkb, PeerKnowledgeBase.ConnectedQuery.CONNECTED, CountryCode.ES, 10);
        assessRegularPeersInCountry(pkb, PeerKnowledgeBase.ConnectedQuery.CONNECTED, CountryCode.AR);
        assessRegularPeers(pkb, PeerKnowledgeBase.ConnectedQuery.ALL, 8, 10, 6);
        assessRegularPeersInCountry(pkb, PeerKnowledgeBase.ConnectedQuery.ALL, CountryCode.ES, 10, 6);
        assessRegularPeersInCountry(pkb, PeerKnowledgeBase.ConnectedQuery.ALL, CountryCode.AR, 8);
    }

    private void assessFavoritePeers(PeerKnowledgeBase pkb, PeerKnowledgeBase.ConnectedQuery connectedQuery, int... ids) {
        PeerId[] peers = getPeerList(ids);
        List<PeerEntryFacade> retrievedPeers = pkb.getFavoritePeers(connectedQuery);
        Assert.assertEquals(peers.length, retrievedPeers.size());
        for (int i = 0; i < peers.length; i++) {
            Assert.assertEquals(peers[i], retrievedPeers.get(i).getPeerId());
        }
    }

    private void assessRegularPeers(PeerKnowledgeBase pkb, PeerKnowledgeBase.ConnectedQuery connectedQuery, int... ids) {
        PeerId[] peers = getPeerList(ids);
        List<PeerEntryFacade> retrievedPeers = pkb.getRegularPeers(connectedQuery);
        Assert.assertEquals(peers.length, retrievedPeers.size());
        for (int i = 0; i < peers.length; i++) {
            Assert.assertEquals(peers[i], retrievedPeers.get(i).getPeerId());
        }
    }

    private void assessRegularPeersInCountry(PeerKnowledgeBase pkb, PeerKnowledgeBase.ConnectedQuery connectedQuery, CountryCode countryCode, int... ids) {
        PeerId[] peers = getPeerList(ids);
        List<PeerEntryFacade> retrievedPeers = pkb.getRegularPeers(connectedQuery, countryCode);
        Assert.assertEquals(peers.length, retrievedPeers.size());
        for (int i = 0; i < peers.length; i++) {
            Assert.assertEquals(peers[i], retrievedPeers.get(i).getPeerId());
        }
    }

    private PeerId[] getPeerList(int... ids) {
        PeerId[] peerList = new PeerId[ids.length];
        for (int i = 0; i < ids.length; i++) {
            peerList[i] = PeerId.buildTestPeerId("" + ids[i]);
        }
        return peerList;
    }
}
