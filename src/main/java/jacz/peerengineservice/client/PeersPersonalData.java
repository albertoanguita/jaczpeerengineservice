package jacz.peerengineservice.client;

import jacz.peerengineservice.PeerId;
import org.aanguita.jacuzzi.io.serialization.localstorage.Updater;
import org.aanguita.jacuzzi.io.serialization.localstorage.VersionedLocalStorage;

import java.io.IOException;

/**
 * This class stores our and others' nicks
 */
public class PeersPersonalData implements Updater {

    private static final String VERSION_0_1_0 = "0.1.0";

    private static final String CURRENT_VERSION = VERSION_0_1_0;

    private static final String DEFAULT_NICK = "defaultNick";

    private static final String OWN_NICK = "ownNick";

    private static final String PEER_NICKS = "peerNicks.";

    private final VersionedLocalStorage localStorage;

    public PeersPersonalData(String localStoragePath, String defaultNick, String ownNick) throws IOException {
        localStorage = VersionedLocalStorage.createNew(localStoragePath, CURRENT_VERSION);
        localStorage.setString(DEFAULT_NICK, defaultNick);
        localStorage.setString(OWN_NICK, ownNick);
    }

    public PeersPersonalData(String localStoragePath) throws IOException {
        localStorage = new VersionedLocalStorage(localStoragePath, this, CURRENT_VERSION);
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

    @Override
    public String update(VersionedLocalStorage versionedLocalStorage, String storedVersion) {
        // no versions yet, cannot be invoked
        return null;
    }
}
