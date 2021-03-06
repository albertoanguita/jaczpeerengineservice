package jacz.peerengineservice.util.datatransfer;

import org.aanguita.jacuzzi.concurrency.task_executor.ThreadExecutor;
import org.aanguita.jacuzzi.concurrency.timer.Timer;
import org.aanguita.jacuzzi.concurrency.timer.TimerAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class that handles active transfers (used for downloads and uploads)
 */
public abstract class TransfersManager<T> implements TimerAction {

    /**
     * Active uploads, indexed by store name and resource streamer id (slave id or master id)
     */
    private final Map<String, Map<String, T>> activeTransfers;

    /**
     * Timer for periodic reports
     */
    private final Timer timer;

    private final AtomicBoolean alive;

    private final String threadExecutorClientId;

    public TransfersManager(String threadName) {
        activeTransfers = new HashMap<>();
        timer = new Timer(1, this, false, threadName);
        alive = new AtomicBoolean(true);
        threadExecutorClientId = ThreadExecutor.registerClient(this.getClass().getName());
    }

    /**
     * Adds a new download
     *
     * @param store    resource store corresponding to this upload
     * @param transfer slave that is added
     */
    synchronized void addTransfer(String store, String id, T transfer) {
        if (!activeTransfers.containsKey(store)) {
            activeTransfers.put(store, new HashMap<>());
        }
        activeTransfers.get(store).put(id, transfer);
    }

    /**
     * Removes a download
     *
     * @param store store then this upload is located
     * @param id    id of the download to remove
     */
    synchronized T removeTransfer(String store, String id) {
        return activeTransfers.get(store).remove(id);
    }

    /**
     * Retrieves a shallow copy of the active downloads for a specific store
     *
     * @return a shallow copy of the active uploads of a store
     */
    protected synchronized List<T> getTransfers(String store) {
        if (activeTransfers.containsKey(store)) {
            return new ArrayList<>(activeTransfers.get(store).values());
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
        ArrayList<T> result = new ArrayList<>();
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
        if (alive.get()) {
            alive.set(false);
            timer.stop();
            ThreadExecutor.shutdownClient(threadExecutorClientId);
        }
    }


    @Override
    /**
     * Visible downloads are notified to the client
     */
    public synchronized Long wakeUp(final Timer timer) {
        // notify the client. Currently we always send a false, maybe use the argument in the future
        ThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                notifyClient();
            }
        });
        return null;
    }

    protected abstract void notifyClient();
}
