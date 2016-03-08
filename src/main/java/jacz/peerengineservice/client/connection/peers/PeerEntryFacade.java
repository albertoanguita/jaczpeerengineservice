package jacz.peerengineservice.client.connection.peers;

import org.javalite.activejdbc.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alberto on 02/03/2016.
 */
public class PeerEntryFacade {

    private final PeerEntry peerEntry;

    PeerEntryFacade(Model peerEntry) {
        this.peerEntry = (PeerEntry) peerEntry;
    }

    static List<PeerEntryFacade> buildList(List<? extends Model> peerEntries) {
        List<PeerEntryFacade> peerEntryFacades = new ArrayList<>();
        for (Model peerEntry : peerEntries) {
            if (peerEntry != null) {
                peerEntryFacades.add(new PeerEntryFacade(peerEntry));
            }
        }
        return peerEntryFacades;
    }
}
