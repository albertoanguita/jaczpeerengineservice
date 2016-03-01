package jacz.peerengineservice.test.list_synch;

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

    private Object customObject;

    public ListSynchProgress(PeerId otherPeerId, String listName, boolean client) {
        this(otherPeerId, listName, client, null);
    }

    public ListSynchProgress(PeerId otherPeerId, String listName, boolean client, Object customObject) {
        this.otherPeerId = otherPeerId;
        this.listName = listName;
        this.client = client;
        this.customObject = customObject;
        printInitMessage();
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
        if (customObject != null) {
            System.out.println("custom!!");
        }
    }
}
