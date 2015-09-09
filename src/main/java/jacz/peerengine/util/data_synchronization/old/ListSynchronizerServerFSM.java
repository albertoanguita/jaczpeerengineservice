package jacz.peerengine.util.data_synchronization.old;

import jacz.peerengine.PeerID;
import jacz.peerengine.client.PeerClient;
import jacz.peerengine.client.PeerFSMServerResponse;
import jacz.peerengine.client.PeerTimedFSMAction;
import jacz.peerengine.util.ConnectionStatus;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.util.io.object_serialization.MutableOffset;
import jacz.util.io.object_serialization.Serializer;
import jacz.util.lists.Duple;
import jacz.util.notification.ProgressNotificationWithError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Server fsm for list synchronization
 * <p/>
 * Protocol is described in client FSM
 */
public class ListSynchronizerServerFSM implements PeerTimedFSMAction<ListSynchronizerServerFSM.State> {

    /**
     * Name of this custom FSM (must start with a special character because it is not user made)
     */
    public static final String CUSTOM_FSM_NAME = PeerClient.OWN_CUSTOM_PREFIX + "ListSynchronizerFSM";

    enum State {
        // Initial state: the server peer waits for details of the request
        // once we receive it, we check that the information is OK (list name, level, list configuration, etc)
        // if ok, we send an OK to the client, and initiate the indexAndHashSynchClient. This object dictates the next state (see next state)
        // TYPE: BYTE_ARRAY
        WAITING_FOR_REQUEST,

        // during the process of synchronizing the hashes
        // waiting for client's responses to the indexAndHashSynchClient queries
        // data is passed to the indexAndHashSynchClient
        // while the synch process is not complete, we stay in this state
        // if the synch process is complete, we move to
        // - DATA_TRANSMISSION_PROCESS_INIT if the elements are different from the hashes
        // - SUCCESS_NOTIFY_COMPLETE if the elements are similar to the hashes
        // - ERROR if there has been an error
        // TYPE: BYTE_ARRAY
        INDEX_AND_HASH_SYNCH_PROCESS,

        // the index and hash synch process just finished. The list of indexes and hashes that the client must request us was just sent to the
        // client.
        // we now initiate the transfer process with the elementTransferSynchServer, which will tell us to what state we must move
        // TYPE: TRANSIENT (we reach this state after invoking an action to the indexAndHashSynchClient, not when receiving any data)
        DATA_TRANSMISSION_PROCESS_INIT,

        // the hash synch process just finished. The client is sending us the amount of indexes that he will request, as an integer
        // TYPE: BYTE_ARRAY
        WAITING_FOR_INDEXES_TO_SERVE_COUNT,

        // the client is sending us the list of indexes that he will request (only for byte array transmissions)
        // TYPE: BYTE_ARRAY
        WAITING_FOR_INDEXES_TO_SERVE,

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

    enum InitialRequestAnswerType {
        INVALID_REQUEST_FORMAT,
        UNKNOWN_LIST,
        ELEMENT_NOT_FOUND,
        INVALID_LEVEL,
        REQUEST_DENIED,
        DIFFERENT_LISTS_CONFIG,
        SERVER_BUSY,
        OK
    }

    static class InitialRequestAnswer {

        final InitialRequestAnswerType initialRequestAnswerType;

        InitialRequestAnswer(InitialRequestAnswerType initialRequestAnswerType) {
            this.initialRequestAnswerType = initialRequestAnswerType;
        }

        byte[] serialize() {
            return Serializer.serialize(initialRequestAnswerType);
        }

        static InitialRequestAnswer deserialize(byte[] data) {
            MutableOffset offset = new MutableOffset();
            InitialRequestAnswerType initialRequestAnswerType = Serializer.deserializeEnum(InitialRequestAnswerType.class, data, offset);
            return new InitialRequestAnswer(initialRequestAnswerType);
        }
    }

    private byte outgoingChannel;

    private final ListContainer listContainer;

    private ListAccessor listAccessor;

    private int level;

