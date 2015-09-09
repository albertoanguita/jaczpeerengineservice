package jacz.peerengine.util.data_synchronization.old;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.util.concurrency.ConcurrencyUtil;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.io.object_serialization.MutableOffset;
import jacz.util.io.object_serialization.Serializer;
import jacz.util.lists.Lists;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generic methods for acting as a server in an ordered list synch process (answers queries)
 * <p/>
 * This class acts as a mini-FSM for synchronizing the list of hashes. Only the synchProcess method must be invoked, until the finishedSynch state
 * is generated. At that moment, the other end will be sending the list of missing hashes
 */
public class OrderedListSynchServer<S> {

    interface State<S> {

        /**
         * @return State for performing the hash synch. Answering hash queries from the client. Once it receives the finished synch
         *         notification, it exits this state. Nothing is send to the other end
         */
        S inProcessState();

        /**
         * @return State for hash synch process finished. After this, the client will send the list of index and hashes to request
         */
        S finishedSynch();
    }

    /**
     * The result of a hash query, which a client sends back to the server
     */
    static class HashQueryResult implements Serializable {

        enum Type {
            HASH_NOT_FOUND_AND_NO_GREATER,
            HASH_NOT_FOUND_BUT_GREATER,
            HASH_FOUND_LIST_HASH_DIFFERS,
            HASH_FOUND_LIST_HASH_EQUALS
        }

        UniqueIdentifier id;

        Type value;

        HashQueryResult(UniqueIdentifier id, Type value) {
            this.id = id;
            this.value = value;
        }

        HashQueryResult(byte[] data) {
            MutableOffset offset = new MutableOffset();
            id = UniqueIdentifier.deserializeWithOffset(data, offset);
            byte byteValue = Serializer.deserializeByte(data, offset);
            for (Type type : Type.values()) {
                if (type.ordinal() == byteValue) {
                    value = type;
                }
            }
        }

        byte[] serialize() {
            return Serializer.addArrays(id.serialize(), Serializer.serialize((byte) value.ordinal()));
        }
    }


    /**
     * List of hashes that the server has. This list is gradually shrunk with hashes that the client also has
     * <p/>
     * We do nothing with the remaining elements
     */
    private final List<String> elementList;

    private final ChannelConnectionPoint ccp;

    private final byte outgoingChannel;

    private final State<S> state;

    OrderedListSynchServer(List<String> elementList, ChannelConnectionPoint ccp, byte outgoingChannel, State<S> state) {
        this.elementList = elementList;
        Lists.sort(this.elementList, false, ConcurrencyUtil.threadCount(0.75d));
        this.ccp = ccp;
        this.outgoingChannel = outgoingChannel;
        this.state = state;
    }

    public List<String> getRemainingElements() {
        return elementList;
    }

    S initialState() {
        return state.inProcessState();
    }

    S synchProcess(byte[] data) {
        OrderedListSynchClient.HashQuery query = new OrderedListSynchClient.HashQuery(data);
        // check if we are done with this hash synch process
        if (query.querySynchronizationComplete) {
            return state.finishedSynch();
        }
        // result that we will send to the server
        HashQueryResult.Type resultType;
        int indexOfFirstHash = Collections.binarySearch(elementList, query.firstHash);
        if (indexOfFirstHash < 0) {
            // not found
            indexOfFirstHash = -(indexOfFirstHash + 1);
            if (indexOfFirstHash == elementList.size()) {
                // we have no greater hashes than the searched hash
                resultType = HashQueryResult.Type.HASH_NOT_FOUND_AND_NO_GREATER;
            } else {
                // we do have greater hashes than the one searched
                resultType = HashQueryResult.Type.HASH_NOT_FOUND_BUT_GREATER;
            }
        } else {
            // found -> remove the searched element (and the full list if the list hash is also equal)
            if ((elementList.size() - indexOfFirstHash) < query.length) {
                // not enough elements to calculate the list hash --> differs
                resultType = HashQueryResult.Type.HASH_FOUND_LIST_HASH_DIFFERS;
                elementList.remove(indexOfFirstHash);
            } else {
                String listHash = OrderedListSynchClient.calculateListHash(elementList, indexOfFirstHash, query.length);
                if (listHash.equals(query.listHash)) {
                    resultType = HashQueryResult.Type.HASH_FOUND_LIST_HASH_EQUALS;
                    for (int cont = 0; cont < query.length; cont++) {
                        elementList.remove(indexOfFirstHash);
                    }
                } else {
                    resultType = HashQueryResult.Type.HASH_FOUND_LIST_HASH_DIFFERS;
                    elementList.remove(indexOfFirstHash);
                }
            }
        }
        // send the server the result of his query. The id in the result is the same thant the id in the
        // received query so the server can correctly identify this result
        ccp.write(outgoingChannel, new HashQueryResult(query.id, resultType).serialize());
        return state.inProcessState();
    }

    static List<String> deserializeElements(byte[] data) {
        MutableOffset offset = new MutableOffset();
        int hashCount = Serializer.deserializeInt(data, offset);
        List<String> hashes = new ArrayList<>(hashCount);
        for (int i = 0; i < hashCount; i++) {
            hashes.add(Serializer.deserializeString(data, offset));
        }
        return hashes;
    }
}
