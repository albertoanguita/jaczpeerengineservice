package jacz.peerengineservice.client.customfsm;

import org.aanguita.jtcpserver.channel.ChannelConnectionPoint;
import jacz.peerengineservice.client.PeerFSMServerResponse;
import jacz.peerengineservice.client.PeerTimedFSMAction;

import java.util.Arrays;

/**
 * Simple peer timed action example
 */
public class AskFilesFSM implements PeerTimedFSMAction<AskFilesFSM.State> {

    enum State {
        REQUEST_SENT,
        REQUEST_SENT_2,
        SUCCESS,
        ERROR
    }

    private byte outgoingChannel;

    private boolean success;

    public AskFilesFSM() {
        success = false;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public State processMessage(State state, byte channel, Object message, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        switch (state) {

            case REQUEST_SENT:
                System.out.println(message);
                return State.REQUEST_SENT_2;
        }
        return State.ERROR;
    }

    @Override
    public State processMessage(State state, byte channel, byte[] data, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        switch (state) {

            case REQUEST_SENT_2:
                System.out.println(Arrays.toString(data));
                return State.SUCCESS;
        }
        return State.ERROR;
    }

    @Override
    public State init(ChannelConnectionPoint ccp) {
        ccp.write(outgoingChannel, "I request your file hashes!");
        return State.REQUEST_SENT;
    }

    @Override
    public boolean isFinalState(State state, ChannelConnectionPoint ccp) {
        switch (state) {

            case SUCCESS:
                System.out.println("AskFiles: success");
                success = true;
                return true;
            case ERROR:
                System.out.println("AskFiles: error");
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

    @Override
    public void timedOut(State state) {
        System.out.println("Ask files FSM timed out!!!");
    }

    @Override
    public void setID(String id) {
        System.out.println("Ask file FSM id: " + id);
    }

    @Override
    public void setOutgoingChannel(byte channel) {
        outgoingChannel = channel;
    }

    @Override
    public void errorRequestingFSM(PeerFSMServerResponse serverResponse) {
        System.out.println("AskFiles request denied: " + serverResponse);
    }
}