    private OrderedListSynchClient<State> indexAndHashSynchClient;

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
    public ListSynchronizerServerFSM(ListSynchronizer listSynchronizer) {
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

            case INDEX_AND_HASH_SYNCH_PROCESS:
                return indexAndHashSynchClient.synchProcess(data);
//                if (newState == State.DATA_TRANSMISSION_PROCESS_INIT) {
//                    return setupElementTransferSynchServer(ccp);
//                } else {
//                    return newState;
//                }

            case WAITING_FOR_INDEXES_TO_SERVE_COUNT:
                int indexesToServeCount = Serializer.deserializeInt(data, new MutableOffset());
                return setupElementTransferSynchServer(ccp, indexesToServeCount);

            case WAITING_FOR_INDEXES_TO_SERVE:
                return elementTransferSynchServer.newIndexesToServe(data);

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
        ListSynchronizerClientFSM.SynchRequest request;
        try {
            Object object = Serializer.deserializeObject(data, new MutableOffset());
            if (!(object instanceof ListSynchronizerClientFSM.SynchRequest)) {
                // unrecognized class
                ccp.write(outgoingChannel, new InitialRequestAnswer(InitialRequestAnswerType.INVALID_REQUEST_FORMAT).serialize());
                return State.ERROR;
            }
            request = (ListSynchronizerClientFSM.SynchRequest) object;
        } catch (ClassNotFoundException e) {
            // invalid class found, error
            ccp.write(outgoingChannel, new InitialRequestAnswer(InitialRequestAnswerType.INVALID_REQUEST_FORMAT).serialize());
            return State.ERROR;
        }
        Duple<ListAccessor, Integer> listAndLevel = assignListAccessorAnfLevelAndCheckForRequestErrors(request, listSynchronizer, listContainer, ccp, outgoingChannel);
        if (listAndLevel == null) {
            return State.ERROR;
        }
        listAccessor = listAndLevel.element1;
        level = listAndLevel.element2;

        // answer each type of request
        // request for a full list synch
        ServerSynchRequestAnswer serverSynchRequestAnswer = listAccessor.initiateListSynchronizationAsServer(request.clientPeerID, level, false);
        if (serverSynchRequestAnswer.type == ServerSynchRequestAnswer.Type.OK) {
            listAccessor.beginSynchProcess(ListAccessor.Mode.SERVER);
            ccp.write(outgoingChannel, new InitialRequestAnswer(InitialRequestAnswerType.OK).serialize());
            progress = serverSynchRequestAnswer.progress;
            try {
                return initialIndexAndHashSynchProcess(ccp, listAccessor.getHashList(level));
            } catch (DataAccessException e) {
                synchronizeError = new Duple<>(SynchronizeError.Type.DATA_ACCESS_ERROR, "Data access error in the list accessor implementation");
                return State.ERROR;
            }
        } else {
            ccp.write(outgoingChannel, new InitialRequestAnswer(InitialRequestAnswerType.SERVER_BUSY).serialize());
            return State.ERROR;
        }
    }

    static Duple<ListAccessor, Integer> assignListAccessorAnfLevelAndCheckForRequestErrors(
            ListSynchronizerClientFSM.SynchRequest request,
            ListSynchronizer listSynchronizer,
            ListContainer listContainer,
            ChannelConnectionPoint ccp,
            byte outgoingChannel) {
        // requests are sometimes denied
        // if the requesting peer is not our friend, the request is automatically denied
        // if allowSynchronizingBetweenNonFriendPeers is false we also check that the status is correct. Any other status (meaning that we are not friend of him) produces a denial
        ListPath listPath = request.listPath;
        PeerID clientPeerID = request.clientPeerID;
        if (!listSynchronizer.getPeerClient().isFriendPeer(clientPeerID) ||
                (!listSynchronizer.isAllowSynchronizingBetweenNonFriendPeers() && !listSynchronizer.getPeerClient().getPeerConnectionStatus(clientPeerID).equals(ConnectionStatus.CORRECT))) {
            ccp.write(outgoingChannel, new InitialRequestAnswer(InitialRequestAnswerType.REQUEST_DENIED).serialize());
            return null;
        }
        // check that we have such list, that the list accessor config hash matches with our own, and that the requested level is ok
        try {
            Duple<ListAccessor, Integer> listAndLevel = ListSynchronizerManager.getListForReading(listContainer, clientPeerID, listPath);
            ListAccessor listAccessor = listAndLevel.element1;
            int level = listAndLevel.element2;
            if (!Arrays.equals(ListSynchronizerClientFSM.getListConfigHash(listAccessor), request.listConfigHash)) {
                ccp.write(outgoingChannel, new InitialRequestAnswer(InitialRequestAnswerType.DIFFERENT_LISTS_CONFIG).serialize());
                return null;
            }
            int allowedLevelCount = listAccessor.getLevelCount();
            if (level < 0 || level > allowedLevelCount) {
                ccp.write(outgoingChannel, new InitialRequestAnswer(InitialRequestAnswerType.INVALID_LEVEL).serialize());
                return null;
            }
            return new Duple<>(listAccessor, level);
        } catch (ListNotFoundException e) {
            ccp.write(outgoingChannel, new InitialRequestAnswer(InitialRequestAnswerType.UNKNOWN_LIST).serialize());
            return null;
        }
    }

