package jacz.peerengine.util.data_synchronization.old;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.util.concurrency.ConcurrencyUtil;
import jacz.util.concurrency.concurrency_controller.ConcurrencyControllerReadWrite;
import jacz.util.concurrency.concurrency_controller.ConcurrencyControllerReadWriteBasic;
import jacz.util.hash.MD5;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.identifier.UniqueIdentifierFactory;
import jacz.util.io.object_serialization.FragmentedByteArray;
import jacz.util.io.object_serialization.MutableOffset;
import jacz.util.io.object_serialization.Serializer;
import jacz.util.lists.Lists;
import jacz.util.numeric.NumericUtil;

import java.io.Serializable;
import java.util.*;

/**
 * Generic methods for acting as a client in an ordered list synch process (launches queries)
 * <p/>
 * This class acts as a mini-FSM for synchronizing the list of hashes. The first time, the initialSynchProcess method must be invoked, for
 * getting the first state
 */
class OrderedListSynchClient<S> {

    interface State<S> {

        /**
         * @return State for performing the hash synch
         */
        S synchronizingState();

        /**
         * @return State for finished hash synch. The transferred hashes to the server are different from the elements themselves, so a
         *         data transfer phase is now required. The elements to send can be retrieved with getElementsToSend()
         *         This can happen even if there are no elements to send
         *         The elements have been sent to the other end
         *         The user must set up a transfer process, and move from this state to the next one immediately
         */
        S finishedSynchMustTransferState();

        /**
         * @return State for finished hash synch. The transferred hashes to the server are identical to the elements themselves, so a
         *         data transfer phase is not required. The other side knows this
         *         This can happen even if there are no elements to send
         *         The elements have been sent to the other end
         */
        S finishedSynchSkipTransfer();

        /**
         * @return State for error in the process
         */
        S errorState();
    }

    /**
     * A query submitted from a synch server to a synch client in an attempt to find out which hashes of a list the client has.
     * <p/>
     * A query is composed of a unique identifier, and some elements representing a list of hash values. These elements are:
     * <p/>
     * - The first hash value of the list
     * - The number of values in the list (length)
     * - The hash of the complete list
     * <p/>
     * The identifier allows correlating answers to queries, since the client will respond with an answer containing the same id, and several
     * queries are submitted simultaneously
     * <p/>
     * This class can also be used to notify the client that the process for synchronizing hashes has finished (querySynchronizationComplete)
     */
    static class HashQuery implements Serializable {

        UniqueIdentifier id;

        String firstHash;

        int length;

        String listHash;

        boolean querySynchronizationComplete;

        HashQuery(String firstHash, int length, String listHash) {
            id = UniqueIdentifierFactory.getOneStaticIdentifier();
            this.firstHash = firstHash;
            this.length = length;
            this.listHash = listHash;
            querySynchronizationComplete = false;
        }

        protected HashQuery(UniqueIdentifier id, String firstHash, int length, String listHash) {
            this.id = id;
            this.firstHash = firstHash;
            this.length = length;
            this.listHash = listHash;
            querySynchronizationComplete = false;
        }

        HashQuery(byte[] data) {
            MutableOffset offset = new MutableOffset();
            boolean hasID = Serializer.deserializeBoolean(data, offset);
            if (hasID) {
                id = UniqueIdentifier.deserializeWithOffset(data, offset);
            }
            boolean hasFirstHash = Serializer.deserializeBoolean(data, offset);
            if (hasFirstHash) {
                firstHash = Serializer.deserializeString(data, offset);
            }
            length = Serializer.deserializeInt(data, offset);
            boolean hasListHash = Serializer.deserializeBoolean(data, offset);
            if (hasListHash) {
                listHash = Serializer.deserializeString(data, offset);
            }
            querySynchronizationComplete = Serializer.deserializeBoolean(data, offset);
        }

        HashQuery(boolean querySynchronizationComplete) {
            this.querySynchronizationComplete = querySynchronizationComplete;
        }

        byte[] serialize() {
            byte[] data;
            if (id != null) {
                data = Serializer.addArrays(Serializer.serialize(true), id.serialize());
            } else {
                data = Serializer.serialize(false);
            }
            if (firstHash != null) {
                data = Serializer.addArrays(data, Serializer.serialize(true), Serializer.serialize(firstHash));
            } else {
                data = Serializer.addArrays(data, Serializer.serialize(false));
            }
            data = Serializer.addArrays(data, Serializer.serialize(length));
            if (listHash != null) {
                data = Serializer.addArrays(data, Serializer.serialize(true), Serializer.serialize(listHash));
            } else {
                data = Serializer.addArrays(data, Serializer.serialize(false));
            }
            data = Serializer.addArrays(data, Serializer.serialize(querySynchronizationComplete));
            return data;
        }
    }


    private static class LocalHashQuery extends HashQuery {

