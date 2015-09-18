package jacz.peerengineservice.util.data_synchronization.old;

import jacz.peerengineservice.PeerID;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * This interface contains the methods for accessing a single list. These include retrieving, updating or adding elements
 * <p/>
 * Elements are referred to by a hash code, so the implementation will have to take care of optimally finding elements given their hash.
 * <p/>
 * A list is structured in different levels. Each level translates to a certain amount of information of each
 * element of the list. For example, in a list of films, level 0 might be title, level 1 small picture, level 2
 * all the detailed information (directors, actors...) and level 3 large picture.
 * <p/>
 * Elements from all levels will be eventually referred by their hash, so the list implementation must be able to find the requested
 * element with the hash
 * <p/>
 * By providing access to the elements of a list, the list synchronizer is able to synch lists between peers. The basic process is:
 * - Hashes of the two lists are compared with an optimal algorithm that minimizes the steps
 * - Missing hashes in the client are transmitted from the server to the client (extra hashes are deleted)
 * - Transmission of each element can be performed in three different ways:
 * -- Object: the element is transmitted via object serialization (use sparely as this is the least optimal way, only with small size objects)
 * -- Byte arrays: the element is translated into an array of bytes. Similar to the object, but we save some size
 * -- Indirect: we employ the resource transmission utility of the peer engine. Use this with big elements, such as pictures, so they do not
 * interfere with other transfers
 * <p/>
 * Lists can contain other lists themselves. For that, the transmission mode of a level is marked as INNER_LIST, meaning that the elements
 * of that level are actual lists. In that case, the method getInnerListLevel allows to decide which levels of the inner list are to be transmitted.
 * <p/>
 * Implementing classes should always care about synchronizing the getter and setter methods properly
 * <p/>
 * The character '@' is forbidden in the indexes and the hashes, as it is a control character
 * Level 0 is always the index of elements, of String type
 */
public interface ListAccessor {

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

