package jacz.peerengineservice.util.data_synchronization.old;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.client.PeerFSMServerResponse;
import jacz.peerengineservice.client.PeerTimedFSMAction;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.util.io.object_serialization.Serializer;
import jacz.util.lists.Duple;
import jacz.util.notification.ProgressNotificationWithError;

import java.util.ArrayList;
import java.util.List;

/**
 * Client fsm for single element synchronization
 */
public class ElementSynchronizerClientFSM implements PeerTimedFSMAction<ElementSynchronizerClientFSM.State> {

    enum State {
        // Initial state: the initial request to the other peer has been sent
        // The request includes the list path, owe peer id and, optionally, the element level and hash to synch
        // We also include the transmission type for that level, for checking the correctness
        // We expect the result of this request (accept or deny)
        // TYPE: BYTE_ARRAY
        WAITING_FOR_REQUEST_ANSWER,

        // the requested elements are objects
        // waiting for server to send the last requested element by the elementTransferSynchClient
        // received data is passed to the elementTransferSynchClient
        // TYPE: OBJECT
        DATA_TRANSMISSION_PROCESS_OBJECT,

        // the requested elements are byte arrays
        // the server must send us the name of the resource store for requesting the data
        // TYPE: BYTE_ARRAY
        WAITING_FOR_BYTE_ARRAY_RESOURCE_STORE_NAME,

        // process complete (final state). In addition, notify the completion to the progress element
        SUCCESS_NOTIFY_COMPLETE,

        // process complete (final state). Do not notify the completion of the progress
        SUCCESS_NOT_NOTIFY_COMPLETE,

        // error during the communication (final state)
        ERROR,

        // error due to element modified in server during synchronization
        ERROR_ELEMENT_CHANGED_IN_SERVER
    }

    /**
     * Requests sent from the client FSM to the server FSM to initiate the process. This request is only send once and contains all the required
     * data to start the synchronization process. The request is send as an array byte to avoid bandwidth looses
     */
    static class SynchRequest extends ListSynchronizerClientFSM.SynchRequest {

        final String index;

        SynchRequest(PeerID clientPeerID, ListPath listPath, ListAccessor listAccessor, String index) {
            super(clientPeerID, listPath, listAccessor);
            this.index = index;
        }
    }

    private final PeerClient peerClient;

    private ListSynchronizer listSynchronizer;

    private byte outgoingChannel;

    private ListContainer listContainer;

    private ListAccessor listAccessor;

    private final ListSynchronizerManager listSynchronizerManager;

    private final PeerID serverPeerID;

    private final ListPath listPath;

    private final String requestedIndex;

    private int level;

    private final ProgressNotificationWithError<Integer, SynchronizeError> progress;

    private ElementTransferSynchClient<State> elementTransferSynchClient;

    private Duple<SynchronizeError.Type, String> synchronizeError;

    /**
     * Class constructor
     *
     * @param listSynchronizerManager the manager that owns this ListSynchronizerClientFSM
     * @param serverPeerID            the PeerID of the server to who we make the request
     * @param listPath                the request itself (name and level of the main list, or path to an inner list)
     * @param progress                progress to which we must report (null if not to be used)
     */
    public ElementSynchronizerClientFSM(PeerClient peerClient, ListSynchronizerManager listSynchronizerManager, PeerID serverPeerID, ListPath listPath, String requestedIndex, ProgressNotificationWithError<Integer, SynchronizeError> progress) {
        this.peerClient = peerClient;
//        this.listSynchronizer = peerClient.getListSynchronizer();
//        listContainer = listSynchronizer.getListContainer();
        this.serverPeerID = serverPeerID;
        this.listSynchronizerManager = listSynchronizerManager;
        this.listPath = listPath;
        this.requestedIndex = requestedIndex;
        this.progress = progress;
        this.synchronizeError = new Duple<>(SynchronizeError.Type.UNDEFINED, null);
    }

