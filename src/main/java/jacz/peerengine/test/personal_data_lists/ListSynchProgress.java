package jacz.peerengine.test.personal_data_lists;

import jacz.peerengine.PeerID;
import jacz.peerengine.util.data_synchronization.old.SynchronizeError;
import jacz.util.notification.ProgressNotificationWithError;

/**
 * basic synch progress
 */
public class ListSynchProgress implements ProgressNotificationWithError<Integer, SynchronizeError> {

    private final PeerID peerID;

    private final String listName;

    private final int level;

    private final boolean client;

    public ListSynchProgress(PeerID peerID, String listName, int level, boolean client) {
        this.peerID = peerID;
        this.listName = listName;
        this.level = level;
        this.client = client;
        printInitMessage();
    }

    private void printInitMessage() {
        String mode;
        if (client) {
            mode = "client";
        } else {
            mode = "server";
        }
        System.out.println("Started synchronizing list as " + mode + " with " + peerID + ". List: " + listName + ". Level: " + level);
    }

    @Override
    public void error(SynchronizeError error) {
        System.out.println("Error in the synchronization of " + listName + "/" + level + ". " + error.type + ": " + error.details);
    }

    @Override
    public void timeout() {
        System.out.println("The synchronization of " + listName + "/" + level + " has failed");
    }

    @Override
    public void addNotification(Integer message) {
        System.out.println("Progress of " + listName + "/" + level + ": " + message / 100 + "---------------------------------------------------------------------");
    }

    @Override
    public void completeTask() {
        System.out.println("Synchronization of " + listName + "/" + level + " complete!!!");
    }
}
