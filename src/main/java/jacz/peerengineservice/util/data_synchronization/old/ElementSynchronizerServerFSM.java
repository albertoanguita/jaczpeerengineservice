package jacz.peerengineservice.util.data_synchronization.old;

//import com.twmacinta.util.MD5;

import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.client.PeerFSMServerResponse;
import jacz.peerengineservice.client.PeerTimedFSMAction;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.util.io.object_serialization.MutableOffset;
import jacz.util.io.object_serialization.Serializer;
import jacz.util.lists.Duple;
import jacz.util.notification.ProgressNotificationWithError;

import java.util.ArrayList;
import java.util.List;

/**
 * Server fsm for element synchronization
 * <p/>
 * Protocol is described in client FSM
 */
public class ElementSynchronizerServerFSM implements PeerTimedFSMAction<ElementSynchronizerServerFSM.State> {

    /**
     * Name of this custom FSM (must start with a special character because it is not user made)
     */
    public static final String CUSTOM_FSM_NAME = PeerClient.OWN_CUSTOM_PREFIX + "ElementSynchronizerFSM";

    enum State {
        // Initial state: the server peer waits for details of the request
        // once we receive it, we check that the information is OK (list name, level, list configuration, etc)
        // if ok, we send an OK to the client, and initiate the indexAndHashSynchClient. This object dictates the next state (see next state)
        // TYPE: BYTE_ARRAY
        WAITING_FOR_REQUEST,

        // Waiting for the client to confirm if he needs the element to be transferred. If the element equals the hash, this is skipped
        // TYPE: BYTE_ARRAY
        WAITING_FOR_CLIENT_CONFIRMATION_FOR_TRANSMISSION,

        // the index and hash synch process just finished. The list of indexes and hashes that the client must request us was just sent to the
        // client.
        // we now initiate the transfer process with the elementTransferSynchServer, which will tell us to what state we must move
        // TYPE: TRANSIENT (we reach this state after invoking an action to the indexAndHashSynchClient, not when receiving any data)
        DATA_TRANSMISSION_PROCESS_INIT,

        // waiting for the client to request object elements
        // TYPE: BYTE_ARRAY
        OBJECT_TRANSMISSION_PROCESS,

        // the state when we have just set up the resource store for the client to claim the byte-array elements through the resource streaming
        // manager functionality
        // TYPE: TRANSIENT (we are just waiting for the timeout to remove the resource store and finish without notifying)
        BYTE_ARRAY_TRANSMISSION_PROCESS,

        // process complete (final state). In addition, notify the completion to the progress element
        SUCCESS_NOTIFY_COMPLETE,

        // process complete (final state)
        SUCCESS_NOT_NOTIFY_COMPLETE,

        // error during the communication (final state)
        ERROR
    }


    static class InitialRequestAnswer extends ListSynchronizerServerFSM.InitialRequestAnswer {

        final String hash;

        InitialRequestAnswer(ListSynchronizerServerFSM.InitialRequestAnswerType initialRequestAnswerType) {
            this(initialRequestAnswerType, null);
        }

        InitialRequestAnswer(ListSynchronizerServerFSM.InitialRequestAnswerType initialRequestAnswerType, String hash) {
            super(initialRequestAnswerType);
            this.hash = hash;
        }

        byte[] serialize() {
            return Serializer.addArrays(super.serialize(), Serializer.serialize(hash));
        }

        static InitialRequestAnswer deserialize(byte[] data) {
            MutableOffset offset = new MutableOffset();
            ListSynchronizerServerFSM.InitialRequestAnswerType initialRequestAnswerType = Serializer.deserializeEnum(ListSynchronizerServerFSM.InitialRequestAnswerType.class, data, offset);
            String hash = Serializer.deserializeString(data, offset);
            return new InitialRequestAnswer(initialRequestAnswerType, hash);
        }
    }

    private byte outgoingChannel;

    private final ListContainer listContainer;

    private ListAccessor listAccessor;

    private int level;

    private String requestedIndex;

