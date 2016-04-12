package jacz.peerengineservice.client;

import jacz.peerengineservice.PeerId;
import jacz.util.io.serialization.localstorage.LocalStorage;

import java.io.IOException;

/**
 * This class stores our and others' nicks
 */
public class PeersPersonalData {

    private static final String DEFAULT_NICK = "defaultNick";

    private static final String OWN_NICK = "ownNick";

    private static final String PEER_NICKS = "peerNicks.";

    private final LocalStorage localStorage;

    public PeersPersonalData(String localStoragePath, String defaultNick, String ownNick) throws IOException {
        localStorage = LocalStorage.createNew(localStoragePath);
        localStorage.setString(DEFAULT_NICK, defaultNick);
        localStorage.setString(OWN_NICK, ownNick);
    }

    public PeersPersonalData(String localStoragePathk) throws IOException {
        localStorage = new LocalStorage(localStoragePathk);
    }

    private String checkNick(String nick) {
        return nick != null ? nick : localStorage.getString(DEFAULT_NICK);
    }

    public synchronized String getOwnNick() {
        return localStorage.getString(OWN_NICK);
    }

    public synchronized boolean setOwnNick(String newNick) {
        newNick = checkNick(newNick);
        return localStorage.setString(OWN_NICK, newNick);
    }

    public synchronized String getPeerNick(PeerId peerId) {
        return localStorage.getString(PEER_NICKS + peerId.toString());
    }

    public synchronized boolean setPeerNick(PeerId peerId, String newNick) {
        newNick = checkNick(newNick);
        return localStorage.setString(PEER_NICKS + peerId.toString(), newNick);
    }
}