        /**
         * Elements are inner lists
         */
        INNER_LISTS,
    }

    public enum Mode {
        CLIENT,
        SERVER;

        public boolean isClient() {
            return this == CLIENT;
        }

        public boolean isServer() {
            return !isClient();
        }
    }

    /**
     * Retrieves the level count of this list, not including level indexes. This value must be a positive integer.
     * <p/>
     * This method is invoked both in client and server mode.
     *
     * @return the level count (how many levels are available) in this list
     */
    public int getLevelCount();

    /**
     * This method simply notifies the accessor that the synch process begins. It indicates the list accessor if the process is in client or
     * server mode. In case of client, this is invoked when we have received the OK from the server. In case of the server, this is invoked when
     * we have accepted the request from the client
     *
     * @param mode client or server mode
     */
    public void beginSynchProcess(Mode mode);

    /**
     * Retrieves the list of hashes of a list at a given level, and their level 0 indexes. Comparisons of client and server hashes will be
     * performed in order to check which elements must be transferred.
     * <p/>
     * If the given level corresponds to a level with inner lists, each returned hash should represent the complete state of its corresponding
     * inner list, so that if two hashes are the same, it means that the list contain exactly the same elements
     * <p/>
     * If the required level is level 0, the hash will be ignored and only the index will be used (so the hash can be set to null to save bandwidth)
     * <p/>
     * This method is invoked in both client and server mode.
     *
     * @param level level for hashes
     * @return the collection of hashes at the given level. Each hash is formed by the index (level 0) and the hash of the required level
     */
    public Collection<IndexAndHash> getHashList(int level) throws DataAccessException;

    /**
     * This method says whether the hashes of the elements in a level are equal to the elements themselves. This
     * allows avoiding unnecessary traffic during the synchronization process. In this case, the method
     * addElementAsObject will be invoked with each of the hashes as argument.
     * <p/>
     * This is useful when the elements themselves are smaller than their hash (e.g. a boolean, an integer) so it makes no sense to
     * first transmit their hash and then the element. In this case, the element itself acts like a hash during the comparison process
     * <p/>
     * This method is invoked in both client and server mode.
     *
     * @param level the level of elements being queried (only invoked for levels from 1 upwards).
     * @return true if for the given level, the hashes provided with getHashList(level) are equal to the elements
     *         themselves at that level, and thus we would obtain the same when invoking getElement. False
     *         otherwise
     */
    public boolean hashEqualsElement(int level);

    /**
     * Retrieves the transmission mode for the elements of a specific level.
     * <p/>
     * This method is invoked both in client and server mode.
     *
     * @param level the level of elements being queried (from 1 upwards)
     * @return the transmission mode for the given level (a TransmissionType value). Values available are direct
     *         objects, direct byte arrays, direct names of inner lists (such names will be transmitted as direct byte
     *         arrays) or indirect
     */
    public TransmissionType getTransmissionType(int level);

    /**
     * This method retrieves for a given level, in case such level elements are inner lists, the level of the
     * elements to retrieve from such inner lists. For example, if level 4 or the current list is an inner list,
     * we must know which levels of such list we must retrieve (it can be several through the level explosion feature)
     * <p/>
     * This method is only invoked in client mode.
     *
     * @param level the level of elements being queried
     * @return the level that should be used for the inner lists
     */
    public List<Integer> getInnerListLevels(int level);

    /**
     * This method retrieves for a given level and hash, the actual ListAccessor implementation of the inner list
     * of the given element. The given hash is one of the previously retrieved with the method
     * getHashList(level). It is responsibility of the implementation to be able to efficiently access elements
     * by their hash, for any possible level (for example, including some index in the returned hash).
     * <p/>
     * It is not needed that the actual list found in the position
     * given by the hash location part has the same hash as the given one. In other words, we want the inner list
     * at the position given by the hash, whatever its actual hash is.
     * <p/>
     * This method is invoked in both client and server mode.
     *
     * @param index                the index of the element whose list we require
     * @param level                the level of elements being queried
     * @param buildElementIfNeeded indicates that, in case that the requested list does not exist, it should be built
     *                             this value is true only if the accessor is acting as client
     * @return the ListAccessor for the inner list of the given element
     * @throws ElementNotFoundException the given hash does not correspond to an existing element, so the inner list
     *                                  could not be found
     */
    public ListAccessor getInnerList(String index, int level, boolean buildElementIfNeeded) throws ElementNotFoundException, DataAccessException;

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
     * This method allows retrieving the hash of a specific element at a given level. To specify which element we
     * want to retrieve, we provide another hash and the level for such hash
     * <p/>
     * This method is used for synchronization of single elements (because it is expected only level 0 acts as index, in order to
     * request the level 4 data of a specific element, we need to transmit the level 0 hash of that element). This is not done when
     * complete lists are synched because in that case we do not have to find a specific element, we need all.
     * <p/>
     * This method is invoked in server mode.
     *
     * @param index        index of the element whose hash is to retrieve
     * @param requestLevel the level of the hash that we want to retrieve
     * @return the hash of the requested element, at the level of requestLevel
     * @throws ElementNotFoundException the provided hash does not fit in any existing element
     */
    public String getElementHash(String index, int requestLevel) throws ElementNotFoundException, DataAccessException;

    /**
     * This method retrieves an element in object mode. It is only invoked if for the given level, the OBJECT
     * transmission mode was previously given. The given hash is one of the previously retrieved with the method
     * getHashList(level). It is responsibility of the implementation to be able to efficiently access elements
     * by their hash, for any possible level.
     * <p/>
     * This method is only invoked in server mode.
     *
     * @param index index of the element to retrieve
     * @param level level value being checked
     * @return an object representing the required element. This element must implement the Serializable interface
     * @throws ElementNotFoundException the given hash does not correspond to an existing element in this list and
     *                                  at the given level
     */
    public Serializable getElementObject(String index, int level) throws ElementNotFoundException, DataAccessException;

    /**
     * This method retrieves an element in byte array mode. It is only invoked if for the given level, the
     * BYTE_ARRAY transmission mode was previously given. The given hash is one of the previously retrieved
     * with the method getHashList(level). It is responsibility of the implementation to be able to efficiently
     * access elements by their hash, for any possible level.
     * <p/>
     * This method is only invoked in server mode.
     *
     * @param index index of the element to retrieve
     * @param level level value being checked
     * @return an array of bytes representing the required element
     * @throws ElementNotFoundException the given hash does not correspond to an existing element in this list and
     *                                  at the given level
     */
    public byte[] getElementByteArray(String index, int level) throws ElementNotFoundException, DataAccessException;

    /**
     * This method retrieves the length of a byte array element. It is preferable to be able to provide this length without putting the full
     * element in memory, since the synchronizer needs to calculate the total length of a set of elements before reading each of them for sending.
     * Nevertheless, if this is not possible, this method can simply invoke getElementByteArray(index, level).length;
     * <p/>
     * This method is only invoked in server mode.
     *
     * @param index index value for the requested element
     * @param level level value being checked
     * @return the length in bytes of the requested element
     * @throws ElementNotFoundException
     */
    public int getElementByteArrayLength(String index, int level) throws ElementNotFoundException, DataAccessException;

    /**
     * This method adds an element in object mode to this list. It is responsibility of the list implementation to
     * include in the object representation of the element any indexation information that may help to add it in an
     * efficient manner. This method is only invoked if for the given level, the OBJECT transmission mode
     * was previously given.
     * <p/>
     * This method is only invoked in client mode.
     *
     * @param level   level value being checked
     * @param element object representation of the element being added to the list
     */
    public void addElementAsObject(String index, int level, Object element) throws DataAccessException;

    /**
     * This method adds an element in object mode to this list. It is responsibility of the list implementation to
     * include in the object representation of the element any indexation information that may help to add it in an
     * efficient manner. This method is only invoked if for the given level, the BYTE_ARRAY transmission mode
     * was previously given.
     * <p/>
     * This method is only invoked in client mode.
     *
     * @param level level value being checked
     * @param data  byte array representation of the element being added to the list
     */
    public void addElementAsByteArray(String index, int level, byte[] data) throws DataAccessException;

    /**
     * Indicates if this list uses an arbitrary number of element positions, and therefore old positions must be erased
     * Lists like the personal data list return false, because it only has one position and copying a new element replaces the old one, so
     * erasing is not needed
     *
     * @return true if old elements must be erased, in addition of copying new ones
     */
    public boolean mustEraseOldIndexes();

    /**
     * This method erases a set of element from the list. It is responsibility of the implementation
     * to be able to efficiently access elements by their hash, for any possible level.
     * <p/>
     * This method is only invoked in client mode.
     *
     * @param indexes indexes of the elements to erase
     */
    public void eraseElements(Collection<String> indexes) throws DataAccessException;

    /**
     * This method is invoked when the synch process is complete
     *
     * @param mode    client or server mode
     * @param success whether the process ended successfully
     */
    public void endSynchProcess(Mode mode, boolean success);

    /**
     * This method returns an object for monitoring progress in server mode. It is invoked right at the beginning of
     * the process, and allows notifying that this list is going to be synched in server mode (for progress monitoring). It is not
     * mandatory to include an implementation on it, as its goal is simply to inform the user about this synchronization, in case he is interested.
     * Returning a null object makes the synchronization software ignore the progress of this process
     * <p/>
     * This method is only invoked in server mode.
     *
     * @param clientPeerID  identifier of the peer requesting synchronization of this list
     * @param level         the level requested
     * @param singleElement the synch request is for a single element (true) or for the full list (false)
     * @return a ServerSynchRequestAnswer object containing the answer (OK or SERVER_BUSY), plus
     *         an implementation of the ProgressNotificationWithError<Integer, String> interface if it is desired that
     *         the progress of such synchronization is monitored. addMessage(Integer) invocations will be performed
     *         to the given implementation, with values from 0 to 10000, and a completeTask() invocation will be
     *         done after the process is complete. Returning a null value means that no progress monitoring is required.
     */
    public ServerSynchRequestAnswer initiateListSynchronizationAsServer(PeerID clientPeerID, int level, boolean singleElement);
}