    @Override
    public State processMessage(State state, byte channel, Object message, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        switch (state) {

            case DATA_TRANSMISSION_PROCESS_OBJECT:
                try {
                    return elementTransferSynchClient.receiveObjectElement(message);
                } catch (DataAccessException e) {
                    synchronizeError = new Duple<>(SynchronizeError.Type.DATA_ACCESS_ERROR, "Data access error in the list accessor implementation");
                    return State.ERROR;
                }

            default:
                synchronizeError = new Duple<>(SynchronizeError.Type.ERROR_IN_PROTOCOL, "Unexpected object data at state " + state);
                return State.ERROR;
        }
    }

    @Override
    public State processMessage(State state, byte channel, byte[] data, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        switch (state) {

            case WAITING_FOR_REQUEST_ANSWER:
                ElementSynchronizerServerFSM.InitialRequestAnswer answer = ElementSynchronizerServerFSM.InitialRequestAnswer.deserialize(data);
                if (answer.initialRequestAnswerType != ListSynchronizerServerFSM.InitialRequestAnswerType.OK) {
                    ListSynchronizerClientFSM.processDeniedRequest(answer.initialRequestAnswerType);
                    return State.ERROR;
                } else {
                    // read the server answer to our request (request for single element synch)
                    // check if we already have the element, or if the hash is the element itself
                    listAccessor.beginSynchProcess(ListAccessor.Mode.CLIENT);
                    boolean containsElement;
                    try {
                        containsElement = !listAccessor.mustRequestElement(requestedIndex, level, answer.hash);
                    } catch (DataAccessException e) {
                        synchronizeError = new Duple<>(SynchronizeError.Type.DATA_ACCESS_ERROR, "Data access error in the list accessor implementation");
                        return State.ERROR;
                    }
                    if (containsElement) {
                        // we already have the element, no need to request it
                        ccp.write(outgoingChannel, Serializer.serialize(false));
                        return State.SUCCESS_NOTIFY_COMPLETE;
                    } else if (listAccessor.hashEqualsElement(level)) {
                        try {
                            listAccessor.addElementAsObject(requestedIndex, level, answer.hash);
                        } catch (DataAccessException e) {
                            synchronizeError = new Duple<>(SynchronizeError.Type.DATA_ACCESS_ERROR, "Data access error in the list accessor implementation");
                            return State.ERROR;
                        }
                        ccp.write(outgoingChannel, Serializer.serialize(false));
                        return State.SUCCESS_NOTIFY_COMPLETE;
                    }
                    // we need to request the element. Notify this to the server with a true, and set up the element transfer
                    ccp.write(outgoingChannel, Serializer.serialize(true));
                    List<IndexAndHash> indexAndHashesToRequest = new ArrayList<>();
                    indexAndHashesToRequest.add(new IndexAndHash(requestedIndex, answer.hash));
                    elementTransferSynchClient = new ElementTransferSynchClient<>(
                            peerClient,
                            listAccessor,
                            level,
                            progress,
                            ccp,
                            outgoingChannel,
                            new ElementTransferSynchClient.State<State>() {
                                @Override
                                public State expectObjectsState() {
                                    return State.DATA_TRANSMISSION_PROCESS_OBJECT;
                                }

                                @Override
                                public State expectByteArrayResourceStoreName() {
                                    return State.WAITING_FOR_BYTE_ARRAY_RESOURCE_STORE_NAME;
                                }

                                @Override
                                public State finishedTransfer() {
                                    return State.SUCCESS_NOTIFY_COMPLETE;
                                }

                                @Override
                                public State finishedTransferNotNotifyComplete() {
                                    return State.SUCCESS_NOT_NOTIFY_COMPLETE;
                                }

                                @Override
                                public State finishedTransferSomeElementChangedInServer() {
                                    return State.ERROR_ELEMENT_CHANGED_IN_SERVER;
                                }
                            },
                            serverPeerID,
                            listSynchronizerManager,
                            indexAndHashesToRequest, 1, 0);
                    try {
                        return elementTransferSynchClient.initiateDataTransferProcess();
                    } catch (DataAccessException e) {
                        synchronizeError = new Duple<>(SynchronizeError.Type.DATA_ACCESS_ERROR, "Data access error in the list accessor implementation");
                        return State.ERROR;
                    }
                }

            case WAITING_FOR_BYTE_ARRAY_RESOURCE_STORE_NAME:
                return elementTransferSynchClient.receivedByteArrayResourceStoreName(data);

            default:
                // unexpected data
                synchronizeError = new Duple<>(SynchronizeError.Type.ERROR_IN_PROTOCOL, "Unexpected byte[] data at state " + state);
                return State.ERROR;
        }
    }

