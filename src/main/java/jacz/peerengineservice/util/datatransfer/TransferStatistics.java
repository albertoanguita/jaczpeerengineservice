package jacz.peerengineservice.util.datatransfer;

import jacz.util.date_time.SpeedRegistry;
import jacz.util.io.serialization.localstorage.Updater;
import jacz.util.io.serialization.localstorage.VersionedLocalStorage;

import java.io.IOException;
import java.util.Date;

/**
 * Created by Alberto on 19/04/2016.
 */
public class TransferStatistics implements Updater {

    private final static long SPEED_MILLIS_MEASURE = 3000L;

    private final static long SPEED_TIME_STORED = 1800000L;

    private final static long SPEED_MONITOR_FREQUENCY = 3000L;

    private static final String VERSION_0_1_0 = "0.1.0";

    private static final String CURRENT_VERSION = VERSION_0_1_0;

    private static final String UPLOADED_BYTES_GLOBAL = "uploadedBytesGlobal";

    private static final String DOWNLOADED_BYTES_GLOBAL = "downloadedBytesGlobal";


    private final SpeedRegistry uploadSpeed;

    private final SpeedRegistry downloadSpeed;

    private final VersionedLocalStorage localStorage;

    public TransferStatistics(String localStoragePath) throws IOException {
        uploadSpeed = new SpeedRegistry(SPEED_MILLIS_MEASURE, SPEED_TIME_STORED, SPEED_MONITOR_FREQUENCY);
        downloadSpeed = new SpeedRegistry(SPEED_MILLIS_MEASURE, SPEED_TIME_STORED, SPEED_MONITOR_FREQUENCY);
        localStorage = new VersionedLocalStorage(localStoragePath);
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
        increaseField(UPLOADED_BYTES_GLOBAL, bytes);
        uploadSpeed.addProgress(bytes);
    }

    public void addDownloadedBytes(long bytes) {
        increaseField(DOWNLOADED_BYTES_GLOBAL, bytes);
        downloadSpeed.addProgress(bytes);
    }

    public long getUploadedBytes() {
        return localStorage.getLong(UPLOADED_BYTES_GLOBAL);
    }

    public long getDownloadedBytes() {
        return localStorage.getLong(DOWNLOADED_BYTES_GLOBAL);
    }

    private void increaseField(String key, long value) {
        localStorage.setLong(key, localStorage.getLong(key) + value);
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
    }

    @Override
    public String update(VersionedLocalStorage versionedLocalStorage, String storedVersion) {
        // no versions yet, cannot be invoked
        return null;
    }
}
