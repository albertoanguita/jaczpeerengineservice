package jacz.peerengineservice.client;

import jacz.peerengineservice.PeerId;

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
 *
 * todo remove
 */
public final class PeerRelations {

    /**
     * Set of peers which are friend of us. The connection manager will actively try to connect to these, and they are allowed to connect to us and
     * gain access to all services
     */
    private Set<PeerId> friendPeers;

    /**
     * Set of blocked peers. The connection manager will never try to connect to these, and they will never be allowed to connect to us.
     */
    private Set<PeerId> blockedPeers;

    public PeerRelations(Set<PeerId> friendPeers, Set<PeerId> blockedPeers) {
        this.friendPeers = friendPeers;
        this.blockedPeers = blockedPeers;
    }

    public PeerRelations() {
        this(new HashSet<PeerId>(), new HashSet<PeerId>());
    }

    public synchronized boolean isFriendPeer(PeerId peerId) {
        return friendPeers.contains(peerId);
    }

    public synchronized boolean isBlockedPeer(PeerId peerId) {
        return blockedPeers.contains(peerId);
    }

    public synchronized boolean isNonRegisteredPeer(PeerId peerId) {
        return !isFriendPeer(peerId) && !isBlockedPeer(peerId);
    }

    public synchronized Set<PeerId> getFriendPeers() {
        return new HashSet<>(friendPeers);
    }

    public synchronized void addFriendPeer(PeerId peerId) {
        if (blockedPeers.contains(peerId)) {
            blockedPeers.remove(peerId);
        }
        friendPeers.add(peerId);
    }

    public synchronized void removeFriendPeer(PeerId peerId) {
        friendPeers.remove(peerId);
    }

    public synchronized Set<PeerId> getBlockedPeers() {
        return new HashSet<>(blockedPeers);
    }

    public synchronized void addBlockedPeer(PeerId peerId) {
        if (friendPeers.contains(peerId)) {
            friendPeers.remove(peerId);
        }
        blockedPeers.add(peerId);
    }

    public synchronized void removeBlockedPeer(PeerId peerId) {
        blockedPeers.remove(peerId);
    }
}
