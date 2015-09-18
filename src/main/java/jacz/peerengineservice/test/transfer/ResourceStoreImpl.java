package jacz.peerengineservice.test.transfer;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.datatransfer.ResourceStore;
import jacz.peerengineservice.util.datatransfer.ResourceStoreResponse;
import jacz.peerengineservice.util.datatransfer.resource_accession.BasicFileReader;

import java.io.FileNotFoundException;

/**
 * impl
 */
public class ResourceStoreImpl implements ResourceStore {

    @Override
    public ResourceStoreResponse requestResource(PeerID peerID, String resourceID) {
        if (resourceID.equals("aaa")) {
            try {
                //return ResourceStoreResponse.resourceApproved(new BasicFileReader("Breaking Bad 1x02 Cat's in the bag [720p x264][DUAL].mkv"));
                return ResourceStoreResponse.resourceApproved(new BasicFileReader("Entre.Copas.(DVDRip.Spanish.XviD.1.0.3.-.AC3.5.1)(SEDG).avi"));
//                return ResourceStoreResponse.resourceApproved(new BasicFileReader("Scrubs 1x01 Mi primer día.avi"));
            } catch (FileNotFoundException e) {
                return ResourceStoreResponse.resourceNotFound();
            }
        } else if (resourceID.equals("bbb")) {
            try {
                //return ResourceStoreResponse.resourceApproved(new BasicFileReader("Breaking Bad 1x02 Cat's in the bag [720p x264][DUAL].mkv"));
//                    return ResourceStoreResponse.resourceApproved(new BasicFileReader("Entre.Copas.(DVDRip.Spanish.XviD.1.0.3.-.AC3.5.1)(SEDG).avi"));
                return ResourceStoreResponse.resourceApproved(new BasicFileReader("Scrubs 1x01 Mi primer día.avi"));
            } catch (FileNotFoundException e) {
                return ResourceStoreResponse.resourceNotFound();
            }
        } else {
            return ResourceStoreResponse.resourceNotFound();
        }
    }
}
