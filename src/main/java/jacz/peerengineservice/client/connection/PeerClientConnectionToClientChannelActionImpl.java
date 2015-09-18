package jacz.peerengineservice.client.connection;

import jacz.commengine.channel.ChannelAction;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.communication.CommError;

import java.util.Set;

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
    FriendConnectionManager friendConnectionManager;

    /**
     * Class constructor
     *
     * @param peerClientConnectionManager the PeerClientConnectionManager that created this object
     */
    public PeerClientConnectionToClientChannelActionImpl(FriendConnectionManager peerClientConnectionManager) {
        this.friendConnectionManager = peerClientConnectionManager;
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
    public void channelsFreed(ChannelConnectionPoint ccp, Set<Byte> channels) {
        friendConnectionManager.channelsFreed(ccp, channels);
    }

    @Override
    public void disconnected(ChannelConnectionPoint ccp, boolean expected) {
        friendConnectionManager.peerDisconnected(ccp);
    }

    @Override
    public void error(ChannelConnectionPoint ccp, CommError e) {
        friendConnectionManager.peerError(ccp, e);
    }
}