        int offset;

        LocalHashQuery(int offset, String firstHash, int length, String listHash) {
            super(firstHash, length, listHash);
            this.offset = offset;
        }

        HashQuery getNonLocalHashQuery() {
            return new HashQuery(id, firstHash, length, listHash);
        }
    }

    private static class HashQueryTable {

        private Map<UniqueIdentifier, LocalHashQuery> map;

        HashQueryTable() {
            map = new HashMap<>();
        }

        synchronized void put(LocalHashQuery hashQuery) {
            map.put(hashQuery.id, hashQuery);
        }

        synchronized LocalHashQuery get(UniqueIdentifier id) {
            return map.get(id);
        }

        synchronized void remove(UniqueIdentifier id) {
            if (map.containsKey(id)) {
                map.remove(id);
            }
        }

        synchronized boolean isEmpty() {
            return map.isEmpty();
        }
    }

    private static final int MAXIMUM_SIMULTANEOUS_QUERIES = 16;

    static final int MINIMUM_COUNT_SENT_ELEMENTS = 100;

    static final int MAXIMUM_COUNT_SENT_ELEMENTS = 150;

    /**
     * List of the hash values themselves that we own, ordered from lesser to greater
     */
    private final List<String> elementList;

    /**
     * List of the indexes of the elements that the client does not have (he has recognized he does not own their
     * hashes) and therefore must be sent to the client
     */
    private final List<Integer> elementsToSend;

    private final ConcurrencyControllerReadWriteBasic concurrencyController;

    private final ChannelConnectionPoint ccp;

    private final byte outgoingChannel;

    private final boolean canSkipTransfer;

    private final State<S> state;

    /**
     * Table storing copies of the hash queries sent to the client and not yet answered. Hash queries are uniquely identified, so that we can
     * find the query corresponding to an answer
     */
    private final HashQueryTable hashQueryTable;

    OrderedListSynchClient(List<String> elementList, ChannelConnectionPoint ccp, byte outgoingChannel, boolean canSkipTransfer, State<S> state) {
        this.elementList = elementList;
        Lists.sort(this.elementList, false, ConcurrencyUtil.threadCount(0.75d));
        this.elementsToSend = new ArrayList<>();
        concurrencyController = new ConcurrencyControllerReadWriteBasic(MAXIMUM_SIMULTANEOUS_QUERIES);
        this.hashQueryTable = new HashQueryTable();
        this.ccp = ccp;
        this.outgoingChannel = outgoingChannel;
        this.canSkipTransfer = canSkipTransfer;
        this.state = state;
    }

    List<String> getElementsToSend() {
        List<String> elementsToTransmit = new ArrayList<>(elementsToSend.size());
        for (Integer indexOfHash : elementsToSend) {
            elementsToTransmit.add(elementList.get(indexOfHash));
        }
        return elementsToTransmit;
    }

    S initialSynchProcess() {
        // initialize the related private fields (sorted hashList, hashQueryTable, hashesToSend) and submit the initial
        // query if we have an empty initial hash list, finish this process as no hash must be sent (and all at the
        // client end must be erased)
        try {
            if (elementList.isEmpty()) {
                // hash synch process complete, send confirmation. Plus, there are no data to transfer --> FINISH
                return sendHashQueryProcessEndMessage();
            } else {
                // create the initial hash query and send it
                LocalHashQuery localHashQuery = new LocalHashQuery(0, elementList.get(0), elementList.size(), calculateListHash(elementList, 0, elementList.size()));
                hashQueryTable.put(localHashQuery);
                write(localHashQuery.getNonLocalHashQuery().serialize(), true);
                return state.synchronizingState();
            }
        } catch (Exception e) {
            // should never happen, already checked
            return state.errorState();
        }
    }

    S synchProcess(byte[] data) {
        // a hash query has been resolved, notify the concurrency controller so other can be sent
        concurrencyController.endActivity(ConcurrencyControllerReadWrite.READ_ACTIVITY);
        OrderedListSynchServer.HashQueryResult result = new OrderedListSynchServer.HashQueryResult(data);
        LocalHashQuery query = hashQueryTable.get(result.id);
        switch (result.value) {

            case HASH_NOT_FOUND_AND_NO_GREATER:
                // the client did not have any hash in the query, mark all hashes in the query to be sent. No need to keep searching in this list
                for (Integer index = query.offset; index < query.offset + query.length; index++) {
                    elementsToSend.add(index);
                }
                break;
            case HASH_NOT_FOUND_BUT_GREATER:
                // mark first hash to send and search in rest
                elementsToSend.add(query.offset);
                searchHashInRestOfList(query);
                break;
            case HASH_FOUND_LIST_HASH_DIFFERS:
                // do not mark first hash to be sent, but search in rest
                searchHashInRestOfList(query);
                break;
            case HASH_FOUND_LIST_HASH_EQUALS:
                // the client has all the hashes from the query. Nothing to do here for the server, no further search required in this list...
                break;
        }
        hashQueryTable.remove(result.id);
        // check if we have finished searching...
        if (hashQueryTable.isEmpty()) {
            return sendHashQueryProcessEndMessage();
        } else {
            return state.synchronizingState();
        }
    }

