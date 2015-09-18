package jacz.peerengineservice.util.data_synchronization.old;

import jacz.util.notification.ProgressNotificationWithError;

/**
 * An answer of a list accessor upon the request of synchronizing it as server
 */
public class ServerSynchRequestAnswer {

    public static enum Type {
        OK,
        SERVER_BUSY
    }

    final Type type;

    final ProgressNotificationWithError<Integer, SynchronizeError> progress;

    public ServerSynchRequestAnswer(Type type, ProgressNotificationWithError<Integer, SynchronizeError> progress) {
        this.type = type;
        this.progress = progress;
    }
}
