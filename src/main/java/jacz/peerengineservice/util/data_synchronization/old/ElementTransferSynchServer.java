package jacz.peerengineservice.util.data_synchronization.old;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.util.datatransfer.ResourceStore;
import jacz.peerengineservice.util.datatransfer.ResourceStoreResponse;
import jacz.peerengineservice.util.datatransfer.resource_accession.ByteArrayReader;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceReader;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.util.identifier.UniqueIdentifierFactory;
import jacz.util.io.object_serialization.MutableOffset;
import jacz.util.io.object_serialization.Serializer;
import jacz.util.notification.ProgressNotificationWithError;
import jacz.util.numeric.NumericUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic methods for serving elements upon hash requests
 */
class ElementTransferSynchServer<S> {


    interface State<S> {

        /**
         * @return State for expecting requests for object elements
         */
        S expectObjectRequestsState();

        /**
         * @return State for expecting from the client additional indexes that he will later request us
         */
        S waitingForIndexesToServe();

        /**
         * @return State for finished sending all elements. This can happen if:
         *         - there were no elements to transfer from the start, so no requests nor notifications will come from the client
         *         - we received the notification from the client indicating that no more object requests will be issued
         */
        S finishedTransfer();

        /**
         * @return State for indicating that there are elements to send, but this are of byte array type. The resource store for serving the
         *         data was set up, and the client was notified that he can already request the data, so there is nothing else to do
         *         This state also indicates that the resource store must be removed after a while
         */
        S finishedTransferRemoveResourceStore();
    }


    static class IndexAndObject implements Serializable {

        final String index;

        final Object object;

        IndexAndObject(String index, Object object) {
            this.index = index;
            this.object = object;
        }
    }

    private static class ByteArrayTransmissionResourceStore implements ResourceStore {

        private final ListAccessor listAccessor;

        private final int level;

        private final List<String> indexList;

        private boolean resourceStillAvailable;

        private boolean resourceAlreadyRequested;

        private ProgressNotificationWithError<Integer, SynchronizeError> progress;

        private ByteArrayTransmissionResourceStore(ListAccessor listAccessor, int level, List<String> indexList, ProgressNotificationWithError<Integer, SynchronizeError> progress) {
            this.listAccessor = listAccessor;
            this.level = level;
            this.indexList = indexList;
            resourceStillAvailable = true;
            resourceAlreadyRequested = false;
            this.progress = progress;
        }

        private synchronized void issueTimeOut() {
            // the timeout has been issued. If we have not received the client's request yet, deny future requests
            resourceStillAvailable = false;
            if (!resourceAlreadyRequested && progress != null) {
                progress.timeout();
            }
        }

        @Override
        public synchronized ResourceStoreResponse requestResource(PeerID peerID, String resourceID) {
            // the resource id value does not matter, since any id is ok for this resource store. We only need to provide the correct resource store
            if (resourceStillAvailable) {
                resourceAlreadyRequested = true;
                ResourceReader resourceReader = new ByteArrayReader(listAccessor, level, indexList, progress);
                return ResourceStoreResponse.resourceApproved(resourceReader);
            } else {
                return ResourceStoreResponse.requestDenied();
            }
        }
    }

    static final class ElementNotFoundMessage implements Serializable {
    }


    private final ListAccessor listAccessor;

    private final int level;

    private final ListAccessor.TransmissionType transmissionType;

    /**
     * Indexes that the client should request to us. The client is already aware of this, since the ordered list synch client sent it
     * todo remove, the client must send us the list in case of byte array
     */
    private final List<String> indexesToServe;

    private final ProgressNotificationWithError<Integer, SynchronizeError> progress;

    private final ChannelConnectionPoint ccp;

    private final byte outgoingChannel;

    private final PeerClient peerClient;

    private final State<S> state;

    private final int hashesToSendCount;

    private int sentHashesCount;

    private String byteArrayResourceStoreName;

    private ByteArrayTransmissionResourceStore byteArrayResourceStore;


