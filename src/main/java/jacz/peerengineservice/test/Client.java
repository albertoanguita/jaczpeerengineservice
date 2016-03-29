package jacz.peerengineservice.test;

import jacz.peerengineservice.PeerEncryption;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.*;
import jacz.peerengineservice.client.connection.NetworkConfiguration;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.datatransfer.ResourceTransferEvents;
import jacz.peerengineservice.util.datatransfer.TransferStatistics;
import jacz.peerengineservice.util.tempfile_api.TempFileManager;
import jacz.util.io.serialization.VersionedObjectSerializer;
import jacz.util.io.serialization.VersionedSerializationException;

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
            PeerId ownPeerId,
            NetworkConfiguration networkConfiguration,
            PeersPersonalData peersPersonalData,
            PeerRelations peerRelations,
            GeneralEventsImpl generalEvents,
            ConnectionEventsImpl connectionEvents,
            Map<String, PeerFSMFactory> customFSMs) throws IOException {
        this(ownPeerId, networkConfiguration, peersPersonalData, peerRelations, generalEvents, connectionEvents, new ResourceTransferEventsImpl(), customFSMs);
    }

    public Client(
            PeerId ownPeerId,
            NetworkConfiguration networkConfiguration,
            PeersPersonalData peersPersonalData,
            PeerRelations peerRelations,
            GeneralEventsImpl generalEvents,
            ConnectionEventsImpl connectionEvents,
            ResourceTransferEvents resourceTransferEvents,
            Map<String, PeerFSMFactory> customFSMs) throws IOException {
        this(ownPeerId, null, networkConfiguration, peersPersonalData, peerRelations, generalEvents, connectionEvents, resourceTransferEvents, customFSMs, null, null);
    }

    public Client(
            PeerId ownPeerId,
            PeerEncryption peerEncryption,
            NetworkConfiguration networkConfiguration,
            PeersPersonalData peersPersonalData,
            PeerRelations peerRelations,
            GeneralEventsImpl generalEvents,
            ConnectionEventsImpl connectionEvents,
            ResourceTransferEvents resourceTransferEvents,
            Map<String, PeerFSMFactory> customFSMs,
            Map<String, DataAccessor> readingLists,
            Map<String, DataAccessor> writingLists) throws IOException {
        generalEvents.init(ownPeerId, this);
        connectionEvents.init(ownPeerId, this);

//        String serverURL = "https://testserver01-1100.appspot.com/_ah/api/server/v1/";
        String serverURL = "https://jaczserver.appspot.com/_ah/api/server/v1/";
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
        peerClient = new PeerClient(ownPeerId, serverURL, peerEncryption, networkConfiguration, generalEvents, connectionEvents, resourceTransferEvents, peersPersonalData, transferStatistics, peerRelations, customFSMs, testListContainer, null);

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
