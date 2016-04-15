package jacz.peerengineservice.client.listsynch;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.util.notification.ProgressNotificationWithError;

/**
 *
 */
public class ListSynchProgress implements ProgressNotificationWithError<Integer, SynchError> {

    private PeerId otherPeerId;

    private String listName;

    private boolean client;

    private boolean success = false;

    public ListSynchProgress(PeerId otherPeerId, String listName, boolean client) {
        this.otherPeerId = otherPeerId;
        this.listName = listName;
        this.client = client;
        printInitMessage();
    }

    public boolean isSuccess() {
        return success;
    }

    private void printInitMessage() {
        String mode;
        if (client) {
            mode = "client";
        } else {
            mode = "server";
        }
        System.out.println("Started synchronizing list as " + mode + ". The other peer is: " + otherPeerId + ". List: " + listName);
    }

    @Override
    public void error(SynchError error) {
        System.out.println("Error in the synchronization of " + listName + ". Error: " + error.toString());
    }

    @Override
    public void timeout() {
        System.out.println("The synchronization of " + listName + " has failed");
    }

    @Override
    public void beginTask() {
        System.out.println("Synchronization of " + listName + " began...");
    }

    @Override
    public void addNotification(Integer message) {
        System.out.println("Progress of " + listName + ": " + message + "---------------------------------------------------------------------");
    }

    @Override
    public void completeTask() {
        System.out.println("Synchronization of " + listName + " complete!!!");
        success = true;
    }
}
