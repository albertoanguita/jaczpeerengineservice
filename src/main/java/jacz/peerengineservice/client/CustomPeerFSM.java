package jacz.peerengineservice.client;

import jacz.peerengineservice.client.connection.RequestFromPeerToPeer;
import jacz.peerengineservice.util.ChannelConstants;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.channel.ChannelFSMAction;
import jacz.util.io.serialization.ObjectListWrapper;

/**
 * A custom FSM for dynamic set up in the peer engine. The client must simply implement the PeerFSMAction interface,
 * and the PeerClient class will be able to generate FSM that communicate with connected peers. This class implements
 * such FSMs
 */
public class CustomPeerFSM<T> implements ChannelFSMAction<T> {

    /**
     * Type of the PeerFSM (either acts as client, initiating the communication, or as server, answering to a
     * client FSM)
     */
    private enum Type {
        CLIENT,
        SERVER
    }

    /**
     * Actions that determine the behaviour of this FSM (provided by the peer engine client)
     */
    private PeerFSMAction<T> internalFSM;

    /**
     * Name of the custom server FSM to ask to the RequestDispatcher
     */
    String serverFSMName;

    /**
     * Assigned incoming channel
     */
    private byte assignedChannel;

    /**
     * Indicates if this custom FSM is in a initializing state. In this stage, the FSM still does not know the
     * outgoing channel, so it is waiting for that data to be received (only used for client FSMs, for waiting for
     * the PeerRequestDispatcherFSM response)
     */
    private boolean initializing;

    /**
     * Indicates if this custom FSM has terminated its execution, no matter what is its internal state
     * (used when the PeerRequestDispatcherFSM denied the request, to allow finalising a client FSM)
     */
    private boolean finished;

    /**
     * Assigned outgoing channel for sending messages to the other end. This value is only known at construction time
     * in the case of server FSMs. Client FSMs must wait for the PeerRequestDispatcherFSM response to assign this value
     */
    private byte outgoingChannel;

    /**
     * Type of the custom FSM. They can be clients (the initiate communication by sending a request to the
     * PeerRequestDispatcherFSM) or servers (the PeerRequestDispatcherFSM creates them as an answer to a request from
     * a client FSM
     */
    private Type type;

    // client

    public CustomPeerFSM(PeerFSMAction<T> internalFSM, String serverFSMName, byte assignedChannel) {
        // clients initializing state is set to true, so the first message that they process is always targeted
        // at setting the outgoing channel
        this.internalFSM = internalFSM;
        this.serverFSMName = serverFSMName;
        this.assignedChannel = assignedChannel;
        initializing = true;
        finished = false;
        type = Type.CLIENT;
    }

    // server

    public CustomPeerFSM(PeerFSMAction<T> internalFSM, byte assignedChannel, byte outgoingChannel) {
        this.internalFSM = internalFSM;
        this.assignedChannel = assignedChannel;
        initializing = false;
        finished = false;
        this.outgoingChannel = outgoingChannel;
        internalFSM.setOutgoingChannel(outgoingChannel);
        type = Type.SERVER;
    }

    @Override
    public T processMessage(T state, byte channel, Object message, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        // the first time, the message will come from the PeerRequestDispatcherFSM at the other end, giving us out
        // outgoing channel -> assign it to the internal FSM and start processing normally
        try {
            if (initializing) {
                initializing = false;
                assert message instanceof ObjectListWrapper;
                ObjectListWrapper objectListWrapper = (ObjectListWrapper) message;
                assert objectListWrapper.getObjects().size() == 2;
                assert objectListWrapper.getObjects().get(0) instanceof PeerFSMServerResponse;
                assert objectListWrapper.getObjects().get(1) instanceof Byte;
                PeerFSMServerResponse serverResponse = (PeerFSMServerResponse) objectListWrapper.getObjects().get(0);
                Byte assignedOutgoingChannel = (Byte) objectListWrapper.getObjects().get(1);
                if (serverResponse == PeerFSMServerResponse.REQUEST_GRANTED) {
                    internalFSM.setOutgoingChannel(assignedOutgoingChannel);
                    return internalFSM.init(ccp);
                } else {
                    internalFSM.errorRequestingFSM(serverResponse);
                    finished = true;
                    return null;
                }
            } else {
                return internalFSM.processMessage(state, channel, message, ccp);
            }
        } catch (AssertionError e) {
            // any part of the message was not correctly recognized -> wrong data format
            internalFSM.errorRequestingFSM(PeerFSMServerResponse.UNEXPECTED_SERVER_RESPONSE);
            finished = true;
            return null;
        }
    }

    @Override
    public T processMessage(T state, byte channel, byte[] data, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        return internalFSM.processMessage(state, channel, data, ccp);
    }

    @Override
    /**
     * Do not perform the init method yet, wait until the server has answered with an outgoing channel (for client)
     * For server, behave normally
     */
    public T init(ChannelConnectionPoint ccp) {
        // the client's initial action consists on submitting a request to the RequestDispatcher of the other end
        if (type == Type.CLIENT) {
            RequestFromPeerToPeer requestFromPeerToPeer = RequestFromPeerToPeer.generateCustomRequest(assignedChannel, serverFSMName);
            ccp.write(ChannelConstants.REQUEST_DISPATCHER_CHANNEL, requestFromPeerToPeer);
            return null;
        } else {
            ccp.write(outgoingChannel, new ObjectListWrapper(PeerFSMServerResponse.REQUEST_GRANTED, assignedChannel));
            return internalFSM.init(ccp);
        }
    }

    @Override
    public boolean isFinalState(T state, ChannelConnectionPoint ccp) {
        // if this FSM is initializing (expecting the outgoing channel) always return false (plus, the state is null)
        if (initializing) {
            return false;
        }
        // if marked finished because no channel was assigned, terminate this FSM
        //noinspection SimplifiableIfStatement
        if (finished) {
            return true;
        }
        return internalFSM.isFinalState(state, ccp);
    }

    @Override
    public void disconnected(ChannelConnectionPoint ccp) {
        internalFSM.disconnected(ccp);
    }
}
