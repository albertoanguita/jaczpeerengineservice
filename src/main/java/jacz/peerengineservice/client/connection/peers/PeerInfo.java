package jacz.peerengineservice.client.connection.peers;

import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.connection.peers.kb.PeerEntryFacade;
import jacz.peerengineservice.util.PeerRelationship;

import java.util.Date;

/**
 * Information summary about a peer
 */
public class PeerInfo {

    private final PeerId peerId;

    private final boolean connected;

    private final PeerRelationship relationship;

    private final boolean wishRegularConnections;

    private final CountryCode mainCountry;

    private final int affinity;

    private final Date lastConnectionDate;

    private final Date lastRelationshipChangeDate;

    private final String nick;

    PeerInfo(PeerEntryFacade peerEntryFacade, String nick) {
        this.peerId = peerEntryFacade.getPeerId();
        this.connected = peerEntryFacade.isConnected();
        this.relationship = PeerConnectionManager.getPeerRelationship(peerEntryFacade);
        this.wishRegularConnections = peerEntryFacade.isWishForRegularConnections();
        this.mainCountry = peerEntryFacade.getMainCountry();
        this.affinity = peerEntryFacade.getAffinity();
        this.lastConnectionDate = peerEntryFacade.getLastSession();
        this.lastRelationshipChangeDate = peerEntryFacade.getLastRelationshipChange();
        this.nick = nick;
    }

    public PeerId getPeerId() {
        return peerId;
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

    public Date getLastConnectionDate() {
        return lastConnectionDate;
    }

    public Date getLastRelationshipChangeDate() {
        return lastRelationshipChangeDate;
    }

    @Override
    public String toString() {
        return "PeerInfo{" +
                "peerId=" + peerId +
                ", connected=" + connected +
                ", relationship=" + relationship +
                ", wishRegularConnections=" + wishRegularConnections +
                ", mainCountry=" + mainCountry +
                ", affinity=" + affinity +
                ", lastConnectionDate=" + lastConnectionDate +
                ", lastRelationshipChangeDate=" + lastRelationshipChangeDate +
                '}';
    }
}
