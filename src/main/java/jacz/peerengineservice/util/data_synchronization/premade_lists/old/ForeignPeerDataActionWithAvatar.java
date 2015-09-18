package jacz.peerengineservice.util.data_synchronization.premade_lists.old;

import jacz.util.hash.hashdb.FileHashDatabase;
import jacz.peerengineservice.PeerID;

/**
 * Interface for controlling changes in other peers personal data values
 */
public interface ForeignPeerDataActionWithAvatar extends ForeignPeerDataAction {

    void newPeerPicture(PeerID peerID, String filePath);

    void newPeerPicture(PeerID peerID, String pictureHash, FileHashDatabase fileHashDatabase);

    void errorWritingNewPictureFile(PeerID peerID, byte[] data);
}
