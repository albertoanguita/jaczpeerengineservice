package jacz.peerengine.test.personal_data_lists;

import jacz.peerengine.util.data_synchronization.old.SynchronizeError;
import jacz.util.notification.ProgressNotificationWithError;
import jacz.peerengine.PeerID;
import jacz.peerengine.util.data_synchronization.premade_lists.old.BasicServerSynchProgressFactoryInterface;

/**
 * Class description
 * <p/>
 * User: Alberto<br>
 * Date: 15-may-2011<br>
 * Last Modified: 15-may-2011
 */
public class BasicServerSynchProgressFactory implements BasicServerSynchProgressFactoryInterface {

    @Override
    public ProgressNotificationWithError<Integer, SynchronizeError> initialize(PeerID peerID, String listName, int level) {
        return new ListSynchProgress(peerID, listName, level, false);
    }
}
