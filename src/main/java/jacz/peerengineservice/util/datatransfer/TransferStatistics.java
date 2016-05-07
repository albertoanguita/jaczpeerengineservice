package jacz.peerengineservice.util.datatransfer;

import jacz.util.concurrency.timer.Timer;
import jacz.util.concurrency.timer.TimerAction;
import jacz.util.date_time.SpeedRegistry;
import jacz.util.io.serialization.localstorage.Updater;
import jacz.util.io.serialization.localstorage.VersionedLocalStorage;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * todo this is disabled for better performance. When enabled, we need either a daemon or a timer writing increases
 * in the background
 */
public class TransferStatistics implements Updater, TimerAction {

    private final static long SPEED_MILLIS_MEASURE = 3000L;

    private final static long SPEED_TIME_STORED = 1800000L;

    private final static long SPEED_MONITOR_FREQUENCY = 3000L;

    private static final String VERSION_0_1_0 = "0.1.0";

    private static final String CURRENT_VERSION = VERSION_0_1_0;

    private static final String UPLOADED_BYTES_GLOBAL = "uploadedBytesGlobal";

    private static final String DOWNLOADED_BYTES_GLOBAL = "downloadedBytesGlobal";

    private static final long STORE_ACCUMULATED_BYTES_PERIODICITY = 5000L;


    private final SpeedRegistry uploadSpeed;

    private final SpeedRegistry downloadSpeed;

    private final VersionedLocalStorage localStorage;

    private final AtomicLong accumulatedUploadedBytes;

    private final AtomicLong accumulatedDownloadedBytes;

    private final Timer storeAccumulatedBytesTimer;

    public TransferStatistics(String localStoragePath) throws IOException {
        uploadSpeed = new SpeedRegistry(SPEED_MILLIS_MEASURE, SPEED_TIME_STORED, SPEED_MONITOR_FREQUENCY);
        downloadSpeed = new SpeedRegistry(SPEED_MILLIS_MEASURE, SPEED_TIME_STORED, SPEED_MONITOR_FREQUENCY);
        localStorage = new VersionedLocalStorage(localStoragePath);
        accumulatedUploadedBytes = new AtomicLong(0);
        accumulatedDownloadedBytes = new AtomicLong(0);
        storeAccumulatedBytesTimer = new Timer(STORE_ACCUMULATED_BYTES_PERIODICITY, this, this.getClass().getName() + "StoreAccumulatedBytes");
    }

    public static TransferStatistics createNew(String localStoragePath) throws IOException {
        VersionedLocalStorage.createNew(localStoragePath, CURRENT_VERSION);
        TransferStatistics transferStatistics = new TransferStatistics(localStoragePath);
        transferStatistics.init();
        return transferStatistics;
    }

    private void init() {
        localStorage.setLong(UPLOADED_BYTES_GLOBAL, 0L);
        localStorage.setLong(DOWNLOADED_BYTES_GLOBAL, 0L);
    }

    public Date getCreationDate() {
        return localStorage.getCreationDate();
    }

    public void reset() {
        init();
    }

    public void addUploadedBytes(long bytes) {
        accumulatedUploadedBytes.getAndAdd(bytes);
        uploadSpeed.addProgress(bytes);
    }

    public void addDownloadedBytes(long bytes) {
        accumulatedDownloadedBytes.getAndAdd(bytes);
        downloadSpeed.addProgress(bytes);
    }

    public long getUploadedBytes() {
        return localStorage.getLong(UPLOADED_BYTES_GLOBAL);
    }

    public long getDownloadedBytes() {
        return localStorage.getLong(DOWNLOADED_BYTES_GLOBAL);
    }

    public synchronized Double[] getUploadSpeedRegistry() {
        return uploadSpeed.getRegistry();
    }

    public synchronized Double[] getDownloadSpeedRegistry() {
        return downloadSpeed.getRegistry();
    }

    public synchronized void stop() {
        uploadSpeed.stop();
        downloadSpeed.stop();
        storeAccumulatedBytesTimer.kill();
    }

    @Override
    public String update(VersionedLocalStorage versionedLocalStorage, String storedVersion) {
        // no versions yet, cannot be invoked
        return null;
    }

    @Override
    public Long wakeUp(Timer timer) {
        // store the accumulated uploaded and downloaded bytes to the local storage
        localStorage.setLong(UPLOADED_BYTES_GLOBAL, accumulatedUploadedBytes.get());
        localStorage.setLong(DOWNLOADED_BYTES_GLOBAL, accumulatedDownloadedBytes.get());
        return null;
    }
}
