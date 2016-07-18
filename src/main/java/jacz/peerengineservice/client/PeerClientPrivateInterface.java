package jacz.peerengineservice.client;

import com.neovisionaries.i18n.CountryCode;
import org.aanguita.jtcpserver.channel.ChannelConnectionPoint;
import org.aanguita.jtcpserver.communication.CommError;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.connection.RequestFromPeerToPeer;
import jacz.peerengineservice.client.connection.peers.kb.Management;
import jacz.peerengineservice.util.PeerRelationship;

/**
 * This class handles the messages that the PeerClientConnectionManager send to its corresponding PeerClient. It is
 * done so several important methods in the PeerClient can be made non-public (new client, free channels, etc).
 * <p/>
 * This class contains the code that handles new connections with other clients, as well as disconnections or errors.
 * The freeing of channels also goes through this class, since that event is captured by the
 * PeerClientConnectionManager.
 */
public class PeerClientPrivateInterface {

    /**
     * PeerClient to which this class gives access
     */
    private final PeerClient peerClient;

    /**
     * Class constructor. Non-public so this class cannot be used outside this package
     *
     * @param peerClient the PeerClient for which this PeerClientPrivateInterface works
     */
    PeerClientPrivateInterface(PeerClient peerClient) {
        this.peerClient = peerClient;
    }

    public String getPeerNick(PeerId peerId) {
        return peerClient.getPeerNick(peerId);
    }

    public void newPeerConnected(PeerId peerId, ChannelConnectionPoint ccp, PeerRelationship peerRelationship) {
        peerClient.newPeerConnected(peerId, ccp, peerRelationship);
    }

    /**
     * The PeerClientConnectionManager informs that we have lost connection to a peer (either we disconnected from him, or he disconnected from us)
     *
     * @param peerId peer that was disconnected
     */
    public void peerDisconnected(PeerId peerId, CommError error) {
        peerClient.peerDisconnected(peerId, error);
    }

    public void modifiedPeerRelationship(PeerId peerId, Management.Relationship relationship) {
        peerClient.modifiedPeerRelationship(peerId, relationship);
    }

    public void modifiedMainCountry(CountryCode mainCountry) {
        peerClient.modifiedMainCountry(mainCountry);
    }

    public void newObjectMessageReceived(PeerId peerId, Object message) {
        peerClient.newObjectMessageReceived(peerId, message);
    }

    public void requestServerCustomFSM(RequestFromPeerToPeer requestFromPeerToPeer, String serverFSMName, PeerId peerId, ChannelConnectionPoint ccp, byte outgoingChannel) {
        peerClient.requestServerCustomFSM(requestFromPeerToPeer, serverFSMName, peerId, ccp, outgoingChannel);
    }
}