    private State initialIndexAndHashSynchProcess(ChannelConnectionPoint ccp, Collection<IndexAndHash> indexAndHashCollection) {
        // initialize the related private fields (sorted hashList, hashQueryTable, hashesToSend) and submit the initial
        // query if we have an empty initial hash list, finish this process as no hash must be sent (and all at the
        // client end must be erased)
        List<String> indexAndHashStringList = new ArrayList<>();
        for (IndexAndHash indexAndHash : indexAndHashCollection) {
            indexAndHashStringList.add(indexAndHash.toString());
        }
        indexAndHashSynchClient = new OrderedListSynchClient<>(indexAndHashStringList, ccp, outgoingChannel, listAccessor.hashEqualsElement(level), new OrderedListSynchClient.State<State>() {
            @Override
            public State synchronizingState() {
                return State.INDEX_AND_HASH_SYNCH_PROCESS;
            }

            @Override
            public State finishedSynchMustTransferState() {
                return State.WAITING_FOR_INDEXES_TO_SERVE_COUNT;
            }

            @Override
            public State finishedSynchSkipTransfer() {
                return State.SUCCESS_NOTIFY_COMPLETE;
            }

            @Override
            public State errorState() {
                return State.ERROR;
            }
        });
        return indexAndHashSynchClient.initialSynchProcess();
//        if (state == State.DATA_TRANSMISSION_PROCESS_INIT) {
//            return setupElementTransferSynchServer(ccp);
//        } else {
//            return state;
//        }
    }

    private State setupElementTransferSynchServer(ChannelConnectionPoint ccp, int indexesToServeCount) {
        elementTransferSynchServer = new ElementTransferSynchServer<>(listAccessor, level, progress, ccp, outgoingChannel, listSynchronizer.getPeerClient(), new ElementTransferSynchServer.State<State>() {
            @Override
            public State expectObjectRequestsState() {
                return State.OBJECT_TRANSMISSION_PROCESS;
            }

            @Override
            public State waitingForIndexesToServe() {
                return State.WAITING_FOR_INDEXES_TO_SERVE;
            }

            @Override
            public State finishedTransfer() {
                return State.SUCCESS_NOTIFY_COMPLETE;
            }

            @Override
            public State finishedTransferRemoveResourceStore() {
                return State.BYTE_ARRAY_TRANSMISSION_PROCESS;
            }
        }, indexesToServeCount);
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

            case BYTE_ARRAY_TRANSMISSION_PROCESS:
                // the resource is being sent, the FSM must finish
                return true;

            case ERROR:
                if (listAccessor != null) {
                    listAccessor.endSynchProcess(ListAccessor.Mode.SERVER, false);
                }
                ListSynchronizerClientFSM.reportError(progress, synchronizeError.element1, synchronizeError.element2);
                return true;
        }
        return false;
    }

    @Override
    public void disconnected(ChannelConnectionPoint ccp) {
        // we got disconnected from the other peer before the synch finished -> notify as an error
        if (listAccessor != null) {
            listAccessor.endSynchProcess(ListAccessor.Mode.SERVER, false);
        }
        if (progress != null) {
            progress.error(new SynchronizeError(SynchronizeError.Type.DISCONNECTED, null));
        }
    }

    @Override
    public void timedOut(State state) {
        if (listAccessor != null) {
            listAccessor.endSynchProcess(ListAccessor.Mode.SERVER, false);
        }
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
