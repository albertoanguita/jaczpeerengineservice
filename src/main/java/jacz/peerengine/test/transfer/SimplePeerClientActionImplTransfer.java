package jacz.peerengine.test.transfer;

import jacz.peerengine.test.SimplePeerClientActionImpl;
import jacz.peerengine.util.datatransfer.DownloadsManager;
import jacz.peerengine.util.datatransfer.master.DownloadManager;

/**
 * Class description
 * <p/>
 * User: Alberto<br>
 * Date: 5/05/12<br>
 * Last Modified: 5/05/12
 */
public class SimplePeerClientActionImplTransfer extends SimplePeerClientActionImpl {

    @Override
    public void periodicDownloadsNotification(DownloadsManager downloadsManager) {
        super.periodicDownloadsNotification(downloadsManager);
        for (DownloadManager downloadManager : downloadsManager.getDownloads("files")) {
            Double speed = downloadManager.getStatistics().getSpeed();
            if (speed != null) {
                speed /= 1024d;
            }
            long size = downloadManager.getStatistics().getDownloadedSizeThisResource();
            Long length = downloadManager.getLength();
            Double part = null;
            if (length != null) {
                part = (double) size / (double) length * 100d;
            }
            if (part != null && part > 100d) {
                System.out.println("AHHH. Length = " + length + " / size = " + size);
                throw new RuntimeException();
            }
            System.out.println("Speed for " + downloadManager.getResourceID() + ": " + speed + "KB, downloaded part: " + part);
        }
    }
}
