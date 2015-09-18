package jacz.peerengineservice.util.data_synchronization.old;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.client.PeerClient;
import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.concurrency.task_executor.ParallelTaskExecutor;
import jacz.util.lists.Duple;
import jacz.util.notification.ProgressNotificationWithError;
import jacz.util.numeric.NumericUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * This class allows organizing and handling the synchronization of one single list. This manager creates the client
 * FSMs needed to synchronize lists. If the given level divides itself into more levels, it will rather recursively
 * create an array of other managers to handle the petitions.
 * <p/>
 * The manager can accept a progress handler so the invoker of this function can have periodic reports about the
 * progress.
 * <p/>
 * The synch task is specified through a ListPath object
 * <p/>
 * The manager accepts tasks which involve synchronizing only one element of a main list. A specific constructor is
 * used in that case.
 * <p/>
 * In this new version, every sub-task that is generated goes to a new ListSynchronizerManager. There is therefore no need for an inner list.
 * Inner lists are exploded in a new ListSynchronizerManager
 */
public class ListSynchronizerManager implements ProgressNotificationWithError<Integer, SynchronizeError> {

    public static final int PROGRESS_MAX = 10000;

    /**
     * PeerClient for registering the FSMs and asking about the connection status of the peers
     */
    private final PeerClient peerClient;

    /**
     * Peer to which we request the synchronization of lists
     */
    private final PeerID serverPeerID;

    /**
     * Timeout for created tasks
     */
    private final long timeout;

    /**
     * If the client requested the synch of a single element, here we store the hash (null if not used)
     */
    private final String elementIndex;

    /**
     * Lists of main tasks. We will be clearing this list during the process until it is empty. Each task is represented by a ListPath object,
     * which indicates a list level that must be synchronized
     * <p/>
     * It does not include inner lists, only main list levels. It is fed at construction time, and no more elements are inserted into it
     * afterwards
     */
    private final List<ListPath> taskQueue;

    /**
     * The list path for the list currently being synchronized
     */
    private ListPath currentListPath;

    /**
     * The progress notifier to which we give reports
     */
    private final ProgressNotificationWithError<Integer, SynchronizeError> progress;

    /**
     * Total number of main tasks (the actual tasks this manager received)
     */
    private int taskCount;

    /**
     * Number of completed main tasks. This only includes levels of the main list
     */
    private int completedTaskCount;

    /**
     * The current min progress to report
     */
    private int currentProgressMin;

    /**
     * The current max progress to report
     */
    private int currentProgressMax;

