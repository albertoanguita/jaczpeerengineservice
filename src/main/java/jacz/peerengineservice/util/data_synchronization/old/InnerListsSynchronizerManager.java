package jacz.peerengineservice.util.data_synchronization.old;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClient;
import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.concurrency.task_executor.ParallelTaskExecutor;
import jacz.util.lists.Duple;
import jacz.util.notification.ProgressNotificationWithError;
import jacz.util.numeric.NumericUtil;

import java.util.List;

/**
 * Class for synchronizing a set of inner lists
 */
public class InnerListsSynchronizerManager implements ProgressNotificationWithError<Integer, SynchronizeError> {



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

    private final ListPath baseListPath;

    private final List<String> indexOfInnerLists;

    private final List<Integer> innerListLevels;

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



    public InnerListsSynchronizerManager(
            PeerClient peerClient,
            PeerID serverPeerID,
            long timeout,
            ListPath baseListPath,
            List<String> indexOfInnerLists,
            List<Integer> innerListLevels,
            ProgressNotificationWithError<Integer, SynchronizeError> progress) {
        this.peerClient = peerClient;
        this.serverPeerID = serverPeerID;
        this.timeout = timeout;
        this.baseListPath = baseListPath;
        this.indexOfInnerLists = indexOfInnerLists;
        this.innerListLevels = innerListLevels;
        this.progress = progress;
        ParallelTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                processTasks();
            }
        });
    }

    /**
     * Processes all pending tasks. Tasks are launched sequentially (a InnerListsSynchronizerManager is created for each of them)
     */
    private void processTasks() {
        taskCount = indexOfInnerLists.size();
        completedTaskCount = 0;
        processOneTask();
    }

    private void processOneTask() {
        if (!indexOfInnerLists.isEmpty()) {
            Duple<Integer, Integer> progressRange = NumericUtil.divideRange(0, ListSynchronizerManager.PROGRESS_MAX, taskCount, completedTaskCount);
            currentProgressMin = progressRange.element1;
            currentProgressMax = progressRange.element2;
            String indexForOneInnerList = indexOfInnerLists.remove(0);
            new ListSynchronizerManager(peerClient, serverPeerID, baseListPath, indexForOneInnerList, innerListLevels, timeout, this);
        } else {
            if (progress != null) {
                progress.completeTask();
            }
        }
    }

    @Override
    public void addNotification(Integer message) {
        // value from 0 to PROGRESS_MAX of the current task
        if (progress != null) {
            progress.addNotification(NumericUtil.displaceInRange(message, 0, ListSynchronizerManager.PROGRESS_MAX, currentProgressMin, currentProgressMax));
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
}
