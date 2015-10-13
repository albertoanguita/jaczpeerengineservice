package jacz.peerengineservice.util.tempfile_api;

/**
 * Created by Alberto on 13/10/2015.
 */
public interface TempFileManagerEvents {

    void indexFileErrorRestoredWithBackup(String indexFilePath);

    void indexFileError(String indexFilePath);
}
