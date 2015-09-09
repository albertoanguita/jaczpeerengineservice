package jacz.peerengine.util.data_synchronization.old;

import jacz.peerengine.PeerID;
import jacz.peerengine.client.PeerClient;
import jacz.peerengine.util.datatransfer.DownloadProgressNotificationHandler;
import jacz.peerengine.util.datatransfer.master.DownloadManager;
import jacz.peerengine.util.datatransfer.master.ProviderStatistics;
import jacz.peerengine.util.datatransfer.master.ResourcePart;
import jacz.peerengine.util.datatransfer.master.Statistics;
import jacz.peerengine.util.datatransfer.resource_accession.ResourceWriter;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.util.io.object_serialization.FragmentedByteArray;
import jacz.util.io.object_serialization.MutableOffset;
import jacz.util.io.object_serialization.Serializer;
import jacz.util.notification.ProgressNotificationWithError;
import jacz.util.numeric.LongRange;
import jacz.util.numeric.NumericUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic methods for requesting elements by hash
 */
class ElementTransferSynchClient<S> implements ProgressNotificationWithError<Integer, String> {

    interface State<S> {

        /**
         * @return State for expecting objects from the server
         */
        S expectObjectsState();

        /**
         * @return State for expecting the name of the resource store to use for downloading the byte array data (which also indicates that
         *         the resource store has been set up in the server)
         */
        S expectByteArrayResourceStoreName();

        /**
         * @return State for all elements received, and complete must be notified to the progress object. This can happen if:
         *         - the list of elements to request is empty
         *         - the hashes were equal to the elements, so we do not need to request the elements
         *         - we received the last object element
         */
        S finishedTransfer();

        /**
         * @return State when the transmission type is byte array. In this case elements are requested through a normal download. The download
         *         handler will notify the completion, so we must finish without notifying it
         */
        S finishedTransferNotNotifyComplete();

        /**
         * @return State when the transmission type is byte array. In this case elements are requested through a normal download. The download
         *         handler will notify the completion, so we must finish without notifying it
         */
        S finishedTransferSomeElementChangedInServer();
    }


    /**
     * Private implementation of a DownloadProgressNotificationHandler, used for controlling events and progress of byte array items downloads
     */
    private class DownloadProgressNotificationHandlerImpl implements DownloadProgressNotificationHandler {

        /**
         * Global progress to which progress notifications at this level must be notified
         */
        private final ProgressNotificationWithError<Integer, String> progress;

        private Long totalLength;

        private Statistics statistics;

        private DownloadProgressNotificationHandlerImpl(ProgressNotificationWithError<Integer, String> progress) {
            this.progress = progress;
            totalLength = null;
        }

        @Override
        public void started(String resourceID, String storeName, DownloadManager downloadManager) {
            statistics = downloadManager.getStatistics();
        }

        @Override
        public void resourceSize(String resourceID, String storeName, DownloadManager downloadManager, long resourceSize) {
            totalLength = resourceSize;
        }

        @Override
        public void providerAdded(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, String providerId) {
            // ignore
        }

        @Override
        public void providerRemoved(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, String providerId) {
            // out only provider has been removed (either for disconnection, or for timeout) -> cancel download so the error is notified
            downloadManager.cancel();
        }

        @Override
        public void providerReportedSharedPart(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, ResourcePart sharedPart) {
            // ignore
        }

        @Override
        public void providerWasAssignedSegment(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, LongRange assignedSegment) {
            // ignore
        }

        @Override
        public void providerWasClearedAssignation(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager) {
            // ignore
        }

        @Override
        public void paused(String resourceID, String storeName, DownloadManager downloadManager) {
            // ignore
        }

        @Override
        public void resumed(String resourceID, String storeName, DownloadManager downloadManager) {
            // ignore
        }

        @Override
        public void downloadedSegment(String resourceID, String storeName, LongRange segment, DownloadManager downloadManager) {
            // report progress
            progress.addNotification((int) NumericUtil.displaceInRange(statistics.getDownloadedSizeThisResource(), 0, totalLength, 0, ListSynchronizerManager.PROGRESS_MAX));
        }

        @Override
        public void successIntermediateHash(String resourceID, String storeName, LongRange range, DownloadManager downloadManager) {
            // ignore
        }

        @Override
        public void failedIntermediateHash(String resourceID, String storeName, LongRange range, DownloadManager downloadManager) {
            // ignore
        }

        @Override
        public void invalidIntermediateHashAlgorithm(String resourceID, String storeName, LongRange range, String hashAlgorithm, DownloadManager downloadManager) {
            // ignore
        }

