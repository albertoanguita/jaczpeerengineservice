package jacz.peerengineservice.client;

import jacz.commengine.channel.ChannelFSMAction;

/**
 * Actions that need to be implemented in order to use custom FSMs in the peer engine. They are similar to channel
 * FSMs, but with a couple additional methods.
 * <p/>
 * One additional method allows setting the outgoing channel (so a pair of FSMs can communicate between them). This
 * will always be invoked before the init method is called (for both client and server FSMs).
 * The other is invoked when a custom FSM could not be requested on the server peer.
 * <p/>
 * Custom peer FSMs work like normal FSMs, but have a first stage of sending a request to the
 * RequestDispatcher of the other end (the FSM acting as client does) and waiting for a response. Upon positive
 * response, the client receives which channel he must send subsequent messages to. Upon negative response, the
 * errorRequestingFSM method is invoked, and the FSM is terminated
 * <p/>
 * The setOutgoingChannel method is guaranteed to be invoked always before the init method implemented by the client.
 */
public interface PeerFSMAction<T> extends ChannelFSMAction<T> {

    /**
     * This method gives the outgoing channel to use for the rest of the communication process to send messages to the
     * other FSM end, no matter if the FSM is client or server. This method is guaranteed to be invoked before
     * the init method
     *
     * @param channel channel assigned for outgoing communications
     */
    void setOutgoingChannel(byte channel);

    /**
     * This method is invoked when the RequestDispatcher at the other end denied our request of setting up a custom
     * FSM (either no channels available, or task unrecognized)
     * <p/>
     * It is only invoked at a client FSM. The server FSM does not need to implement it
     *
     * @param serverResponse cause of the error in the request
     */
    void errorRequestingFSM(PeerFSMServerResponse serverResponse);

}