    ElementTransferSynchServer(
            ListAccessor listAccessor,
            int level,
            ProgressNotificationWithError<Integer, SynchronizeError> progress,
            ChannelConnectionPoint ccp,
            byte outgoingChannel,
            PeerClient peerClient,
            State<S> state,
            int hashesToSendCount) {
        this.listAccessor = listAccessor;
        this.level = level;
        this.transmissionType = listAccessor.getTransmissionType(level);
        this.indexesToServe = new ArrayList<>();
        this.progress = progress;
        this.ccp = ccp;
        this.outgoingChannel = outgoingChannel;
        this.peerClient = peerClient;
        this.state = state;
        this.hashesToSendCount = hashesToSendCount;
        sentHashesCount = 0;
    }

    S initiateDataTransferProcess() {

        if (hashesToSendCount == 0) {
            // there are no elements to send. The client will not send any request -> finish!
            return state.finishedTransfer();
        }
        switch (transmissionType) {

            case OBJECT:
                return state.expectObjectRequestsState();

            case BYTE_ARRAY:
                // todo first wait for indexes to serve
//                byteArrayResourceStoreName = generateResourceStoreName();
//                byteArrayResourceStore = new ByteArrayTransmissionResourceStore(listAccessor, level, indexesToServe, progress);
//                peerClient.addLocalResourceStore(byteArrayResourceStoreName, byteArrayResourceStore);
//                // notify the client that he can now initiate the download process (we send the resource store name)
//                ccp.write(outgoingChannel, Serializer.serialize(byteArrayResourceStoreName));
//                return state.finishedTransferRemoveResourceStore();
                return state.waitingForIndexesToServe();

            case INNER_LISTS:
                return state.finishedTransfer();

            default:
                return null;
        }
    }

    public S newIndexesToServe(byte[] data) {
        List<String> receivedIndexesToServe = OrderedListSynchServer.deserializeElements(data);
        if (!receivedIndexesToServe.isEmpty()) {
            indexesToServe.addAll(receivedIndexesToServe);
            return state.waitingForIndexesToServe();
        } else {
            // finished receiving indexes
            if (indexesToServe.size() == 0) {
                // there is nothing to send -> finish
                return state.finishedTransfer();
            } else {
                byteArrayResourceStoreName = generateResourceStoreName();
                byteArrayResourceStore = new ByteArrayTransmissionResourceStore(listAccessor, level, indexesToServe, progress);
                peerClient.addLocalResourceStore(byteArrayResourceStoreName, byteArrayResourceStore);
                // notify the client that he can now initiate the download process (we send the resource store name)
                ccp.write(outgoingChannel, Serializer.serialize(byteArrayResourceStoreName));
                return state.finishedTransferRemoveResourceStore();
            }
        }
    }


    S transferObject(byte[] data) throws DataAccessException {
        MutableOffset offset = new MutableOffset();
        boolean requiresElement = Serializer.deserializeBoolean(data, offset);
        if (requiresElement) {
            String requestedIndex = Serializer.deserializeString(data, offset);
            if (requestedIndex.isEmpty()) {
                // an empty index indicates that there are no more requests
                return state.finishedTransfer();
            } else {
                try {
                    // requests for hashes only happens in object transmission, no need to check transmission type again
                    IndexAndObject indexAndObject = new IndexAndObject(requestedIndex, listAccessor.getElementObject(requestedIndex, level));
                    ccp.write(outgoingChannel, indexAndObject);
                } catch (ElementNotFoundException e) {
                    // send message indicating that we no longer have this element (the process continues ok)
                    ccp.write(outgoingChannel, new ElementNotFoundMessage());
                }
            }
        }
        increaseSentElementsAndUpdateProgress();
        return state.expectObjectRequestsState();
    }

    private void increaseSentElementsAndUpdateProgress() {
        sentHashesCount++;
        if (progress != null) {
            progress.addNotification(NumericUtil.displaceInRange(sentHashesCount, 0, hashesToSendCount, 0, ListSynchronizerManager.PROGRESS_MAX));
        }
    }

    void removeResourceStore() {
        byteArrayResourceStore.issueTimeOut();
        peerClient.removeLocalResourceStore(byteArrayResourceStoreName);
    }

    static String generateResourceStoreName() {
        return PeerClient.OWN_CUSTOM_PREFIX + "listSyncStore_" + UniqueIdentifierFactory.getOneStaticIdentifier().toString();
    }
}
