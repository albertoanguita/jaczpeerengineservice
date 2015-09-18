package jacz.peerengineservice.client.connection;

import jacz.commengine.channel.ChannelAction;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.communication.CommError;

import java.util.Set;

/**
 * ChannelAction employed by the PeerClientConnectionManager when connecting to a PeerServer. No messages should be
 * received at this class, as all communication is handled with FSMs. It is useful to notify the
 * PeerClientConnectionManager in case of an error or being stopped.
 * <p/>
 * At this stage, the freeing of channels is ignored, as the communications with the peer server are very simple.
 * In the future, this will be handled.
 */
public class PeerClientConnectionToServerChannelActionImpl implements ChannelAction {

    private final PeerServerManager peerServerManager;

    public PeerClientConnectionToServerChannelActionImpl(PeerServerManager peerServerManager) {
        this.peerServerManager = peerServerManager;
    }

    @Override
    public void newMessage(ChannelConnectionPoint ccp, byte b, Object message) {
        // ignore
    }

    @Override
    public void newMessage(ChannelConnectionPoint ccp, byte channel, byte[] data) {
        // ignore
    }

    @Override
    public void channelsFreed(ChannelConnectionPoint ccp, Set<Byte> channels) {
        peerServerManager.channelsFreed(channels);
    }

    @Override
    public void disconnected(ChannelConnectionPoint ccp, boolean expected) {
        if (!expected) {
            // only notify non-expected disconnections
            peerServerManager.disconnectedFromServer(expected, ccp);
        }
    }

    @Override
    public void error(ChannelConnectionPoint ccp, CommError e) {
        peerServerManager.disconnectedFromServer(false, ccp);
    }
}
