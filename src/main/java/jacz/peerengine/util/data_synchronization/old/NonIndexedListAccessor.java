package jacz.peerengine.util.data_synchronization.old;

import jacz.peerengine.PeerID;

import java.io.Serializable;
import java.util.Collection;

/**
 * An accessor for lists of elements without indexes. There is only one level, since there are no indexes to join several levels together.
 * There are no inner lists, since it does not make sense that the elements are inner lists (it is simpler that we have a normal list accessor)
 * <p/>
 * Since there are no indexes, we cannot synchronize single elements, only full lists
 */
public interface NonIndexedListAccessor {

    /**
     * Different possible transmission modes. These modes are divided in two: direct and indirect. In direct modes the
     * elements will be sent directly through the available ChannelConnectionPoints to the other end. In indirect mode,
     * elements are downloaded from one point to the other as resources. The direct mode should be used for small
     * sized elements, such as small objects or small data of no more than around 2KB. The indirect mode should be
     * used for larger elements, since its direct transmission could saturate communications.
     */
    public enum TransmissionType {
        /**
         * Elements are sent as objects, directly
         */
        OBJECT,

        /**
         * Elements are sent as byte arrays, indirectly
         */
        BYTE_ARRAY,
    }

    public void beginSynchProcess(ListAccessor.Mode mode);

    /**
     * Retrieves the list of hashes of the list. Comparisons of client and server hashes will be performed in order to check which elements
     * must be transferred. NOTE: repeated hashes are not allowed
     * <p/>
     * This method is invoked in both client and server mode.
     *
     * @return the collection of hashes at the given level. Each hash is formed by the index (level 0) and the hash of the required level
     */
    public Collection<String> getHashList() throws DataAccessException;

    /**
     * This method says whether the hashes of the elements are equal to the elements themselves. This
     * allows avoiding unnecessary traffic during the synchronization process. In this case, the method
     * addElementAsObject will be invoked with each of the hashes as argument.
     * <p/>
     * This is useful when the elements themselves are smaller than their hash (e.g. a boolean, an integer) so it makes no sense to
     * first transmit their hash and then the element. In this case, the element itself acts like a hash during the comparison process
     * <p/>
     * This method is only invoked in server mode.
     *
     * @return true if for the given level, the hashes provided with getHashList(level) are equal to the elements
     *         themselves at that level, and thus we would obtain the same when invoking getElement. False
     *         otherwise
     */
    public boolean hashEqualsElement();

    /**
     * This method is invoked when writing on a list and a specific element (identified by its hash) was not found
     * in the list. For each element not found in the list, the synchronizer code will invoke this method, asking
     * "again" the list if it needs to request this element and add it. At this moment the list can check if it
     * already contains the element or if it can gather it from somewhere else (e.g. a FileDatabase). If false is
     * returned, a subsequent invocation to addElement* will happen. If true is returned, it is assumed that the
     * list already contains the element and that it is not necessary to add it.
     * <p/>
     * This method is only invoked in client mode.
     *
     * @param index index of this element
     * @param level level value being checked
     * @param hash  hash of the checked element
     * @return true if the list has a way of obtaining the specified element, and therefore the synchronizer does not need to request it to the server
     *         false otherwise (in this case the element will be added subsequently)
     */
    public boolean mustRequestElement(String index, int level, String hash) throws DataAccessException;


    /**
     * Retrieves the transmission mode for the elements of a specific level.
     * <p/>
     * This method is invoked both in client and server mode.
     *
     * @return the transmission mode for the given level (a TransmissionType value). Values available are direct
     *         objects, direct byte arrays, direct names of inner lists (such names will be transmitted as direct byte
     *         arrays) or indirect
     */
    public TransmissionType getTransmissionType();

    /**
     * This method retrieves an element in object mode. It is only invoked if for the given level, the OBJECT
     * transmission mode was previously given. The given hash is one of the previously retrieved with the method
     * getHashList(level). It is responsibility of the implementation to be able to efficiently access elements
     * by their hash, for any possible level.
     * <p/>
     * This method is only invoked in server mode.
     *
     * @param hash hash of the element to retrieve
     * @return an object representing the required element. This element must implement the Serializable interface
     * @throws ElementNotFoundException the given hash does not correspond to an existing element in this list and
     *                                  at the given level
     */
    public Serializable getElementObject(String hash) throws ElementNotFoundException, DataAccessException;

