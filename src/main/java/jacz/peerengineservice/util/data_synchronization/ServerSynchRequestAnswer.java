package jacz.peerengineservice.util.data_synchronization;

import jacz.util.notification.ProgressNotificationWithError;

/**
 * An answer of a list accessor upon the request of synchronizing it as server
 */
public class ServerSynchRequestAnswer {

    public enum Type {
        OK,
        SERVER_BUSY
    }

    final Type type;

    final ProgressNotificationWithError<Integer, SynchError> progress;

    public ServerSynchRequestAnswer(Type type, ProgressNotificationWithError<Integer, SynchError> progress) {
        this.type = type;
        this.progress = progress;
    }
}
