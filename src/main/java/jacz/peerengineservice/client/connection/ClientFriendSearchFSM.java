package jacz.peerengineservice.client.connection;

import jacz.peerengineservice.server.RequestFromPeerToServer;
import jacz.util.io.object_serialization.ObjectListWrapper;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.channel.TimedChannelFSMAction;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.server.PeerConnectionInfo;
import jacz.peerengineservice.server.PeerServer;

import java.util.Collection;
import java.util.Map;

/**
 * FSM for searching for connected friends in the peer server.
 */
public class ClientFriendSearchFSM implements TimedChannelFSMAction<ClientFriendSearchFSM.State> {

    enum State {
        // Initial state: we have sent the IDs of the peers we are searching to the peer server
        DATA_SENT,
        // the server answered with the list of found ids
        SUCCESS,
        // there was an error during the conversation with the peer server
        ERROR
    }

    /**
     * The PeerClientConnectionManager that created this FSM and that wants to connect to some friends
     */
    private FriendConnectionManager friendConnectionManager;

    /**
     * Channel at which this FSM will be listening from data from the peer server
     */
    private byte channel;

    /**
     * The IDs of the friends that we must ask for to the peer server
     */
    private Collection<PeerID> friendPeers;

    /**
     * Class constructor
     *
     * @param friendConnectionManager the FriendConnectionManager that created this FSM and that wants to
     *                                connect to some friends
     * @param channel                 channel at which this FSM will be listening from data from the peer server
     * @param friendPeers             the IDs of the friends that we must ask for to the peer server
     */
    public ClientFriendSearchFSM(FriendConnectionManager friendConnectionManager, byte channel, Collection<PeerID> friendPeers) {
        this.friendConnectionManager = friendConnectionManager;
        this.channel = channel;
        this.friendPeers = friendPeers;
    }

    @Override
    public State processMessage(State state, byte canal, Object message, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        // the only valid state is DATA_SENT
        if (state != State.DATA_SENT) {
            throw new IllegalArgumentException();
        }
        // we expect and ObjectListWrapper containing the list of found friends. This list is submitted to our
        // PeerClientConnectionManager so he starts trying to connect to them
        if (message instanceof ObjectListWrapper) {
            ObjectListWrapper receivedData = (ObjectListWrapper) message;
            if (receivedData.getObjects().get(0) instanceof Map<?, ?>) {
                @SuppressWarnings
                        ("unchecked") Map<PeerID, PeerConnectionInfo> friendsConnectionData = (Map<PeerID, PeerConnectionInfo>) receivedData.getObjects().get(0);
                friendConnectionManager.reportConnectedFriendsData(friendsConnectionData);
                return State.SUCCESS;
            } else {
                return State.ERROR;
            }
        } else {
            // wrong data received
            return State.ERROR;
        }
    }

    @Override
    public State processMessage(State state, byte canal, byte[] data, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        // unexpected data received
        return State.ERROR;
    }

    @Override
    public State init(ChannelConnectionPoint ccp) {
        // send friends ids to the peer server in a RequestFromPeerToServer object
        RequestFromPeerToServer requestFromPeerToServer = RequestFromPeerToServer.friendSearchRequest(channel, friendPeers);
        ccp.write(PeerServer.REQUESTS_CHANNEL, requestFromPeerToServer);

        /*ObjectListWrapper personalData = new ObjectListWrapper(channel);
       for (PeerID peerID : friendPeers) {
           personalData.getObjects().add(peerID);
       }
       ccp.write(PeerServer.FRIEND_SEARCH_CHANNEL, personalData);*/
        return State.DATA_SENT;
    }

    @Override
    public boolean isFinalState(State state, ChannelConnectionPoint ccp) {
        switch (state) {

            case SUCCESS:
                return true;
            case ERROR:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void disconnected(ChannelConnectionPoint ccp) {
        // ignore
    }

    @Override
    public void timedOut(State state, ChannelConnectionPoint ccp) {
        friendConnectionManager.friendDataRequestToServerTimedOut();
    }
}
