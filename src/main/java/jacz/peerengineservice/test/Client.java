package jacz.peerengineservice.test;

import jacz.peerengineservice.client.*;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
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

    private SimplePeerClientActionImpl peerClientActionImpl;

    private PeerClientData peerClientData;

    private PeerRelations peerRelations;

    private Map<String, PeerFSMFactory> customFSMs;

    private PeerClient peerClient;

    private TestListContainer testListContainer;

    TempFileManager tempFileManager;

    private TransferStatistics transferStatistics;

    public Client(
            PeersPersonalData peersPersonalData,
            PeerClientData peerClientData,
            PeerRelations peerRelations,
            SimplePeerClientActionImpl peerClientActionImpl,
            Map<String, PeerFSMFactory> customFSMs) throws IOException {
        this(peersPersonalData, peerClientData, peerRelations, peerClientActionImpl, customFSMs, null, null);
    }

    public Client(
            PeersPersonalData peersPersonalData,
            PeerClientData peerClientData,
            PeerRelations peerRelations,
            SimplePeerClientActionImpl peerClientActionImpl,
            Map<String, PeerFSMFactory> customFSMs,
            Map<String, DataAccessor> readingLists,
            Map<String, DataAccessor> writingLists) throws IOException {
        this.peerClientData = peerClientData;
        this.peerRelations = peerRelations;
        this.peerClientActionImpl = peerClientActionImpl;
        this.customFSMs = customFSMs;
        peerClientActionImpl.init(peerClientData.getOwnPeerID(), this);

//        ownData = new PeerPersonalData(peerClientData.getOwnPeerID(), "", PeerPersonalData.State.UNDEFINED, "", peerClientActionImpl);
//        basicReadingLists.put(PeerPersonalData.getListName(), ownData);
        testListContainer = new TestListContainer(readingLists, writingLists);
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
        peerClient = new PeerClient(peerClientData, peerClientActionImpl, new ResourceTransferEventsImpl(), peersPersonalData, transferStatistics, peerRelations, customFSMs, new DataSynchEventsImpl(), testListContainer);

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
