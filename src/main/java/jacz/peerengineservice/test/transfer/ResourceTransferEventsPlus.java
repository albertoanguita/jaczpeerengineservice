package jacz.peerengineservice.test.transfer;

import jacz.peerengineservice.test.ResourceTransferEventsImpl;
import jacz.peerengineservice.util.datatransfer.DownloadsManager;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;

/**
 * Created by Alberto on 10/12/2015.
 */
public class ResourceTransferEventsPlus extends ResourceTransferEventsImpl {

    @Override
    public void periodicDownloadsNotification(DownloadsManager downloadsManager) {
        super.periodicDownloadsNotification(downloadsManager);
        for (DownloadManager downloadManager : downloadsManager.getDownloads("files")) {
            Double speed = downloadManager.getStatistics().getSpeed();
            speed /= 1024d;
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
