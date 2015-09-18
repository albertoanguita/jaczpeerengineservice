package jacz.peerengineservice.client;

import jacz.peerengineservice.PeerID;

import java.util.HashSet;
import java.util.Set;

/**
 * This class stores which peers are our friends and which peers are blocked (are not allowed to connect to us). The peer client handles this class
 * upon client's requests.
 * <p/>
 * All services that require to know which peers are our friends and which are blocked (such as the peer connection manager) read the info from here,
 * as these data will not be replicated elsewhere.
 * <p/>
 * This class handles concurrency, in case several writes are performed simultaneously
 * <p/>
 * This class stores two sets: friend peers and blocked peers. Friends are allowed to connect to us and gain access to all services (chat,
 * transfer...). Blocked peers never connect to us.
 * <p/>
 * If a peer which is not in any of the previous lists tries to connect to us, he will be allowed to connect but will have restricted access
 * (only chat and personal info). These peers are referred to as "NonRegistered" peers
 * <p/>
 * The methods to modify the lists of peers (add, remove) will always ensure that a peer cannot be in both lists at the same time
 */
public final class PeerRelations {

    /**
     * Set of peers which are friend of us. The connection manager will actively try to connect to these, and they are allowed to connect to us and
     * gain access to all services
     */
    private Set<PeerID> friendPeers;

    /**
     * Set of blocked peers. The connection manager will never try to connect to these, and they will never be allowed to connect to us.
     */
    private Set<PeerID> blockedPeers;

    public PeerRelations(Set<PeerID> friendPeers, Set<PeerID> blockedPeers) {
        this.friendPeers = friendPeers;
        this.blockedPeers = blockedPeers;
    }

    public PeerRelations() {
        this(new HashSet<PeerID>(), new HashSet<PeerID>());
    }

    public synchronized boolean isFriendPeer(PeerID peerID) {
        return friendPeers.contains(peerID);
    }

    public synchronized boolean isBlockedPeer(PeerID peerID) {
        return blockedPeers.contains(peerID);
    }

    public synchronized boolean isNonRegisteredPeer(PeerID peerID) {
        return !isFriendPeer(peerID) && !isBlockedPeer(peerID);
    }

    public synchronized Set<PeerID> getFriendPeers() {
        return new HashSet<>(friendPeers);
    }

    public synchronized void addFriendPeer(PeerID peerID) {
        if (blockedPeers.contains(peerID)) {
            blockedPeers.remove(peerID);
        }
        friendPeers.add(peerID);
    }

    public synchronized void removeFriendPeer(PeerID peerID) {
        friendPeers.remove(peerID);
    }

    public synchronized Set<PeerID> getBlockedPeers() {
        return new HashSet<>(blockedPeers);
    }

    public synchronized void addBlockedPeer(PeerID peerID) {
        if (friendPeers.contains(peerID)) {
            friendPeers.remove(peerID);
        }
        blockedPeers.add(peerID);
    }

    public synchronized void removeBlockedPeer(PeerID peerID) {
        blockedPeers.remove(peerID);
    }
}
