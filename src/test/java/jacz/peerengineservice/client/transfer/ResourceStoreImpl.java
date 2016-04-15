package jacz.peerengineservice.client.transfer;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.datatransfer.ResourceStore;
import jacz.peerengineservice.util.datatransfer.ResourceStoreResponse;
import jacz.peerengineservice.util.datatransfer.resource_accession.BasicFileReader;

import java.io.FileNotFoundException;

/**
 * impl
 */
public class ResourceStoreImpl implements ResourceStore {

    private final String fileId;

    private final String filePath;

    public ResourceStoreImpl(String fileId, String filePath) {
        this.fileId = fileId;
        this.filePath = filePath;
    }

    @Override
    public ResourceStoreResponse requestResource(PeerId peerId, String resourceID) {
        try {
            if (fileId.equals(resourceID)) {
                return ResourceStoreResponse.resourceApproved(new BasicFileReader(filePath));
            } else {
                return ResourceStoreResponse.resourceNotFound();
            }
        } catch (FileNotFoundException e) {
            return ResourceStoreResponse.resourceNotFound();
        }
    }
}
