package jacz.peerengine.util.data_synchronization.premade_lists.old;

import jacz.peerengine.util.data_synchronization.old.SynchronizeError;
import jacz.util.notification.ProgressNotificationWithError;
import jacz.peerengine.PeerID;

/**
 *
 */
public interface BasicServerSynchProgressFactoryInterface {

    ProgressNotificationWithError<Integer, SynchronizeError> initialize(PeerID peerID, String listName, int level);
}
