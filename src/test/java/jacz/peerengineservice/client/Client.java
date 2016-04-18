package jacz.peerengineservice.client;

import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineservice.PeerEncryption;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.connection.peers.PeerConnectionConfig;
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
 * Created by Alberto on 12/04/2016.
 */
public class Client {

    private static final String STATISTICS_PATH = "./statistics.dat";

    private PeerClient peerClient;

    TempFileManager tempFileManager;

    private TransferStatistics transferStatistics;

    public Client(
            PeerId ownPeerId,
            String networkConfigurationPath,
            String peersPersonalDataPath,
            String peerKnowledgeBasePath,
            GeneralEventsImpl generalEvents,
            ConnectionEventsImpl connectionEvents,
            Map<String, PeerFSMFactory> customFSMs) throws IOException {
        this(ownPeerId, networkConfigurationPath, peersPersonalDataPath, peerKnowledgeBasePath, generalEvents, connectionEvents, new ResourceTransferEventsImpl(), customFSMs);
    }

    public Client(
            PeerId ownPeerId,
            String networkConfigurationPath,
            String peersPersonalDataPath,
            String peerKnowledgeBasePath,
            GeneralEventsImpl generalEvents,
            ConnectionEventsImpl connectionEvents,
            ResourceTransferEvents resourceTransferEvents,
            Map<String, PeerFSMFactory> customFSMs) throws IOException {
        this(ownPeerId, new PeerEncryption(new byte[0]), networkConfigurationPath, peersPersonalDataPath, peerKnowledgeBasePath, generalEvents, connectionEvents, resourceTransferEvents, customFSMs, null, null);
    }

    public Client(
            PeerId ownPeerId,
            PeerEncryption peerEncryption,
            String networkConfigurationPath,
            String peersPersonalDataPath,
            String peerKnowledgeBasePath,
            GeneralEventsImpl generalEvents,
            ConnectionEventsImpl connectionEvents,
            ResourceTransferEvents resourceTransferEvents,
            Map<String, PeerFSMFactory> customFSMs,
            Map<String, DataAccessor> readingLists,
            Map<String, DataAccessor> writingLists) throws IOException {
        generalEvents.init(this);

        String serverURL = "https://jaczserver.appspot.com/_ah/api/server/v1/";
        ListContainer listContainer = new ListContainer(readingLists, writingLists);
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
        peerClient = new PeerClient(
                ownPeerId,
                serverURL,
                buildPeerConnectionConfig(),
                peerKnowledgeBasePath,
                peerEncryption,
                networkConfigurationPath,
                generalEvents,
                connectionEvents,
                resourceTransferEvents,
                peersPersonalDataPath,
                transferStatistics,
                customFSMs,
                listContainer,
                null);

        tempFileManager = null;
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
        if (tempFileManager != null) {
            tempFileManager.stop();
        }
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

    public TempFileManager getTempFileManager(String path) throws IOException {
        tempFileManager = new TempFileManager(path, new TempFileManagerEventsImpl());
        return tempFileManager;
    }

    private String formatPeer(PeerId peerId) {
        return "{" + peerId.toString().substring(40) + "}";
    }


    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("END!!!");
    }
}
