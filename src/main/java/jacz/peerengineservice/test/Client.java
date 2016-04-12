package jacz.peerengineservice.test;

import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineservice.PeerEncryption;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.client.PeerFSMFactory;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeersPersonalData;
import jacz.peerengineservice.client.connection.NetworkConfiguration;
import jacz.peerengineservice.client.connection.peers.PeerConnectionConfig;
import jacz.peerengineservice.client.connection.peers.kb.Management;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.datatransfer.ResourceTransferEvents;
import jacz.peerengineservice.util.datatransfer.TransferStatistics;
import jacz.peerengineservice.util.tempfile_api.TempFileManager;
import jacz.util.io.serialization.VersionedObjectSerializer;
import jacz.util.io.serialization.VersionedSerializationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Generic test client
 */
public class Client {

    public static final String NETWORK_CONFIGURATION_PATH = "networkConfig.dat";

    public static final String PEERS_PERSONAL_DATA_PATH = "peersPersonalData.dat";

    private static final String STATISTICS_PATH = "./statistics.dat";

    private static final String DB_PATH = "test_peerKB.db";

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

        String serverURL = "https://jaczserver.appspot.com/_ah/api/server/v1/";
        TestListContainer testListContainer = new TestListContainer(readingLists, writingLists);
//        if (FileUtil.isFile("globalDownloads.txt")) {
//            VersionedObjectSerializer.deserializeVersionedObject(globalDownloadStatistics);
//            globalDownloadStatistics = new GlobalDownloadStatistics();
//        } else {
//            globalDownloadStatistics = new GlobalDownloadStatistics();
//        }

        try {
            transferStatistics = new TransferStatistics(STATISTICS_PATH);
        } catch (IOException | VersionedSerializationException e) {
            transferStatistics = new TransferStatistics();
        }
        Management.dropAndCreateKBDatabase(DB_PATH);
        peerClient = new PeerClient(
                ownPeerId,
                serverURL,
                buildPeerConnectionConfig(),
                DB_PATH,
                peerEncryption,
                NETWORK_CONFIGURATION_PATH,
                generalEvents,
                connectionEvents,
                resourceTransferEvents,
                PEERS_PERSONAL_DATA_PATH,
                transferStatistics,
                customFSMs,
                testListContainer,
                null);

        tempFileManager = new TempFileManager("./etc/temp", new TempFileManagerEventsImpl());
    }

    private PeerConnectionConfig buildPeerConnectionConfig() {
        return new PeerConnectionConfig(
                100, false, CountryCode.ES, new ArrayList<>(), 10
        );
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
