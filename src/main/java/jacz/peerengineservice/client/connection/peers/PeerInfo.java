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

    public final PeerId peerId;

    public final boolean connected;

    public final PeerRelationship relationship;

    public final CountryCode mainCountry;

    public final int affinity;

    public final Date lastConnectionDate;

    public final Date lastRelationshipChangeDate;

    public final String nick;

    PeerInfo(PeerEntryFacade peerEntryFacade, String nick) {
        this.peerId = peerEntryFacade.getPeerId();
        this.connected = peerEntryFacade.isConnected();
        this.relationship = PeerConnectionManager.getPeerRelationship(peerEntryFacade);
        this.mainCountry = peerEntryFacade.getMainCountry();
        this.affinity = peerEntryFacade.getAffinity();
        this.lastConnectionDate = peerEntryFacade.getLastSession();
        this.lastRelationshipChangeDate = peerEntryFacade.getLastRelationshipChange();
        this.nick = nick;
    }

    @Override
    public String toString() {
        return "PeerInfo{" +
                "peerId=" + peerId +
                ", connected=" + connected +
                ", relationship=" + relationship +
                ", mainCountry=" + mainCountry +
                ", affinity=" + affinity +
                ", lastConnectionDate=" + lastConnectionDate +
                ", lastRelationshipChangeDate=" + lastRelationshipChangeDate +
                ", nick=" + nick +
                '}';
    }
}