    /**
     * This method retrieves an element in byte array mode. It is only invoked if for the given level, the
     * BYTE_ARRAY transmission mode was previously given. The given hash is one of the previously retrieved
     * with the method getHashList(level). It is responsibility of the implementation to be able to efficiently
     * access elements by their hash, for any possible level.
     * <p/>
     * This method is only invoked in server mode.
     *
     * @param hash hash of the element to retrieve
     * @return an array of bytes representing the required element
     * @throws ElementNotFoundException the given hash does not correspond to an existing element in this list and
     *                                  at the given level
     */
    public byte[] getElementByteArray(String hash) throws ElementNotFoundException, DataAccessException;

    /**
     * This method retrieves the length of a byte array element. It is preferable to be able to provide this length without putting the full
     * element in memory, since the synchronizer needs to calculate the total length of a set of elements before reading each of them for sending.
     * Nevertheless, if this is not possible, this method can simply invoke getElementByteArray(index, level).length;
     * <p/>
     * This method is only invoked in server mode.
     *
     * @param hash hash value for the requested element
     * @return the length in bytes of the requested element
     * @throws ElementNotFoundException
     */
    public int getElementByteArrayLength(String hash) throws ElementNotFoundException, DataAccessException;

    /**
     * This method adds an element in object mode to this list. It is responsibility of the list implementation to
     * include in the object representation of the element any indexation information that may help to add it in an
     * efficient manner. This method is only invoked if for the given level, the OBJECT transmission mode
     * was previously given.
     * <p/>
     * This method is only invoked in client mode.
     *
     * @param element object representation of the element being added to the list
     */
    public void addElementAsObject(Object element) throws DataAccessException;

    /**
     * This method adds an element in object mode to this list. It is responsibility of the list implementation to
     * include in the object representation of the element any indexation information that may help to add it in an
     * efficient manner. This method is only invoked if for the given level, the BYTE_ARRAY transmission mode
     * was previously given.
     * <p/>
     * This method is only invoked in client mode.
     *
     * @param data byte array representation of the element being added to the list
     */
    public void addElementAsByteArray(byte[] data) throws DataAccessException;

    /**
     * Indicates if this list uses indexes, and therefore old indexes must be removed during synchronizations
     *
     * @return true if indexes are used
     */
    public boolean mustEraseOldIndexes();

    /**
     * This method erases a set of element from the list. It is responsibility of the implementation
     * to be able to efficiently access elements by their hash, for any possible level.
     * <p/>
     * This method is only invoked in client mode.
     *
     * @param hashes hashes of the elements to erase
     */
    public void eraseElements(Collection<String> hashes) throws DataAccessException;

    public void endSynchProcess(ListAccessor.Mode mode, boolean success);

    /**
     * This method returns an object for monitoring progress in server mode. It is invoked right at the beginning of
     * the process, and allows notifying that this list is going to be synched in server mode (for progress monitoring). It is not
     * mandatory to include an implementation on it, as its goal is simply to inform the user about this synchronization, in case he is interested.
     * Returning a null object makes the synchronization software ignore the progress of this process
     * <p/>
     * This method is only invoked in server mode.
     *
     * @param clientPeerID  identifier of the peer requesting synchronization of this list
     * @param singleElement the synch request is for a single element (true) or for the full list (false)
     * @return a ServerSynchRequestAnswer object containing the answer (OK or SERVER_BUSY), plus
     *         an implementation of the ProgressNotificationWithError<Integer, String> interface if it is desired that
     *         the progress of such synchronization is monitored. addMessage(Integer) invocations will be performed
     *         to the given implementation, with values from 0 to 10000, and a completeTask() invocation will be
     *         done after the process is complete. Returning a null value means that no progress monitoring is required.
     */
    public ServerSynchRequestAnswer initiateListSynchronizationAsServer(PeerID clientPeerID, boolean singleElement);
}
