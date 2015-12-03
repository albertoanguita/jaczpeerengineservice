package jacz.peerengineservice.util.data_synchronization;

import jacz.peerengineservice.PeerID;
import jacz.util.lists.Duple;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Alberto on 17/09/2015.
 */
public interface DataAccessor {

    enum Mode {
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
     * This method simply notifies the accessor that the synch process begins. It indicates the list accessor if the process is in client or
     * server mode. In case of client, this is invoked when we have received the OK from the server. In case of the server, this is invoked when
     * we have accepted the request from the client
     *
     * @param mode client or server mode
     */
    void beginSynchProcess(Mode mode);

    /**
     * The database id (for both client and server)
     *
     * @return a String with the database ID. If null, no ID is considered for synching
     */
    String getDatabaseID();

    /**
     * Sets a new database ID for the client
     *
     * @param databaseID the new databaseID
     */
    void setDatabaseID(String databaseID);

    /**
     * The last timestamp that the client has, for sending it to the server
     *
     * @return the last value of the timestamp of the requested data, or NULL/negative if the whole list is requested
     * @throws DataAccessException error accessing the data
     */
    Integer getLastTimestamp() throws DataAccessException;

    /**
     * This method retrieves an element in object mode. It is only invoked if for the given level, the OBJECT
     * transmission mode was previously given. The given hash is one of the previously retrieved with the method
     * getHashList(level). It is responsibility of the implementation to be able to efficiently access elements
     * by their hash, for any possible level.
     * <p/>
     * This method is only invoked in server mode.
     *
     * @param fromTimestamp the minimum timestamp required. Elements must be provided
     *                      with equal or newer timestamps, order by timestamp
     *                      If 0, all elements are requested
     * @return an object representing the required element. This element must implement the Serializable interface
     */
    List<? extends Serializable> getElementsFrom(int fromTimestamp) throws DataAccessException;

    int elementsPerMessage();

    int CRCBytes();

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
    void setElement(Object element) throws DataAccessException;

    /**
     * This method is invoked when the synch process is complete
     *
     * @param mode    client or server mode
     * @param success whether the process ended successfully
     */
    void endSynchProcess(Mode mode, boolean success);

    /**
     * This method returns an object for monitoring progress in server mode. It is invoked right at the beginning of
     * the process, and allows notifying that this list is going to be synched in server mode (for progress monitoring). It is not
     * mandatory to include an implementation on it, as its goal is simply to inform the user about this synchronization, in case he is interested.
     * Returning a null object makes the synchronization software ignore the progress of this process
     * <p/>
     * This method is only invoked in server mode.
     *
     * @param clientPeerID identifier of the peer requesting synchronization of this list
     * @return a ServerSynchRequestAnswer object containing the answer (OK or SERVER_BUSY), plus
     * an implementation of the ProgressNotificationWithError<Integer, String> interface if it is desired that
     * the progress of such synchronization is monitored. addMessage(Integer) invocations will be performed
     * to the given implementation, with values from 0 to 10000, and a completeTask() invocation will be
     * done after the process is complete. Returning a null value means that no progress monitoring is required.
     */
    ServerSynchRequestAnswer initiateListSynchronizationAsServer(PeerID clientPeerID);
}
