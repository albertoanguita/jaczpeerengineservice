package jacz.peerengineservice.client;

import jacz.peerengineservice.PeerEncryption;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.datatransfer.ResourceTransferEvents;
import jacz.peerengineservice.util.tempfile_api.TempFileManager;

import java.io.IOException;
import java.util.Map;

/**
 * Client for tests
 */
public class Client {

    private PeerClient peerClient;

    TempFileManager tempFileManager;

    public Client(
            PeerId ownPeerId,
            String networkConfigurationPath,
            String peersPersonalDataPath,
            String peerKnowledgeBasePath,
            String peerConnectionConfigPath,
            String transferStatisticsPath,
            GeneralEventsImpl generalEvents,
            ConnectionEventsImpl connectionEvents,
            PeersEventsImpl peersEvents,
            Map<String, PeerFSMFactory> customFSMs) throws IOException {
        this(ownPeerId, networkConfigurationPath, peersPersonalDataPath, peerKnowledgeBasePath, peerConnectionConfigPath, transferStatisticsPath, generalEvents, connectionEvents, peersEvents, new ResourceTransferEventsImpl(), customFSMs);
    }

    public Client(
            PeerId ownPeerId,
            String networkConfigurationPath,
            String peersPersonalDataPath,
            String peerKnowledgeBasePath,
            String peerConnectionConfigPath,
            String transferStatisticsPath,
            GeneralEventsImpl generalEvents,
            ConnectionEventsImpl connectionEvents,
            PeersEventsImpl peersEvents,
            ResourceTransferEvents resourceTransferEvents,
            Map<String, PeerFSMFactory> customFSMs) throws IOException {
        this(ownPeerId, new PeerEncryption(new byte[0]), networkConfigurationPath, peersPersonalDataPath, peerKnowledgeBasePath, peerConnectionConfigPath, transferStatisticsPath, generalEvents, connectionEvents, peersEvents, resourceTransferEvents, customFSMs, null, null);
    }

    public Client(
            PeerId ownPeerId,
            PeerEncryption peerEncryption,
            String networkConfigurationPath,
            String peersPersonalDataPath,
            String peerKnowledgeBasePath,
            String peerConnectionConfigPath,
            String transferStatisticsPath,
            GeneralEventsImpl generalEvents,
            ConnectionEventsImpl connectionEvents,
            PeersEventsImpl peersEvents,
            ResourceTransferEvents resourceTransferEvents,
            Map<String, PeerFSMFactory> customFSMs,
            Map<String, DataAccessor> readingLists,
            Map<String, DataAccessor> writingLists) throws IOException {
        peersEvents.init(this);

        String serverURL = "https://jaczserver.appspot.com/_ah/api/server/v1/";
        ListContainer listContainer = new ListContainer(readingLists, writingLists);
//        if (FileUtil.isFile("globalDownloads.txt")) {
//            VersionedObjectSerializer.deserializeVersionedObject(globalDownloadStatistics);
//            globalDownloadStatistics = new GlobalDownloadStatistics();
//        } else {
//            globalDownloadStatistics = new GlobalDownloadStatistics();
//        }

        peerClient = new PeerClient(
                ownPeerId,
                serverURL,
                peerConnectionConfigPath,
                peerKnowledgeBasePath,
                peerEncryption,
                networkConfigurationPath,
                generalEvents,
                connectionEvents,
                peersEvents,
                resourceTransferEvents,
                peersPersonalDataPath,
                transferStatisticsPath,
                customFSMs,
                listContainer,
                null);

        tempFileManager = null;
    }

    public void startClient() throws IOException {
        peerClient.connect();
    }

    public void stopClient() {
        peerClient.stop();
        if (tempFileManager != null) {
            tempFileManager.stop();
        }
    }

    public PeerClient getPeerClient() {
        return peerClient;
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
