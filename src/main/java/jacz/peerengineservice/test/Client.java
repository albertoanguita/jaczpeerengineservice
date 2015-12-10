package jacz.peerengineservice.test;

import jacz.peerengineservice.client.*;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.datatransfer.ResourceTransferEvents;
import jacz.peerengineservice.util.datatransfer.TransferStatistics;
import jacz.peerengineservice.util.tempfile_api.TempFileManager;
import jacz.util.io.object_serialization.VersionedObjectSerializer;
import jacz.util.io.object_serialization.VersionedSerializationException;

import java.io.IOException;
import java.util.Map;

/**
 * Generic test client
 */
public class Client {

    private static final String STATISTICS_PATH = "./statistics.dat";

    private PeerClient peerClient;

    TempFileManager tempFileManager;

    private TransferStatistics transferStatistics;

    public Client(
            PeersPersonalData peersPersonalData,
            PeerClientData peerClientData,
            PeerRelations peerRelations,
            GeneralEventsImpl generalEvents,
            ConnectionEventsImpl connectionEvents,
            Map<String, PeerFSMFactory> customFSMs) throws IOException {
        this(peersPersonalData, peerClientData, peerRelations, generalEvents, connectionEvents, new ResourceTransferEventsImpl(), customFSMs, null, null);
    }

    public Client(
            PeersPersonalData peersPersonalData,
            PeerClientData peerClientData,
            PeerRelations peerRelations,
            GeneralEventsImpl generalEvents,
            ConnectionEventsImpl connectionEvents,
            ResourceTransferEvents resourceTransferEvents,
            Map<String, PeerFSMFactory> customFSMs) throws IOException {
        this(peersPersonalData, peerClientData, peerRelations, generalEvents, connectionEvents, resourceTransferEvents, customFSMs, null, null);
    }

    public Client(
            PeersPersonalData peersPersonalData,
            PeerClientData peerClientData,
            PeerRelations peerRelations,
            GeneralEventsImpl generalEvents,
            ConnectionEventsImpl connectionEvents,
            ResourceTransferEvents resourceTransferEvents,
            Map<String, PeerFSMFactory> customFSMs,
            Map<String, DataAccessor> readingLists,
            Map<String, DataAccessor> writingLists) throws IOException {
        generalEvents.init(peerClientData.getOwnPeerID(), this);
        connectionEvents.init(peerClientData.getOwnPeerID(), this);

        TestListContainer testListContainer = new TestListContainer(readingLists, writingLists);
//        if (FileUtil.isFile("globalDownloads.txt")) {
//            VersionedObjectSerializer.deserializeVersionedObject(globalDownloadStatistics);
//            globalDownloadStatistics = new GlobalDownloadStatistics();
//        } else {
//            globalDownloadStatistics = new GlobalDownloadStatistics();
//        }

//        transferStatistics = new TransferStatistics(FileReaderWriter.readBytes(STATISTICS_PATH));
        try {
//            byte[] data = FileReaderWriter.readBytes(STATISTICS_PATH);
//            VersionedObjectSerializer.deserialize(transferStatistics, data);
            transferStatistics = new TransferStatistics(STATISTICS_PATH);
        } catch (IOException | VersionedSerializationException e) {
            transferStatistics = new TransferStatistics();
        }
        peerClient = new PeerClient(peerClientData, generalEvents, connectionEvents, resourceTransferEvents, peersPersonalData, transferStatistics, peerRelations, customFSMs, testListContainer);

        tempFileManager = new TempFileManager("./etc/temp", new TempFileManagerEventsImpl());
    }

    public void startClient() throws IOException {
        peerClient.connect();
    }

    public void stopClient() {
        peerClient.stop();
        tempFileManager.stop();
        transferStatistics.stop();
        try {
            VersionedObjectSerializer.serialize(transferStatistics, 4, STATISTICS_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PeerClient getPeerClient() {
        return peerClient;
    }

    public TransferStatistics getTransferStatistics() {
        return transferStatistics;
    }

    public void disconnect() {
        peerClient.disconnect();
    }

    public TempFileManager getTempFileManager() {
        return tempFileManager;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("END!!!");
    }
}