    private ElementTransferSynchServer<State> elementTransferSynchServer;

    /**
     * Progress notifier for the server side. It is obtained form the list accessor (null if not used)
     */
    ProgressNotificationWithError<Integer, SynchronizeError> progress;

    /**
     * ListSynchronizer object
     */
    private final ListSynchronizer listSynchronizer;

    private Duple<SynchronizeError.Type, String> synchronizeError;

    /**
     * Class constructor
     *
     * @param listSynchronizer List synchronizer object for this list synchronizer server FSM
     */
    public ElementSynchronizerServerFSM(ListSynchronizer listSynchronizer) {
        this.listSynchronizer = listSynchronizer;
        listContainer = listSynchronizer.getListContainer();
        this.synchronizeError = new Duple<>(SynchronizeError.Type.UNDEFINED, null);
    }

    @Override
    public State processMessage(State state, byte channel, Object message, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        switch (state) {

            default:
                synchronizeError = new Duple<>(SynchronizeError.Type.ERROR_IN_PROTOCOL, "Unexpected object data at state " + state);
                return State.ERROR;
        }
    }

    @Override
    public State processMessage(State state, byte channel, byte[] data, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        switch (state) {
            case WAITING_FOR_REQUEST:
                return processMessageWaitingForRequest(data, ccp);

            case WAITING_FOR_CLIENT_CONFIRMATION_FOR_TRANSMISSION:
                boolean clientRequestsElement = Serializer.deserializeBoolean(data, new MutableOffset());
                if (clientRequestsElement) {
                    List<String> indexList = new ArrayList<>();
                    indexList.add(requestedIndex);
                    return setupElementTransferSynchServer(indexList, ccp);
                } else {
                    return State.SUCCESS_NOTIFY_COMPLETE;
                }

            case OBJECT_TRANSMISSION_PROCESS:
                try {
                    return elementTransferSynchServer.transferObject(data);
                } catch (DataAccessException e) {
                    synchronizeError = new Duple<>(SynchronizeError.Type.DATA_ACCESS_ERROR, "Data access error in the list accessor implementation");
                    return State.ERROR;
                }

            default:
                // unexpected data
                synchronizeError = new Duple<>(SynchronizeError.Type.ERROR_IN_PROTOCOL, "Unexpected byte[] data at state " + state);
                return State.ERROR;
        }
    }

    private State processMessageWaitingForRequest(byte[] data, ChannelConnectionPoint ccp) {
        // parse the request details
        ElementSynchronizerClientFSM.SynchRequest request;
        try {
            Object object = Serializer.deserializeObject(data, new MutableOffset());
            if (!(object instanceof ElementSynchronizerClientFSM.SynchRequest)) {
                // unrecognized class
                ccp.write(outgoingChannel, new InitialRequestAnswer(ListSynchronizerServerFSM.InitialRequestAnswerType.INVALID_REQUEST_FORMAT).serialize());
                return State.ERROR;
            }
            request = (ElementSynchronizerClientFSM.SynchRequest) object;
        } catch (ClassNotFoundException e) {
            // invalid class found, error
            ccp.write(outgoingChannel, new InitialRequestAnswer(ListSynchronizerServerFSM.InitialRequestAnswerType.INVALID_REQUEST_FORMAT).serialize());
            return State.ERROR;
        }

        Duple<ListAccessor, Integer> listAndLevel = ListSynchronizerServerFSM.assignListAccessorAnfLevelAndCheckForRequestErrors(request, listSynchronizer, listContainer, ccp, outgoingChannel);
        if (listAndLevel == null) {
            return State.ERROR;
        }
        listAccessor = listAndLevel.element1;
        level = listAndLevel.element2;

        // request the synch process to the list accessor
        ServerSynchRequestAnswer serverSynchRequestAnswer = listAccessor.initiateListSynchronizationAsServer(request.clientPeerID, level, true);

        if (serverSynchRequestAnswer.type == ServerSynchRequestAnswer.Type.OK) {
            listAccessor.beginSynchProcess(ListAccessor.Mode.SERVER);
            try {
                requestedIndex = request.index;
                String requestedHash = listAccessor.getElementHash(requestedIndex, level);
                ccp.write(outgoingChannel, new InitialRequestAnswer(ListSynchronizerServerFSM.InitialRequestAnswerType.OK, requestedHash).serialize());
                progress = serverSynchRequestAnswer.progress;
                if (listAccessor.hashEqualsElement(level)) {
                    // the request answer is all that the client needs -> complete
                    return State.SUCCESS_NOTIFY_COMPLETE;
                } else {
                    // we must wait for the client to confirm if he wants the element to be transferred
                    return State.WAITING_FOR_CLIENT_CONFIRMATION_FOR_TRANSMISSION;
                }
            } catch (ElementNotFoundException e) {
                ccp.write(outgoingChannel, new InitialRequestAnswer(ListSynchronizerServerFSM.InitialRequestAnswerType.ELEMENT_NOT_FOUND).serialize());
                return State.ERROR;
            } catch (DataAccessException e) {
                synchronizeError = new Duple<>(SynchronizeError.Type.DATA_ACCESS_ERROR, "Data access error in the list accessor implementation");
                return State.ERROR;
            }
        } else {
            ccp.write(outgoingChannel, new InitialRequestAnswer(ListSynchronizerServerFSM.InitialRequestAnswerType.SERVER_BUSY).serialize());
            return State.ERROR;
        }
    }

