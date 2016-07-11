package jacz.peerengineservice.util.tempfile_api;

/**
 * Created by Alberto on 13/10/2015.
 */
public interface TempFileManagerEvents {

    void indexFileGenerated(String indexFilePath);

    void indexFileRecovered(String indexFilePath);

    void indexFileErrorRestoredWithBackup(String indexFilePath);

    void indexFileError(String indexFilePath, Exception e);
}
