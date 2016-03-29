package jacz.peerengineservice.client.connection.peers;


import jacz.commengine.channel.ChannelAction;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.communication.CommError;
import jacz.peerengineservice.client.connection.FriendConnectionManager;

/**
 * This class implements the actions used by the Client module when connecting to another peer. Direct messages are
 * ignored, as all communications will be made through FSMs. As objects of this class will handle every event related to
 * ChannelConnectionPoints used with peers to which we are connected, the freeing of channels will come through this
 * class. This is notified to the PeerClient.
 */
public class PeerClientConnectionToClientChannelActionImpl implements ChannelAction {

    /**
     * The FriendConnectionManager that created this object to connect to another peer
     */
   PeerConnectionManager peerConnectionManager;

    /**
     * Class constructor
     *
     * @param peerConnectionManager the peerConnectionManager that created this object
     */
    public PeerClientConnectionToClientChannelActionImpl(PeerConnectionManager peerConnectionManager) {
        this.peerConnectionManager = peerConnectionManager;
    }

    @Override
    public void newMessage(ChannelConnectionPoint channelConnectionPoint, byte b, Object o) {
        // ignore
    }

    @Override
    public void newMessage(ChannelConnectionPoint channelConnectionPoint, byte b, byte[] bytes) {
        // ignore
    }

    @Override
    public void channelFreed(ChannelConnectionPoint ccp, byte channel) {
        peerConnectionManager.channelFreed(ccp, channel);
    }

    @Override
    public void disconnected(ChannelConnectionPoint ccp, boolean expected) {
        peerConnectionManager.peerDisconnected(ccp);
    }

    @Override
    public void error(ChannelConnectionPoint ccp, CommError e) {
        peerConnectionManager.peerError(ccp, e);
    }
}