        @Override
        public void checkingTotalHash(String resourceID, String storeName, int percentage, DownloadManager downloadManager) {
            // ignore
        }

        @Override
        public void successTotalHash(String resourceID, String storeName, DownloadManager downloadManager) {
            // ignore
        }

        @Override
        public void failedTotalHash(String resourceID, String storeName, DownloadManager downloadManager) {
            // ignore
        }

        @Override
        public void invalidTotalHashAlgorithm(String resourceID, String storeName, String hashAlgorithm, DownloadManager downloadManager) {
            // ignore
        }

        @Override
        public void completed(String resourceID, String storeName, ResourceWriter resourceWriter, DownloadManager downloadManager) {
            // the byte array items download was successfully completed
            progress.completeTask();
        }

        @Override
        public void cancelled(String resourceID, String storeName, CancellationReason reason, DownloadManager downloadManager) {
            // the download of the byte array items failed (several possible reasons) -> notify error
            progress.error("Byte array item download failed");
        }

        @Override
        public void stopped(String resourceID, String storeName, DownloadManager downloadManager) {
            // ignore
        }
    }

    /**
     * PeerClient for downloading byte array resources
     */
    private final PeerClient peerClient;

    private final ListAccessor listAccessor;

    private final int level;

    private final ListAccessor.TransmissionType transmissionType;

    private final ProgressNotificationWithError<Integer, SynchronizeError> progress;

    private final ChannelConnectionPoint ccp;

    private final byte outgoingChannel;

    private final State<S> state;

    private final PeerID serverPeerID;

    private final ListSynchronizerManager listSynchronizerManager;

    /**
     * List of indexes and hashes to request to the server. This is actually only used if the hashes equal the elements, or if the
     * transmission type is object.
     * For byte array transmissions, the server will send the index values inside the data
     * (in object transmissions, the index is also attached to the transmitted object, to make it simpler)
     */
    private final List<IndexAndHash> indexAndHashesToRequest;

    private int hashesToRequestCount;

    private int requestsAchieved;

    private boolean someElementsWereChangedInServer;


    ElementTransferSynchClient(
            PeerClient peerClient,
            ListAccessor listAccessor,
            int level,
            ProgressNotificationWithError<Integer, SynchronizeError> progress,
            ChannelConnectionPoint ccp,
            byte outgoingChannel,
            State<S> state,
            PeerID serverPeerID,
            ListSynchronizerManager listSynchronizerManager,
            List<IndexAndHash> indexAndHashesToRequest,
            int hashesToRequestCount,
            int requestsAchieved) {
        this.peerClient = peerClient;
        this.listAccessor = listAccessor;
        this.level = level;
        transmissionType = listAccessor.getTransmissionType(level);
        this.progress = progress;
        this.ccp = ccp;
        this.outgoingChannel = outgoingChannel;
        this.state = state;
        this.serverPeerID = serverPeerID;
        this.listSynchronizerManager = listSynchronizerManager;
        this.indexAndHashesToRequest = indexAndHashesToRequest;
        this.hashesToRequestCount = hashesToRequestCount;
        this.requestsAchieved = requestsAchieved;
        someElementsWereChangedInServer = false;
    }

    public S initiateDataTransferProcess() throws DataAccessException {

        if (indexAndHashesToRequest.isEmpty()) {
            // there are no elements to request -> notify as complete!
            return state.finishedTransfer();
        }

        if (listAccessor.hashEqualsElement(level)) {
            // if the hashes are the elements themselves, there is nothing left to do, we are finished!
            // the server is not notified, since he also knows this
            for (IndexAndHash indexAndHash : indexAndHashesToRequest) {
                listAccessor.addElementAsObject(indexAndHash.index, level, indexAndHash.hash);
                addProgress();
            }
            requestsAchieved = hashesToRequestCount;
            return state.finishedTransfer();
        }

        switch (transmissionType) {

            case OBJECT:
                // we start requesting object elements
                return requestObjectItem();

            case BYTE_ARRAY:
                // first send the list of indexes to request. Use the code from the ordered list synch client
                // then we must wait for the server to send us the name of the resource store to use to request the elements
                sendIndexesToRequest();
                return state.expectByteArrayResourceStoreName();

            case INNER_LISTS:
                List<String> indexOfInnerLists = new ArrayList<>();
                for (IndexAndHash indexAndHash : indexAndHashesToRequest) {
                    indexOfInnerLists.add(indexAndHash.index);
                }
                listSynchronizerManager.currentTaskIsInnerList(indexOfInnerLists, listAccessor.getInnerListLevels(level));
                return state.finishedTransferNotNotifyComplete();

            default:
                return null;
        }
    }

