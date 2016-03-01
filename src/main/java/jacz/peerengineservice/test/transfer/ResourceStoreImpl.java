package jacz.peerengineservice.test.transfer;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.datatransfer.ResourceStore;
import jacz.peerengineservice.util.datatransfer.ResourceStoreResponse;
import jacz.peerengineservice.util.datatransfer.resource_accession.BasicFileReader;

import java.io.FileNotFoundException;

/**
 * impl
 */
public class ResourceStoreImpl implements ResourceStore {

    public static String getHash(String resourceID) {
        switch (resourceID) {
            case "file_1":
                return "717ccd9c43e95bc3fb296df529ef0850";
            case "file_2":
                return "5368b7d0807615745512cf187e254049";
            case "file_3":
                return "45cc9177e5e4080cac426d9759a62641";
            case "file_4":
                return "4de39a9e54d1272c524a50ccff3679ad";
            case "file_5":
                return "7d5afa3195a2f872800b2ca4443b0f84";
            case "file_6":
                return "0a62fc26219082cc8ad568994a712fd3";
            case "file_7":
                return "dc8abfd2a07676e6dd82d3ca600ad03a";
            default:
                return null;
        }
    }

    @Override
    public ResourceStoreResponse requestResource(PeerId peerId, String resourceID) {
        try {
            switch (resourceID) {
                case "file_1":
                    return ResourceStoreResponse.resourceApproved(new BasicFileReader("etc/files/file_1.rar"));
                case "file_2":
                    return ResourceStoreResponse.resourceApproved(new BasicFileReader("etc/files/file_2.rar"));
                case "file_3":
                    return ResourceStoreResponse.resourceApproved(new BasicFileReader("etc/files/file_3.rar"));
                case "file_4":
                    return ResourceStoreResponse.resourceApproved(new BasicFileReader("etc/files/file_4.rar"));
                case "file_5":
                    return ResourceStoreResponse.resourceApproved(new BasicFileReader("etc/files/file_5.rar"));
                case "file_6":
                    return ResourceStoreResponse.resourceApproved(new BasicFileReader("etc/files/file_6.rar"));
                case "file_7":
                    return ResourceStoreResponse.resourceApproved(new BasicFileReader("etc/files/file_7.rar"));
                default:
                    return ResourceStoreResponse.resourceNotFound();
            }
        } catch (FileNotFoundException e) {
            return ResourceStoreResponse.resourceNotFound();
        }
    }
}
