package jacz.peerengineservice.client;

import jacz.peerengineservice.PeerId;

import java.util.HashMap;
import java.util.Map;

/**
 * This class stores our and others' nicks
 */
public class PeersPersonalData {

    private final String defaultNick;

    private String ownNick;

    private Map<PeerId, String> peersNicks;

    public PeersPersonalData(String defaultNick, String ownNick) {
        this(defaultNick, ownNick, new HashMap<PeerId, String>());
    }

    public PeersPersonalData(String defaultNick, String ownNick, Map<PeerId, String> peersNicks) {
        this.defaultNick = defaultNick != null ? defaultNick : "";
        this.ownNick = checkNick(ownNick);
        this.peersNicks = new HashMap<>();
        for (Map.Entry<PeerId, String> peerAndNick : peersNicks.entrySet()) {
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

    public synchronized String getPeerNick(PeerId peerId) {
        return peersNicks.get(peerId);
    }

    public synchronized boolean setPeersNicks(PeerId peerId, String newNick) {
        newNick = checkNick(newNick);
        boolean hasChanged = !peersNicks.containsKey(peerId) || !peersNicks.get(peerId).equals(newNick);
        peersNicks.put(peerId, newNick);
        return hasChanged;
    }
}
