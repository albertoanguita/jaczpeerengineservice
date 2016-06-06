package jacz.peerengineservice.client.connection.peers;

import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineservice.client.connection.peers.kb.PeerEntryFacade;
import jacz.peerengineservice.util.PeerRelationship;

/**
 * Information summary about a peer
 *
 * todo add more info, including id
 */
public class PeerInfo {

    private final boolean connected;

    private final PeerRelationship relationship;

    private final boolean wishRegularConnections;

    private final CountryCode mainCountry;

    private final int affinity;

    PeerInfo(PeerEntryFacade peerEntryFacade) {
        this.connected = peerEntryFacade.isConnected();
        this.relationship = PeerConnectionManager.getPeerRelationship(peerEntryFacade);
        this.wishRegularConnections = peerEntryFacade.isWishForRegularConnections();
        this.mainCountry = peerEntryFacade.getMainCountry();
        this.affinity = peerEntryFacade.getAffinity();
    }

    public boolean isConnected() {
        return connected;
    }

    public PeerRelationship getRelationship() {
        return relationship;
    }

    public boolean isWishRegularConnections() {
        return wishRegularConnections;
    }

    public CountryCode getMainCountry() {
        return mainCountry;
    }

    public int getAffinity() {
        return affinity;
    }

    @Override
    public String toString() {
        return "PeerInfo{" +
                "connected=" + connected +
                ", relationship=" + relationship +
                ", wishRegularConnections=" + wishRegularConnections +
                ", mainCountry=" + mainCountry +
                ", affinity=" + affinity +
                '}';
    }
}
