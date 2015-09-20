package jacz.peerengineservice.test.customfsm;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.util.io.object_serialization.ObjectListWrapper;
import jacz.peerengineservice.client.PeerFSMServerResponse;
import jacz.peerengineservice.client.PeerTimedFSMAction;

import java.util.HashSet;
import java.util.Set;

/**
 * Simple peer timed action example
 */
public class AskFilesFSM implements PeerTimedFSMAction<AskFilesFSM.State> {

    enum State {
        REQUEST_SENT,
        SUCCESS,
        ERROR
    }

    private byte outgoingChannel;

    public AskFilesFSM() {
    }

    @Override
    public State processMessage(State state, byte channel, Object message, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        switch (state) {

            case REQUEST_SENT:
                // files received
                if (message instanceof ObjectListWrapper) {
                    ObjectListWrapper objectListWrapper = (ObjectListWrapper) message;
                    Set<String> fileHashes = new HashSet<String>(objectListWrapper.getObjects().size());
                    for (Object o : objectListWrapper.getObjects()) {
                        fileHashes.add((String) o);
                    }
                    System.out.println("AskFilesFSM -> Received file hashes: " + fileHashes);
                    return State.SUCCESS;
                } else {
                    return State.ERROR;
                }
        }
        return State.ERROR;
    }

    @Override
    public State processMessage(State state, byte channel, byte[] data, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        // unexpected data
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
    public void timedOut(State state) {
        System.out.println("Ask files FSM timed out!!!");
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
