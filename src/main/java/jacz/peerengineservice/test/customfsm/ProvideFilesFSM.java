package jacz.peerengineservice.test.customfsm;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.util.io.object_serialization.ObjectListWrapper;
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

    public ProvideFilesFSM() {
    }

    @Override
    public State processMessage(State state, byte b, Object o, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        switch (state) {

            case WAITING_FOR_REQUEST:
                // provide file hashes
                System.out.println("ProvideFilesFSM -> received request: " + o.toString());
                ObjectListWrapper objectListWrapper = new ObjectListWrapper();
                objectListWrapper.getObjects().add("aaa");
                objectListWrapper.getObjects().add("bbb");
                objectListWrapper.getObjects().add("ccc");
                objectListWrapper.getObjects().add("ddd");
                ccp.write(outgoingChannel, objectListWrapper);
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

    //@Override

    public void timedOut(State state) {
        System.out.println("Provide files FSM timed out!!!");
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
