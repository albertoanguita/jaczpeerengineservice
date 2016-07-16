package jacz.peerengineservice.client.connection.peers;

import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.connection.PeerAddress;
import org.aanguita.jacuzzi.queues.TimedQueue;

import java.io.Serializable;
import java.util.List;

/**
 * This class stores peers who have recently attempted to connect with us (and might have connected) and are
 * searching for regular connections of our main country
 * <p/>
 * There records are periodically deleted, so they only persist for a limited amount of time
 * <p/>
 * When we reject a connection, we also provide the most recent peers stored here, so the rejected peer can try
 * to connect with those
 */
public class PeersLookingForRegularConnectionsRecord {

    public static class PeerRecord implements Serializable {

        public final byte[] peerId;

        public final PeerAddress peerAddress;

        public PeerRecord(PeerId peerId, PeerAddress peerAddress) {
            this.peerId = peerId.toByteArray();
            this.peerAddress = peerAddress;
        }

        public PeerId getPeerId() {
            return new PeerId(peerId);
        }

        @Override
        public String toString() {
            return "PeerRecord{" +
                    "peerId=" + getPeerId() +
                    ", peerAddress=" + peerAddress +
                    '}';
        }
    }

    /**
     * Peer records are stored for 10 minutes
     */
    private static final long STORAGE_TIME = 1000L * 60L * 10L;

    private CountryCode mainCountry;

    private final TimedQueue<PeerRecord> peerRecordQueue;

    public PeersLookingForRegularConnectionsRecord(CountryCode mainCountry) {
        this.mainCountry = mainCountry;
        peerRecordQueue = new TimedQueue<>(STORAGE_TIME);
    }

    public CountryCode getMainCountry() {
        return mainCountry;
    }

    public void setMainCountry(CountryCode mainCountry) {
        this.mainCountry = mainCountry;
        peerRecordQueue.clear();
    }

    public void addPeer(PeerId peerId, PeerAddress peerAddress) {
        peerRecordQueue.addElement(new PeerRecord(peerId, peerAddress));
    }

    public List<PeerRecord> getRecords(PeerId exceptPeer, int maxCount) {
        peerRecordQueue.clearOldElements();
        List<PeerRecord> peerRecords = peerRecordQueue.peek(maxCount);
        // check if the exceptPeer is present in the retrieved list
        for (int i = 0; i < peerRecords.size(); i++) {
            if (peerRecords.get(i).peerId.equals(exceptPeer)) {
                // exceptPeer found -> remove and require one more
                peerRecords.remove(i);
                peerRecords.addAll(peerRecordQueue.peek(1));
                break;
            }
        }
        return peerRecords;
    }
}