    private void searchHashInRestOfList(LocalHashQuery oldQuery) {
        // the old query produces two new queries, which are sent again
        Set<LocalHashQuery> newQueries = generateQueriesForRestOfList(oldQuery);
        for (LocalHashQuery newQuery : newQueries) {
            hashQueryTable.put(newQuery);
            write(newQuery.getNonLocalHashQuery().serialize(), true);
        }
    }

    private Set<LocalHashQuery> generateQueriesForRestOfList(LocalHashQuery oldQuery) {
        // we divide the rest of the old query list in two sub-lists
        // in case that the underlying list supports repeated elements, we avoid cutting series of repetitions
        // e.g. if the rest of the list contains the elements {1, 2, 3, 3, 3, 3, 4, 5}, the initial cut would be {1, 2, 3, 3} and {3, 3, 4, 5}
        // we shift elements to the second list until we don't cut any series, or until the first list is empty
        List<Integer> subLengths = NumericUtil.divide(oldQuery.length - 1, 2);
        while (subLengths.get(0) > 0 && elementList.get(oldQuery.offset + subLengths.get(0)).equals(elementList.get(oldQuery.offset + subLengths.get(0)) + 1)) {
            subLengths.set(0, subLengths.get(0) - 1);
            subLengths.set(1, subLengths.get(1) + 1);
        }
        Set<LocalHashQuery> newQueries = new HashSet<>();
        if (subLengths.get(0) > 0) {
            int offset = oldQuery.offset + 1;
            int length = subLengths.get(0);
            newQueries.add(new LocalHashQuery(offset, elementList.get(offset), length, calculateListHash(elementList, offset, length)));
        }
        if (subLengths.get(1) > 0) {
            int offset = oldQuery.offset + 1 + subLengths.get(0);
            int length = subLengths.get(1);
            newQueries.add(new LocalHashQuery(offset, elementList.get(offset), length, calculateListHash(elementList, offset, length)));
        }
        return newQueries;
    }


    private S sendHashQueryProcessEndMessage() {
        // it also sends the list of hashes to send. This is a single message with the list of the hashes
        // (first an integer saying how many hashes there are, then the hashes serialized with the assistant in util)
        write(new HashQuery(true).serialize(), false, false);
//        write(Serializer.serialize(elementsToSend.size()), false, false);
        writeElementsToSend();
        if (canSkipTransfer) {
            // the hashes to send are the elements themselves, we are finished
            return state.finishedSynchSkipTransfer();
        } else {
            // the client will now start requesting the element for the given hashes
            return state.finishedSynchMustTransferState();
        }
    }

    private void writeElementsToSend() {
        int elementsSent = 0;
        int elementsToSendCount = elementsToSend.size();
        while (elementsSent < elementsToSendCount) {
            int elementsToSendThisRound;
            if (elementsToSendCount - elementsSent <= MAXIMUM_COUNT_SENT_ELEMENTS) {
                elementsToSendThisRound = elementsToSendCount - elementsSent;
            } else {
                elementsToSendThisRound = MINIMUM_COUNT_SENT_ELEMENTS;
            }
            write(serializeHashesToSend(elementsSent, elementsToSendThisRound), false, false);
            elementsSent += elementsToSendThisRound;
        }
        ccp.write(outgoingChannel, Serializer.serialize(0), true);
    }

    private byte[] serializeHashesToSend(int from, int count) {
        FragmentedByteArray fragmentedByteArray = new FragmentedByteArray();
        fragmentedByteArray.addArrays(Serializer.serialize(count));
        for (int elementPos = from; elementPos < from + count; elementPos++) {
            int hashIndex = elementsToSend.get(elementPos);
            fragmentedByteArray.addArrays(Serializer.serialize(elementList.get(hashIndex)));
        }
        return fragmentedByteArray.generateArray();
    }

    static <T> String calculateListHash(List<T> list, int offset, int length) {
        MD5 md5 = new MD5();
        for (int i = offset; i < offset + length; i++) {
            md5.update(list.get(i));
        }
        return md5.digestAsHex();
    }

    private void write(final byte[] data, boolean useConcurrencyController) {
        write(data, true, useConcurrencyController);
    }

    private void write(final byte[] data, boolean flush, boolean useConcurrencyController) {
        ccp.write(outgoingChannel, data, flush);
        if (useConcurrencyController) {
            concurrencyController.beginActivity(ConcurrencyControllerReadWrite.READ_ACTIVITY);
        }
    }
}
