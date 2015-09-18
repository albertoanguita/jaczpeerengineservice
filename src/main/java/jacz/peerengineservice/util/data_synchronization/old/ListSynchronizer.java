package jacz.peerengineservice.util.data_synchronization.old;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClient;
import jacz.util.notification.ProgressNotificationWithError;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains the utilities for synchronizing a list of elements between peers. This list must be ordered,
 * and the elements of the list must implement a hash method that allows checking the equality of them.
 * <p/>
 * Algorithm: the algorithm for synchronizing lists is implemented by two FSMs, the ListSynchronizerClientFSM and the
 * ListSynchronizerServerFSM.
 * <p/>
 * Similar calls can be made concurrently, it is responsibility of the client not to invoke two times the same thing. In any case,
 * concurrent similar calls should mostly help each other by transferring different data each time (the actual datum
 * transferred at each step is selected randomly, so the chances of transferring two times the same datum is rather low)
 * <p/>
 * issues left:
 * - regarding the indexation of lists, it is problem of the client to include the indexes in the transferred data.
 * i.e. the client must include himself the index of the element being transferred. It makes no sense that we do it,
 * because we would not know when it is included or not. This way, when we add an element to a list, the element
 * should contain the needed index information to do it efficiently, for any possible level
 * <p/>
 * - regarding retrieval of elements with a given hash, the problem is that the client would need to have indexes
 * for the hashes of every possible level. That can be inefficient in some cases. But that is also a problem of the
 * client. He can have all indexes, or include the first level hash (the one that acts as index) together with other
 * level hashes. For example he can concatenate the hashes so it is easy to insert new elements.
 * <p/>
 * - regarding inner lists, when we must transfer the content of an inner list, the server will generate a name for
 * accessing that list and transfer it to the client. The client will give that name to the synch manager so he
 * queues that name. The construction of the name is a problem of the client. It should typically include the hashes
 * involved.
 *
 * todo remove synch of single elements. It will never be used. We want to synch the full lists from a beginning, because the peer might not be
 * available later. So we don't need to synch specific elements, only full lists
 */
public class ListSynchronizer {

    static final long SERVER_FSM_TIMEOUT = 15000;

    /**
     * The PeerClient for which this ListSynchronizer works
     */
    private final PeerClient peerClient;

    /**
     * List container provided by the client, with the lists that can be synched
     */
    private ListContainer listContainer;

    /**
     * Owr own peer id
     */
    private PeerID ownPeerID;

    /**
     * Whether it is permitted to perform list synchronization with peers that are not yet confirmed as friends
     */
    private final boolean allowSynchronizingBetweenNonFriendPeers;


    public ListSynchronizer(PeerClient peerClient, ListContainer listContainer, PeerID ownPeerID, boolean allowSynchronizingBetweenNonFriendPeers) {
        this.peerClient = peerClient;
        this.listContainer = listContainer;
        this.ownPeerID = ownPeerID;
        this.allowSynchronizingBetweenNonFriendPeers = allowSynchronizingBetweenNonFriendPeers;
    }

    public void synchronizeList(PeerID peerID, String list, int level, long timeout) {
        synchronizeList(peerID, list, level, timeout, null);
    }

    public void synchronizeList(PeerID peerID, String list, int level, long timeout, ProgressNotificationWithError<Integer, SynchronizeError> progress) {
        ArrayList<Integer> levelList = new ArrayList<>(1);
        levelList.add(level);
        synchronizeList(peerID, list, levelList, timeout, progress);
    }

    public void synchronizeList(PeerID peerID, String list, int fromLevel, int toLevel, long timeout) {
        synchronizeList(peerID, list, fromLevel, toLevel, timeout, null);
    }

    public void synchronizeList(PeerID peerID, String list, int fromLevel, int toLevel, long timeout, ProgressNotificationWithError<Integer, SynchronizeError> progress) {
        ArrayList<Integer> levelList = new ArrayList<>();
        for (int i = fromLevel; i <= toLevel; i++) {
            levelList.add(i);
        }
        synchronizeList(peerID, list, levelList, timeout, progress);
    }

    public void synchronizeList(PeerID peerID, String list, List<Integer> levelList, long timeout) {
        synchronizeList(peerID, list, levelList, timeout, null);
    }

    public void synchronizeList(PeerID peerID, String list, List<Integer> levelList, long timeout, ProgressNotificationWithError<Integer, SynchronizeError> progress) {
        new ListSynchronizerManager(peerClient, peerID, list, levelList, timeout, progress);
    }

    public void synchronizeElement(PeerID peerID, String list, String elementIndex, int level, long timeout) {
        synchronizeElement(peerID, list, elementIndex, level, timeout, null);
    }

    public void synchronizeElement(PeerID peerID, String list, String elementIndex, int level, long timeout, ProgressNotificationWithError<Integer, SynchronizeError> progress) {
        ArrayList<Integer> levelList = new ArrayList<>(1);
        levelList.add(level);
        synchronizeElement(peerID, list, elementIndex, levelList, timeout, progress);
    }

    public void synchronizeElement(PeerID peerID, String list, String elementIndex, int fromLevel, int toLevel, long timeout) {
        synchronizeElement(peerID, list, elementIndex, fromLevel, toLevel, timeout, null);
    }

    public void synchronizeElement(PeerID peerID, String list, String elementIndex, int fromLevel, int toLevel, long timeout, ProgressNotificationWithError<Integer, SynchronizeError> progress) {
        ArrayList<Integer> levelList = new ArrayList<>();
        for (int i = fromLevel; i <= toLevel; i++) {
            levelList.add(i);
        }
        synchronizeElement(peerID, list, elementIndex, levelList, timeout, progress);
    }

    public void synchronizeElement(PeerID peerID, String list, String elementIndex, List<Integer> levelList, long timeout) {
        synchronizeElement(peerID, list, elementIndex, levelList, timeout, null);
    }

    public void synchronizeElement(PeerID peerID, String list, String elementIndex, List<Integer> levelList, long timeout, ProgressNotificationWithError<Integer, SynchronizeError> progress) {
        new ListSynchronizerManager(peerClient, peerID, list, elementIndex, levelList, timeout, progress);
    }

    PeerClient getPeerClient() {
        return peerClient;
    }

    PeerID getOwnPeerID() {
        return ownPeerID;
    }

    boolean isAllowSynchronizingBetweenNonFriendPeers() {
        return allowSynchronizingBetweenNonFriendPeers;
    }

    ListContainer getListContainer() {
        return listContainer;
    }

    static String generateErrorMessageForListNotFound(String list) {
        return "Error: list not found in ListAccessor implementation: " + list;
    }

    static String generateErrorMessageForIncorrectLevel(String list, int level) {
        return "Error: incorrect level required for list: " + list + "/" + level;
    }
}
