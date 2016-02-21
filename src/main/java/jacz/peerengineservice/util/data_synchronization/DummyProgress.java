package jacz.peerengineservice.util.data_synchronization;

import jacz.util.notification.ProgressNotificationWithError;

/**
 * A dummy implementation of the progress monitor
 */
public class DummyProgress implements ProgressNotificationWithError<Integer, SynchError> {

    @Override
    public void error(SynchError error) {
        // ignore
    }

    @Override
    public void timeout() {
        // ignore
    }

    @Override
    public void beginTask() {
        // ignore
    }

    @Override
    public void addNotification(Integer message) {
        // ignore
    }

    @Override
    public void completeTask() {
        // ignore
    }
}
