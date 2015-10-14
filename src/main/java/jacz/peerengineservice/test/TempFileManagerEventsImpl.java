package jacz.peerengineservice.test;

import jacz.peerengineservice.util.tempfile_api.TempFileManager;
import jacz.peerengineservice.util.tempfile_api.TempFileManagerEvents;

/**
 * Created by Alberto on 13/10/2015.
 */
public class TempFileManagerEventsImpl implements TempFileManagerEvents {
    @Override
    public void indexFileGenerated(String indexFilePath) {

    }

    @Override
    public void indexFileRecovered(String indexFilePath) {

    }

    @Override
    public void indexFileErrorRestoredWithBackup(String indexFilePath) {

    }

    @Override
    public void indexFileError(String indexFilePath) {

    }
}
