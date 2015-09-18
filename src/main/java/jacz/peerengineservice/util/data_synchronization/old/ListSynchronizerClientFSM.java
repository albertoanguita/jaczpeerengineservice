package jacz.peerengineservice.util.data_synchronization.old;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.client.PeerFSMServerResponse;
import jacz.peerengineservice.client.PeerTimedFSMAction;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.util.hash.SHA_256;
import jacz.util.io.object_serialization.Serializer;
import jacz.util.lists.Duple;
import jacz.util.notification.ProgressNotificationWithError;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Client fsm for full list synchronization
 */
public class ListSynchronizerClientFSM implements PeerTimedFSMAction<ListSynchronizerClientFSM.State> {

    enum State {
        // Initial state: the initial request to the other peer has been sent
        // The request includes the list path, owe peer id and, optionally, the element level and hash to synch
        // We also include the transmission type for that level, for checking the correctness
        // We expect the result of this request (accept or deny)
        // TYPE: BYTE_ARRAY
        WAITING_FOR_REQUEST_ANSWER,

        // if the request answer was OK, we start the index and hash synch process, so the server can calculate what elements we need
        // we are expecting hash queries from the server to determine which hashes the server must send us. We reply according to the
        // hashes that we have, and wait for the server to say that he is done with this process
        // we leave this state when the server says we are done. We will then check if we must request elements, or we are done
        // TYPE: BYTE_ARRAY
        INDEX_AND_HASH_SYNCH_PROCESS,

        // the index and hash synch process just finished
        // waiting for server to send the number of hashes that we need. After we get it, we will wait for the hashes themselves
        // TYPE: BYTE_ARRAY
//        WAITING_FOR_INDEX_AND_HASHES_SIZE_TO_REQUEST,

        // the index and hash synch process just finished
        // waiting for server to send the list of hashes that we need. With these data, we initialize the elementTransferSynchClient
        // with it, and execute its initiateDataTransferProcess, and start requesting elements
        // TYPE: BYTE_ARRAY
        WAITING_FOR_INDEX_AND_HASHES_TO_REQUEST,

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
    static class SynchRequest implements Serializable {

        final PeerID clientPeerID;

        final ListPath listPath;

        final byte[] listConfigHash;

        SynchRequest(PeerID clientPeerID, ListPath listPath, ListAccessor listAccessor) {
            this.clientPeerID = clientPeerID;
            this.listPath = listPath;
            this.listConfigHash = getListConfigHash(listAccessor);
        }
    }

    private final PeerClient peerClient;

    private final ListSynchronizer listSynchronizer;

    private byte outgoingChannel;

    private final ListContainer listContainer;

    private ListAccessor listAccessor;

    private final ListSynchronizerManager listSynchronizerManager;

    private final PeerID serverPeerID;

    private final ListPath listPath;

    private int level;

    private final ProgressNotificationWithError<Integer, SynchronizeError> progress;

    private OrderedListSynchServer<State> indexAndHashesSynchServer;

//    private int elementsToRequestCount;

