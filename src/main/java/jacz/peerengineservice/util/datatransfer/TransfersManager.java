package jacz.peerengineservice.util.datatransfer;

import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.concurrency.task_executor.ParallelTaskExecutor;
import jacz.util.concurrency.timer.SimpleTimerAction;
import jacz.util.concurrency.timer.Timer;
import jacz.util.identifier.UniqueIdentifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that handles active transfers (used for downloads and uploads)
 */
public abstract class TransfersManager<T> implements SimpleTimerAction {

    /**
     * Active uploads, indexed by store name and slave resource streamer id
     */
    private Map<String, Map<UniqueIdentifier, T>> activeTransfers;

    /**
     * Timer for periodic reports
     */
    private final Timer timer;

    public TransfersManager(String threadName) {
        activeTransfers = new HashMap<String, Map<UniqueIdentifier, T>>();
        timer = new Timer(1, this, false, threadName);
    }

    /**
     * Adds a new download
     *
     * @param store    resource store corresponding to this upload
     * @param transfer slave that is added
     */
    synchronized void addTransfer(String store, UniqueIdentifier id, T transfer) {
        if (!activeTransfers.containsKey(store)) {
            activeTransfers.put(store, new HashMap<UniqueIdentifier, T>());
        }
        activeTransfers.get(store).put(id, transfer);
    }

    /**
     * Removes a download
     *
     * @param store store then this upload is located
     * @param id    id of the download to remove
     */
    synchronized T removeTransfer(String store, UniqueIdentifier id) {
//        try {
            return activeTransfers.get(store).remove(id);
//        } catch (NullPointerException e) {
//            System.out.println("NULL REMOVE TRANSFER");
//            for (Map.Entry<String, Map<UniqueIdentifier, T>> entry : activeTransfers.entrySet()) {
//                System.out.println(entry.getKey());
//                System.out.println(entry.getValue());
//            }
//            throw e;
//        }
    }

    /**
     * Retrieves a shallow copy of the active downloads for a specific store
     *
     * @return a shallow copy of the active uploads of a store
     */
    protected synchronized List<T> getTransfers(String store) {
        if (activeTransfers.containsKey(store)) {
            return new ArrayList<T>(activeTransfers.get(store).values());
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves a shallow copy of the active downloads (visible plus invisible)
     *
     * @return a shallow copy of the active downloads
     */
    protected synchronized List<T> getAllTransfers() {
        ArrayList<T> result = new ArrayList<T>();
        for (String store : activeTransfers.keySet()) {
            result.addAll(getTransfers(store));
        }
        return result;
    }

    /**
     * Set the timer for periodic notifications
     *
     * @param millis time in millis for the timer
     */
    public synchronized void setTimer(long millis) {
        timer.reset(millis);
    }

    /**
     * Stops notifying downloads
     */
    public synchronized void stopTimer() {
        timer.stop();
    }

    /**
     * Stops this class threads, so it will not be usable anymore
     */
    synchronized void stop() {
        timer.kill();
    }


    @Override
    /**
     * Visible downloads are notified to the client
     */
    public synchronized Long wakeUp(final Timer timer) {
        // notify the client. Currently we always send a false, maybe use the argument in the future
        ParallelTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                notifyClient();
            }
        });
        return null;
    }

    protected abstract void notifyClient();
}