    /**
     * Constructor for synchronizing a set of levels of a main list
     *
     * @param peerClient   PeerClient that issues the request
     * @param serverPeerID peer to which we will request the synch
     * @param list         list to synch
     * @param levelList    levels to synch
     * @param timeout      process timeout
     * @param progress     progress to report to
     */
    public ListSynchronizerManager(PeerClient peerClient, PeerID serverPeerID, String list, List<Integer> levelList, long timeout, ProgressNotificationWithError<Integer, SynchronizeError> progress) {
        // todo remove public, just to make it compile (9/9/2015)
        this.peerClient = peerClient;
        this.serverPeerID = serverPeerID;
        this.timeout = timeout;
        this.progress = progress;
        elementIndex = null;
        taskQueue = new ArrayList<>();
        initializeTaskQueue(list, levelList);
        ParallelTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                processTasks();
            }
        });
    }

    /**
     * Constructor for synchronizing a specific element
     *
     * @param peerClient   PeerClient that issues the request
     * @param serverPeerID peer to which we will request the synch
     * @param list         list to synch
     * @param elementIndex element to synch (single element synchronization)
     * @param levelList    levels to synch
     * @param timeout      process timeout
     * @param progress     progress to report to
     */
    public ListSynchronizerManager(PeerClient peerClient, PeerID serverPeerID, String list, String elementIndex, List<Integer> levelList, long timeout, ProgressNotificationWithError<Integer, SynchronizeError> progress) {
        // todo remove public, just to make it compile (9/9/2015)
        this.peerClient = peerClient;
        this.serverPeerID = serverPeerID;
        this.timeout = timeout;
        this.progress = progress;
        this.elementIndex = elementIndex;
        taskQueue = new ArrayList<>();
        initializeTaskQueue(list, levelList);
        ParallelTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                processTasks();
            }
        });
    }

    /**
     * Constructor for synchronizing inner lists
     *
     * @param peerClient       PeerClient that issues the request
     * @param serverPeerID     peer to which we will request the synch
     * @param baseListPath     base list path for constructing the list paths
     * @param indexOfInnerList index of the inner list to synch in the base list path
     * @param levelList        levels to synch
     * @param timeout          process timeout
     * @param progress         progress to report to
     */
    ListSynchronizerManager(PeerClient peerClient, PeerID serverPeerID, ListPath baseListPath, String indexOfInnerList, List<Integer> levelList, long timeout, ProgressNotificationWithError<Integer, SynchronizeError> progress) {
        this.peerClient = peerClient;
        this.serverPeerID = serverPeerID;
        this.timeout = timeout;
        this.progress = progress;
        elementIndex = null;
        taskQueue = new ArrayList<>();
        initializeTaskQueue(baseListPath, indexOfInnerList, levelList);
        ParallelTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                processTasks();
            }
        });
    }

    /**
     * Initializes the task queue with a main list and a set of main list levels
     *
     * @param list      name of the main list to synch
     * @param levelList levels of the main list to synch
     */
    private void initializeTaskQueue(String list, List<Integer> levelList) {
        for (Integer level : levelList) {
            if (level == null) {
                continue;
            }
            taskQueue.add(new ListPath(list, level));
        }
    }

    /**
     * Initializes the task queue with a list path and a set of levels for the last inner list in that list path
     *
     * @param listPath         a base list path
     * @param indexOfInnerList index for the inner list to add to the list path
     * @param levelList        levels of the inner list
     */
    private void initializeTaskQueue(ListPath listPath, String indexOfInnerList, List<Integer> levelList) {
        // substitute the last level in listPath for each level given, so we get a new set of list paths
        for (Integer level : levelList) {
            if (level == null) {
                continue;
            }
            taskQueue.add(new ListPath(listPath, new ListPath.IndexAndLevel(indexOfInnerList, level)));
        }
    }

    /**
     * Processes all pending tasks. Tasks are launched sequentially (an FSM is created for each of them)
     */
    private void processTasks() {
        taskCount = taskQueue.size();
        completedTaskCount = 0;
        processOneTask();
    }

    private void processOneTask() {
        if (!taskQueue.isEmpty()) {
            // main lists
            Duple<Integer, Integer> progressRange = NumericUtil.divideRange(0, PROGRESS_MAX, taskCount, completedTaskCount);
            currentProgressMin = progressRange.element1;
            currentProgressMax = progressRange.element2;
            ListPath listPath = taskQueue.remove(0);
            executeListSynchronizer(serverPeerID, listPath, timeout);
        } else {
            if (progress != null) {
                progress.completeTask();
            }
        }
    }

    /**
     * Performs the synchronization of one specific level
     *
     * @param peerID   peer to synch with
     * @param listPath list and level to synch
     * @param timeout  provided timeout
     */
    private void executeListSynchronizer(PeerID peerID, ListPath listPath, long timeout) {
        currentListPath = listPath;
        try {
            boolean result;
            if (synchronizationIsForSingleElement()) {
                result = peerClient.registerTimedCustomFSM(peerID, new ElementSynchronizerClientFSM(peerClient, this, serverPeerID, listPath, elementIndex, this), ElementSynchronizerServerFSM.CUSTOM_FSM_NAME, timeout);
            } else {
                result = peerClient.registerTimedCustomFSM(peerID, new ListSynchronizerClientFSM(peerClient, this, serverPeerID, listPath, this), ListSynchronizerServerFSM.CUSTOM_FSM_NAME, timeout);
            }
            if (!result) {
                ParallelTaskExecutor.executeTask(new ParallelTask() {
                    @Override
                    public void performTask() {
                        ListSynchronizerManager.this.error(new SynchronizeError(SynchronizeError.Type.PEER_CLIENT_BUSY, null));
                    }
                });
            }
        } catch (UnavailablePeerException e) {
            ParallelTaskExecutor.executeTask(new ParallelTask() {
                @Override
                public void performTask() {
                    ListSynchronizerManager.this.error(new SynchronizeError(SynchronizeError.Type.DISCONNECTED, null));
                }
            });
        }
    }

    boolean synchronizationIsForSingleElement() {
        return elementIndex != null;
    }

    @Override
    public void addNotification(Integer message) {
        // value from 0 to PROGRESS_MAX of the current task
        if (progress != null) {
            progress.addNotification(NumericUtil.displaceInRange(message, 0, PROGRESS_MAX, currentProgressMin, currentProgressMax));
        }
    }

    @Override
    public void completeTask() {
        // current task complete, execute next one
        completedTaskCount++;
        processOneTask();
    }

    @Override
    public void error(SynchronizeError error) {
        if (progress != null) {
            progress.error(error);
        }
    }

    @Override
    public void timeout() {
        if (progress != null) {
            progress.timeout();
        }
    }

    void currentTaskIsInnerList(List<String> indexOfInnerLists, List<Integer> innerListLevels) {
        new InnerListsSynchronizerManager(peerClient, serverPeerID, timeout, currentListPath, indexOfInnerLists, innerListLevels, progress);
    }

    static Duple<ListAccessor, Integer> getListForReading(ListContainer listContainer, PeerID clientPeerID, ListPath listPath) throws ListNotFoundException {
        ListAccessor listAccessor = listContainer.getListForTransmitting(clientPeerID, listPath.mainList);
        return getListAux(listAccessor, listPath.mainListLevel, listPath.innerLists, false, 0);
    }

    static Duple<ListAccessor, Integer> getListForWriting(ListContainer listContainer, PeerID serverPeerID, ListPath listPath) throws ListNotFoundException {
        ListAccessor listAccessor = listContainer.getListForReceiving(serverPeerID, listPath.mainList);
        return getListAux(listAccessor, listPath.mainListLevel, listPath.innerLists, true, 0);
    }

    private static Duple<ListAccessor, Integer> getListAux(ListAccessor listAccessor, int currentLevel, List<ListPath.IndexAndLevel> innerLists, boolean buildElementIfNeeded, int position) throws ListNotFoundException {
        if (position == innerLists.size()) {
            return new Duple<>(listAccessor, currentLevel);
        } else {
            try {
                listAccessor = listAccessor.getInnerList(innerLists.get(position).index, currentLevel, buildElementIfNeeded);
                int levelForInnerList = innerLists.get(position).level;
                return getListAux(listAccessor, levelForInnerList, innerLists, buildElementIfNeeded, position + 1);
            } catch (Exception e) {
                throw new ListNotFoundException();
            }
        }
    }
}