    @Override
    public State init(ChannelConnectionPoint ccp) {
        // submit the initial request, where we specify which list we want to synchronize, and with what parameters
        try {
            Duple<ListAccessor, Integer> listAndLevel = ListSynchronizerManager.getListForWriting(listContainer, serverPeerID, listPath);
            listAccessor = listAndLevel.element1;
            level = listAndLevel.element2;
            if (ListSynchronizerClientFSM.checkListAndLevel(listAccessor, level)) {
                SynchRequest synchRequest;
                // the request for the synch of a full list of elements consists on the list path and our own PeerID
                synchRequest = new SynchRequest(listSynchronizer.getOwnPeerID(), listPath, listAccessor, requestedIndex);
                ccp.write(outgoingChannel, Serializer.serializeObject(synchRequest));
                return State.WAITING_FOR_REQUEST_ANSWER;
            } else {
                // incorrect level requested
                ListSynchronizerClientFSM.generateIncorrectLevelError(listPath, level);
                return State.ERROR;
            }
        } catch (ListNotFoundException e) {
            synchronizeError = ListSynchronizerClientFSM.generateUnknownListError(listPath);
            return State.ERROR;
        }
    }

    @Override
    public boolean isFinalState(State state, ChannelConnectionPoint ccp) {
        switch (state) {
            case SUCCESS_NOTIFY_COMPLETE:
                listAccessor.endSynchProcess(ListAccessor.Mode.CLIENT, true);
                if (progress != null) {
                    progress.completeTask();
                }
                return true;

            case SUCCESS_NOT_NOTIFY_COMPLETE:
                // nothing to notify here, as the synch process continues somewhere else. Completion or error will be notified somewhere else
                return true;

            case ERROR:
                listAccessor.endSynchProcess(ListAccessor.Mode.CLIENT, false);
                ListSynchronizerClientFSM.reportError(progress, synchronizeError.element1, synchronizeError.element2);
                return true;

            case ERROR_ELEMENT_CHANGED_IN_SERVER:
                listAccessor.endSynchProcess(ListAccessor.Mode.CLIENT, false);
                ListSynchronizerClientFSM.reportError(progress, SynchronizeError.Type.ELEMENT_CHANGED_IN_SERVER, null);
                return true;
        }
        return false;
    }

    @Override
    public void disconnected(ChannelConnectionPoint ccp) {
        // we got disconnected from the other peer before the synch finished -> notify as an error
        listAccessor.endSynchProcess(ListAccessor.Mode.CLIENT, false);
        if (progress != null) {
            progress.error(new SynchronizeError(SynchronizeError.Type.DISCONNECTED, null));
        }
    }

    @Override
    public void timedOut(State state) {
        listAccessor.endSynchProcess(ListAccessor.Mode.CLIENT, false);
        if (progress != null) {
            progress.timeout();
        }
    }

    @Override
    public void setOutgoingChannel(byte channel) {
        outgoingChannel = channel;
    }

    @Override
    public void errorRequestingFSM(PeerFSMServerResponse serverResponse) {
        ListSynchronizerClientFSM.reportError(progress, SynchronizeError.Type.SERVER_BUSY);
    }
}
