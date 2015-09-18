package jacz.peerengineservice.test.list_synch;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.old.SynchronizeError;
import jacz.util.notification.ProgressNotificationWithError;

/**
 *
 */
public class ListSynchProgress implements ProgressNotificationWithError<Integer, SynchronizeError> {

    private PeerID otherPeerID;

    private String listName;

    private boolean client;

    private Object customObject;

    public ListSynchProgress(PeerID otherPeerID, String listName, boolean client) {
        this(otherPeerID, listName, client, null);
    }

    public ListSynchProgress(PeerID otherPeerID, String listName, boolean client, Object customObject) {
        this.otherPeerID = otherPeerID;
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
        System.out.println("Started synchronizing list as " + mode + ". The other peer is: " + otherPeerID + ". List: " + listName);
    }

    @Override
    public void error(SynchronizeError error) {
        System.out.println("Error in the synchronization of " + listName + ". Error: " + error.toString());
    }

    @Override
    public void timeout() {
        System.out.println("The synchronization of " + listName + " has failed");
    }

    @Override
    public void addNotification(Integer message) {
        System.out.println("Progress of " + listName + ": " + message / 100 + "---------------------------------------------------------------------");
    }

    @Override
    public void completeTask() {
        System.out.println("Synchronization of " + listName + " complete!!!");
        if (customObject != null) {
            System.out.println("custom!!");
        }
    }
}
