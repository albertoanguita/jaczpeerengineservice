package jacz.peerengineservice.client;

import jacz.peerengineservice.PeerID;

import java.util.HashMap;
import java.util.Map;

/**
 * This class stores our and others' nicks
 */
public class PeersPersonalData {

    private final String defaultNick;

    private String ownNick;

    private Map<PeerID, String> peersNicks;

    public PeersPersonalData(String defaultNick, String ownNick) {
        this(defaultNick, ownNick, new HashMap<PeerID, String>());
    }

    public PeersPersonalData(String defaultNick, String ownNick, Map<PeerID, String> peersNicks) {
        this.defaultNick = defaultNick != null ? defaultNick : "";
        this.ownNick = checkNick(ownNick);
        this.peersNicks = new HashMap<>();
        for (Map.Entry<PeerID, String> peerAndNick : peersNicks.entrySet()) {
            this.peersNicks.put(peerAndNick.getKey(), checkNick(peerAndNick.getValue()));
        }
    }

    private String checkNick(String nick) {
        return nick != null ? nick : defaultNick;
    }

    public synchronized String getOwnNick() {
        return ownNick;
    }

    public synchronized boolean setOwnNick(String newNick) {
        newNick = checkNick(newNick);
        boolean hasChanged = !ownNick.equals(newNick);
        ownNick = newNick;
        return hasChanged;
    }

    public synchronized String getPeerNick(PeerID peerID) {
        return peersNicks.get(peerID);
    }

    public synchronized boolean setPeersNicks(PeerID peerID, String newNick) {
        newNick = checkNick(newNick);
        boolean hasChanged = !peersNicks.containsKey(peerID) || !peersNicks.get(peerID).equals(newNick);
        peersNicks.put(peerID, newNick);
        return hasChanged;
    }
}