    /**
     * List of indexes and corresponding hashes that will be finally requested to the server
     */
    private List<IndexAndHash> indexAndHashesToRequest;

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
    public ListSynchronizerClientFSM(PeerClient peerClient, ListSynchronizerManager listSynchronizerManager, PeerID serverPeerID, ListPath listPath, ProgressNotificationWithError<Integer, SynchronizeError> progress) {
        this.peerClient = peerClient;
//        this.listSynchronizer = peerClient.getListSynchronizer();
        this.listSynchronizer = null;
        listContainer = listSynchronizer.getListContainer();
        this.serverPeerID = serverPeerID;
        this.listSynchronizerManager = listSynchronizerManager;
        this.listPath = listPath;
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
                ListSynchronizerServerFSM.InitialRequestAnswer answer = ListSynchronizerServerFSM.InitialRequestAnswer.deserialize(data);
                if (answer.initialRequestAnswerType != ListSynchronizerServerFSM.InitialRequestAnswerType.OK) {
                    synchronizeError = processDeniedRequest(answer.initialRequestAnswerType);
                    return State.ERROR;
                } else {
                    // read the server answer to our request (request for full list synch)
                    // prepare to process server hash queries, initializing all related fields (ordered clientIndexAndHashes)
                    // finally, go to the INDEX_AND_HASH_SYNCH_PROCESS state
                    listAccessor.beginSynchProcess(ListAccessor.Mode.CLIENT);
                    List<String> indexAndHashList = new ArrayList<>();
                    try {
                        for (IndexAndHash indexAndHash : listAccessor.getHashList(level)) {
                            indexAndHashList.add(indexAndHash.toString());
                        }
                    } catch (DataAccessException e) {
                        synchronizeError = new Duple<>(SynchronizeError.Type.DATA_ACCESS_ERROR, "Data access error in the list accessor implementation");
                        return State.ERROR;
                    }
                    indexAndHashesSynchServer = new OrderedListSynchServer<>(indexAndHashList, ccp, outgoingChannel, new OrderedListSynchServer.State<State>() {
                        @Override
                        public State inProcessState() {
                            return State.INDEX_AND_HASH_SYNCH_PROCESS;
                        }

                        @Override
                        public State finishedSynch() {
                            indexAndHashesToRequest = new ArrayList<>();
                            return State.WAITING_FOR_INDEX_AND_HASHES_TO_REQUEST;
                        }
                    });
                    return indexAndHashesSynchServer.initialState();
                }

            case INDEX_AND_HASH_SYNCH_PROCESS:
                return indexAndHashesSynchServer.synchProcess(data);

//            case WAITING_FOR_INDEX_AND_HASHES_SIZE_TO_REQUEST:
//                // message with the total number of elements to request to the server
////                elementsToRequestCount = Serializer.deserializeInt(data, new MutableOffset());
//                indexAndHashesToRequest = new ArrayList<>();
//                return State.WAITING_FOR_INDEX_AND_HASHES_TO_REQUEST;

            case WAITING_FOR_INDEX_AND_HASHES_TO_REQUEST:
                // message containing the list of indexes and hashes that we need
                // the index and hash synch process just finished. We can calculate the indexes that we can remove from the remaining indexes at
                // the indexAndHashesSynchServer and the list of indexes that the server just sent us

                List<String> indexAndHashesStringList = OrderedListSynchServer.deserializeElements(data);
                if (!indexAndHashesStringList.isEmpty()) {
                    for (String indexAndHashString : indexAndHashesStringList) {
                        indexAndHashesToRequest.add(IndexAndHash.deserializeString(indexAndHashString));
                    }
                    return State.WAITING_FOR_INDEX_AND_HASHES_TO_REQUEST;
                }

                // we have all indexes and hashes -> remove the unnecessary ones and request the elements
                if (listAccessor.mustEraseOldIndexes()) {
                    // these are the indexes that we must remove (initialized with the indexes that remained from the index and hash synch phase)
                    List<String> indexesToRemove = new ArrayList<>();
                    for (String remainingIndexAndHashString : indexAndHashesSynchServer.getRemainingElements()) {
                        indexesToRemove.add(IndexAndHash.deserializeString(remainingIndexAndHashString).index);
                    }
                    for (IndexAndHash indexAndHashToRequest : indexAndHashesToRequest) {
                        // the indexes that we must request to the server are not to be removed
                        indexesToRemove.remove(indexAndHashToRequest.index);
                    }
                    // finally, we remove the remaining indexes
                    try {
                        listAccessor.eraseElements(indexesToRemove);
                    } catch (DataAccessException e) {
                        synchronizeError = new Duple<>(SynchronizeError.Type.DATA_ACCESS_ERROR, "Data access error in the list accessor implementation");
                        return State.ERROR;
                    }
                }
                // check if there are elements that must not be requested
                int totalElementsToRequest = indexAndHashesToRequest.size();
                int elementsAdded = 0;
                int i = 0;
                try {
                    while (i < indexAndHashesToRequest.size()) {
                        IndexAndHash indexAndHash = indexAndHashesToRequest.get(i);
                        if (!listAccessor.mustRequestElement(indexAndHash.index, level, indexAndHash.hash)) {
                            // the list accessor does not need to request this element
                            elementsAdded++;
                            indexAndHashesToRequest.remove(i);
                            ElementTransferSynchClient.reportProgress(progress, totalElementsToRequest, elementsAdded);
                        } else {
                            i++;
                        }
                    }
                } catch (DataAccessException e) {
                    synchronizeError = new Duple<>(SynchronizeError.Type.DATA_ACCESS_ERROR, "Data access error in the list accessor implementation");
                    return State.ERROR;
                }
                // indexAndHashesToRequest now contains the indexes that must be requested to the server
                // send the count of elements, so the server knows how many he will have to serve us
                ccp.write(outgoingChannel, Serializer.serialize(indexAndHashesToRequest.size()), true);

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
                        indexAndHashesToRequest,
                        totalElementsToRequest,
                        elementsAdded);
                try {
                    return elementTransferSynchClient.initiateDataTransferProcess();
                } catch (DataAccessException e) {
                    synchronizeError = new Duple<>(SynchronizeError.Type.DATA_ACCESS_ERROR, "Data access error in the list accessor implementation");
                    return State.ERROR;
                }

