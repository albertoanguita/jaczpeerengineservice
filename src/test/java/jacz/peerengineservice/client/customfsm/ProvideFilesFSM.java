package jacz.peerengineservice.client.customfsm;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.peerengineservice.client.PeerFSMServerResponse;
import jacz.peerengineservice.client.PeerTimedFSMAction;

/**
 * Simple peer timed action example
 */
public class ProvideFilesFSM implements PeerTimedFSMAction<ProvideFilesFSM.State> {

    public enum State {
        WAITING_FOR_REQUEST,
        SUCCESS,
        ERROR
    }

    public static final String SERVER_FSM = "ProvideFilesFSM";

    private byte outgoingChannel;

    private boolean success;

    public ProvideFilesFSM() {
        success = false;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public State processMessage(State state, byte b, Object o, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        switch (state) {

            case WAITING_FOR_REQUEST:
                // provide file hashes
                System.out.println("ProvideFilesFSM -> received request: " + o.toString());

                ccp.write(outgoingChannel, true);

                byte[] data = new byte[1];
                data[0] = 5;
                System.out.println("sending data of length: " + data.length);
                ccp.write(outgoingChannel, data);
                return State.SUCCESS;
        }
        return State.ERROR;
    }

    @Override
    public State processMessage(State state, byte b, byte[] bytes, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        return State.ERROR;
    }

    @Override
    public State init(ChannelConnectionPoint ccp) {
        return State.WAITING_FOR_REQUEST;
    }

    @Override
    public boolean isFinalState(State state, ChannelConnectionPoint ccp) {
        switch (state) {
            case SUCCESS:
                System.out.println("ProvideFiles: success");
                success = true;
                return true;
            case ERROR:
                System.out.println("ProvideFiles: error");
                return true;
        }
        return false;
    }

    @Override
    public void disconnected(ChannelConnectionPoint ccp) {
        // ignore
    }

    @Override
    public void raisedUnhandledException(Exception e, ChannelConnectionPoint ccp) {
        e.printStackTrace();
    }

    //@Override

    public void timedOut(State state) {
        System.out.println("Provide files FSM timed out!!!");
    }

    @Override
    public void setID(String id) {
        System.out.println("Provide files FSM id: " + id);
    }

    @Override
    public void setOutgoingChannel(byte channel) {
        outgoingChannel = channel;
    }

    @Override
    public void errorRequestingFSM(PeerFSMServerResponse serverResponse) {
        // ignore
    }
}