    private State setupElementTransferSynchServer(List<String> indexList, ChannelConnectionPoint ccp) {
        elementTransferSynchServer = new ElementTransferSynchServer<>(listAccessor, level, progress, ccp, outgoingChannel, listSynchronizer.getPeerClient(), new ElementTransferSynchServer.State<State>() {
            @Override
            public State expectObjectRequestsState() {
                return State.OBJECT_TRANSMISSION_PROCESS;
            }

            @Override
            public State waitingForIndexesToServe() {
                return null;
            }

            @Override
            public State finishedTransfer() {
                return State.SUCCESS_NOTIFY_COMPLETE;
            }

            @Override
            public State finishedTransferRemoveResourceStore() {
                return State.BYTE_ARRAY_TRANSMISSION_PROCESS;
            }
        }, 0);
        return elementTransferSynchServer.initiateDataTransferProcess();
    }

    @Override
    public State init(ChannelConnectionPoint ccp) {
        // initially we wait for the original request from the client peer
        return State.WAITING_FOR_REQUEST;
    }

    @Override
    public boolean isFinalState(State state, ChannelConnectionPoint ccp) {
        switch (state) {
            case SUCCESS_NOT_NOTIFY_COMPLETE:
                return true;

            case SUCCESS_NOTIFY_COMPLETE:
                listAccessor.endSynchProcess(ListAccessor.Mode.SERVER, true);
                if (progress != null) {
                    progress.completeTask();
                }
                return true;

            case ERROR:
                listAccessor.endSynchProcess(ListAccessor.Mode.SERVER, false);
                ListSynchronizerClientFSM.reportError(progress, synchronizeError.element1, synchronizeError.element2);
                return true;
        }
        return false;
    }

    @Override
    public void disconnected(ChannelConnectionPoint ccp) {
        // we got disconnected from the other peer before the synch finished -> notify as an error
        listAccessor.endSynchProcess(ListAccessor.Mode.SERVER, false);
        if (progress != null) {
            progress.error(new SynchronizeError(SynchronizeError.Type.DISCONNECTED, null));
        }
    }

    @Override
    public void timedOut(State state) {
        listAccessor.endSynchProcess(ListAccessor.Mode.SERVER, false);
        if (state == State.BYTE_ARRAY_TRANSMISSION_PROCESS) {
            // time to shut down the resource store (the client should have issued a request by now, so the resource store is no longer needed)
            elementTransferSynchServer.removeResourceStore();
        } else if (progress != null) {
            progress.timeout();
        }
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