    private void sendIndexesToRequest() {
        int elementsSent = 0;
        int elementsToSendCount = indexAndHashesToRequest.size();
        while (elementsSent < elementsToSendCount) {
            int elementsToSendThisRound;
            if (elementsToSendCount - elementsSent <= OrderedListSynchClient.MAXIMUM_COUNT_SENT_ELEMENTS) {
                elementsToSendThisRound = elementsToSendCount - elementsSent;
            } else {
                elementsToSendThisRound = OrderedListSynchClient.MINIMUM_COUNT_SENT_ELEMENTS;
            }
            ccp.write(outgoingChannel, serializeHashesToSend(elementsSent, elementsToSendThisRound), false);
            elementsSent += elementsToSendThisRound;
        }
        ccp.write(outgoingChannel, Serializer.serialize(0), true);
    }

    private byte[] serializeHashesToSend(int from, int count) {
        FragmentedByteArray fragmentedByteArray = new FragmentedByteArray();
        fragmentedByteArray.addArrays(Serializer.serialize(count));
        for (int elementPos = from; elementPos < from + count; elementPos++) {
            String hash = indexAndHashesToRequest.get(elementPos).index;
            fragmentedByteArray.addArrays(Serializer.serialize(hash));
        }
        return fragmentedByteArray.generateArray();
    }


    public S receiveObjectElement(Object message) throws DataAccessException {
        if (message instanceof ElementTransferSynchServer.ElementNotFoundMessage) {
            // the requested element was not found at the server
            someElementsWereChangedInServer = true;
            addProgress();
            return requestObjectItem();
        } else {
            ElementTransferSynchServer.IndexAndObject indexAndObject = (ElementTransferSynchServer.IndexAndObject) message;
            listAccessor.addElementAsObject(indexAndObject.index, level, indexAndObject.object);
            addProgress();
            return requestObjectItem();
        }
    }

    private S requestObjectItem() {
        if (indexAndHashesToRequest.size() > 0) {
            // request the first element
            IndexAndHash indexAndHash = indexAndHashesToRequest.remove(0);
            byte[] data = Serializer.addArrays(Serializer.serialize(true), Serializer.serialize(indexAndHash.index));
            ccp.write(outgoingChannel, data);
            return state.expectObjectsState();
        } else {
            // request complete --> finish (send a true and an empty index)
            ccp.write(outgoingChannel, Serializer.addArrays(Serializer.serialize(true), Serializer.serialize("")));
            if (!someElementsWereChangedInServer) {
                return state.finishedTransfer();
            } else {
                return state.finishedTransferSomeElementChangedInServer();
            }
        }
    }

    public S receivedByteArrayResourceStoreName(byte[] data) {
        // set up download of byte array items through a download resources using the byte array writer
        // the resource id value does not matter, since any id is ok for this resource store. We only need to provide the correct resource store
        // download streaming need is always 1 because we need the data in order
        String byteArrayResourceStoreName = Serializer.deserializeString(data, new MutableOffset());
        peerClient.downloadResource(serverPeerID, byteArrayResourceStoreName, "", new ByteArrayWriter(listAccessor, level), new DownloadProgressNotificationHandlerImpl(this), 1.0d, null, null, null);
        return state.finishedTransferNotNotifyComplete();
    }

    private void addProgress() {
        requestsAchieved++;
        reportProgress(progress, hashesToRequestCount, requestsAchieved);
    }

    static void reportProgress(ProgressNotificationWithError<Integer, SynchronizeError> progress, int totalElementCount, int requestsAchieved) {
        if (progress != null) {
            progress.addNotification(NumericUtil.displaceInRange(requestsAchieved, 0, totalElementCount, 0, ListSynchronizerManager.PROGRESS_MAX));
        }
    }

    @Override
    public void addNotification(Integer message) {
        // value from 0 to PROGRESS_MAX of the current task
        if (progress != null) {
            progress.addNotification(message);
        }
    }

    @Override
    public void completeTask() {
        // current task complete
        listAccessor.endSynchProcess(ListAccessor.Mode.CLIENT, true);
        if (progress != null) {
            progress.completeTask();
        }
    }

    @Override
    public void error(String error) {
        listAccessor.endSynchProcess(ListAccessor.Mode.CLIENT, false);
        ListSynchronizerClientFSM.reportError(progress, SynchronizeError.Type.DATA_TRANSFER_FAILED, error);
    }

    @Override
    public void timeout() {
        listAccessor.endSynchProcess(ListAccessor.Mode.CLIENT, false);
        if (progress != null) {
            progress.timeout();
        }
    }
}