            case WAITING_FOR_BYTE_ARRAY_RESOURCE_STORE_NAME:
                return elementTransferSynchClient.receivedByteArrayResourceStoreName(data);

            default:
                // unexpected data
                synchronizeError = new Duple<>(SynchronizeError.Type.ERROR_IN_PROTOCOL, "Unexpected byte[] data at state " + state);
                return State.ERROR;
        }
    }

    static Duple<SynchronizeError.Type, String> processDeniedRequest(ListSynchronizerServerFSM.InitialRequestAnswerType initialRequestAnswerType) {
        switch (initialRequestAnswerType) {

            case INVALID_REQUEST_FORMAT:
                return new Duple<>(SynchronizeError.Type.ERROR_IN_PROTOCOL, "The synchronizing algorithm failed in the initial request!");

            case UNKNOWN_LIST:
                return new Duple<>(SynchronizeError.Type.UNKNOWN_LIST, "The requested list does not exist at the server");

            case INVALID_LEVEL:
                return new Duple<>(SynchronizeError.Type.INVALID_LEVEL, "The requested level is incorrect at the server");

            case ELEMENT_NOT_FOUND:
                return new Duple<>(SynchronizeError.Type.ELEMENT_NOT_FOUND, "The requested element was not found in the list");

            case REQUEST_DENIED:
                return new Duple<>(SynchronizeError.Type.REQUEST_DENIED, "The request has been denied due to lack of permission");

            case DIFFERENT_LISTS_CONFIG:
                return new Duple<>(SynchronizeError.Type.DIFFERENT_LISTS_CONFIG, "The requested list has different configuration in client and server");

            default:
                return new Duple<>(SynchronizeError.Type.ERROR_IN_PROTOCOL, "Unexpected server answer for the initial request!");
        }
    }

    @Override
    public State init(ChannelConnectionPoint ccp) {
        // submit the initial request, where we specify which list we want to synchronize, and with what parameters
        try {
            Duple<ListAccessor, Integer> listAndLevel = ListSynchronizerManager.getListForWriting(listContainer, serverPeerID, listPath);
            listAccessor = listAndLevel.element1;
            level = listAndLevel.element2;
            if (checkListAndLevel(listAccessor, level)) {
                SynchRequest synchRequest;
                // the request for the synch of a full list of elements consists on the list path and our own PeerID
                synchRequest = new SynchRequest(listSynchronizer.getOwnPeerID(), listPath, listAccessor);
                ccp.write(outgoingChannel, Serializer.serializeObject(synchRequest));
                return State.WAITING_FOR_REQUEST_ANSWER;
            } else {
                // incorrect level requested
                synchronizeError = generateIncorrectLevelError(listPath, level);
                return State.ERROR;
            }
        } catch (ListNotFoundException e) {
            synchronizeError = generateUnknownListError(listPath);
            return State.ERROR;
        }
    }

    static boolean checkListAndLevel(ListAccessor listAccessor, int level) {
        return level >= 0 && level < listAccessor.getLevelCount();
    }

    static Duple<SynchronizeError.Type, String> generateUnknownListError(ListPath listPath) {
        if (listPath.isMainList()) {
            return new Duple<>(SynchronizeError.Type.UNKNOWN_LIST, "Unknown list: " + listPath.mainList);
        } else {
            return new Duple<>(SynchronizeError.Type.UNKNOWN_LIST, "Unknown inner list");
        }
    }

    static Duple<SynchronizeError.Type, String> generateIncorrectLevelError(ListPath listPath, int level) {
        if (listPath.isMainList()) {
            return new Duple<>(SynchronizeError.Type.INVALID_LEVEL, "Incorrect level for list: " + listPath.mainList + "/" + level);
        } else {
            return new Duple<>(SynchronizeError.Type.INVALID_LEVEL, "Incorrect level for inner list");
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
                // nothing to notify here, as the synch process continues somewhere else
                return true;

            case ERROR:
                if (listAccessor != null) {
                    listAccessor.endSynchProcess(ListAccessor.Mode.CLIENT, false);
                }
                reportError(progress, synchronizeError.element1, synchronizeError.element2);
                return true;

            case ERROR_ELEMENT_CHANGED_IN_SERVER:
                listAccessor.endSynchProcess(ListAccessor.Mode.CLIENT, false);
                reportError(progress, SynchronizeError.Type.ELEMENT_CHANGED_IN_SERVER, null);
                return true;
        }
        return false;
    }

    @Override
    public void disconnected(ChannelConnectionPoint ccp) {
        // we got disconnected from the other peer before the synch finished -> notify as an error
        if (listAccessor != null) {
            listAccessor.endSynchProcess(ListAccessor.Mode.CLIENT, false);
        }
        if (progress != null) {
            progress.error(new SynchronizeError(SynchronizeError.Type.DISCONNECTED, null));
        }
    }

    @Override
    public void timedOut(State state) {
        if (listAccessor != null) {
            listAccessor.endSynchProcess(ListAccessor.Mode.CLIENT, false);
        }
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
        synchronizeError = new Duple<>(SynchronizeError.Type.SERVER_BUSY, null);
    }

    static void reportError(ProgressNotificationWithError<Integer, SynchronizeError> progress, SynchronizeError.Type type) {
        reportError(progress, type, null);
    }

    static void reportError(ProgressNotificationWithError<Integer, SynchronizeError> progress, SynchronizeError.Type type, String error) {
        if (progress != null) {
            progress.error(new SynchronizeError(type, error));
        }
    }

    /**
     * Retrieves a hash of a list accessor configuration. This includes the number of levels, the transmission types, and all things which
     * allow deciding if two lists are the "same"
     *
     * @param listAccessor list accessor to evaluate
     * @return the hash of the list accessor configuration
     */
    static byte[] getListConfigHash(ListAccessor listAccessor) {
        SHA_256 sha_256 = new SHA_256();
        int levelCount = listAccessor.getLevelCount();
        sha_256.update(levelCount);
        for (int oneLevel = 0; oneLevel < levelCount; oneLevel++) {
            sha_256.update(listAccessor.hashEqualsElement(oneLevel));
            ListAccessor.TransmissionType transmissionType = listAccessor.getTransmissionType(oneLevel);
            if (transmissionType != null) {
                sha_256.update(listAccessor.getTransmissionType(oneLevel));
                if (transmissionType == ListAccessor.TransmissionType.INNER_LISTS) {
                    sha_256.update(listAccessor.getInnerListLevels(oneLevel));
                }
            }
        }
        return sha_256.digest();
    }
}
