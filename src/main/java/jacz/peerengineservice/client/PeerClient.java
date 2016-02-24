package jacz.peerengineservice.client;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.commengine.communication.CommError;
import jacz.peerengineservice.NotAliveException;
import jacz.peerengineservice.PeerEncryption;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.client.connection.*;
import jacz.peerengineservice.util.ChannelConstants;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.peerengineservice.util.ForeignStoreShare;
import jacz.peerengineservice.util.data_synchronization.DataAccessorContainer;
import jacz.peerengineservice.util.data_synchronization.DataSynchServerFSM;
import jacz.peerengineservice.util.data_synchronization.DataSynchServerFSMFactory;
import jacz.peerengineservice.util.data_synchronization.DataSynchronizer;
import jacz.peerengineservice.util.datatransfer.*;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceWriter;
import jacz.peerengineservice.util.datatransfer.slave.UploadManager;
import jacz.util.event.notification.NotificationReceiver;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.io.serialization.ObjectListWrapper;
import jacz.util.log.ErrorFactory;
import jacz.util.log.ErrorHandler;
import jacz.util.log.ErrorLog;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is the entry point to the whole peer engine API. It contains the main methods for initializing and
 * controlling the peer engine, and handles part of the connection process with other peers.
 */
public class PeerClient {

    public static final String VERSION = "0.1";

    public static final String OWN_CUSTOM_PREFIX = "@@@";

    public static final String ERROR_LOG = "PEER_CLIENT_ERROR_LOG";

    public static final String MANUAL_REMOVE_BAG = "PEER_CLIENT_MANUAL_REMOVE_BAG";


    /**
     * Our own peer ID
     */
    private final PeerID ownPeerID;

    /**
     * Peer encryption object that defines the authentication information for this peer. Not currently used.
     */
    private final PeerEncryption peerEncryption;

    /**
     * Actions invoked by the PeerClient upon some events (connection of a new peer, new chat message, etc)
     */
    private final GeneralEventsBridge generalEvents;

    /**
     * Own and other peers personal data (nick)
     */
    private final PeersPersonalData peersPersonalData;

    /**
     * The PeerClientConnectionManager employed by this PeerClient. It will handle the search and connection with friend peers
     */
    private final PeerClientConnectionManager peerClientConnectionManager;

    private final ConnectedPeers connectedPeers;

    private final ConnectedPeersMessenger connectedPeersMessenger;

    /**
     * The DataStreamingManager employed by this PeerClient, for taking care of the transfer of files between peers
     */
    private final ResourceStreamingManager resourceStreamingManager;

    private final PeerRelations peerRelations;

    /**
     * The different factories of custom FSMs that our client wishes to use (custom FSM are defined by the client,
     * who simply provides a way to create and initialize those FSM upon their request, each with a different name)
     */
    private final Map<String, PeerFSMFactory> customFSMs;

    /**
     * Object for synchronizing data (utility for the user)
     * <p/>
     * NULL if not used
     */
    private final DataSynchronizer dataSynchronizer;

    private static ErrorHandler errorHandler;

    /**
     * Class constructor
     *
     * @param connectionEvents      actions invoked upon some events, like the connection with a peer, or receiving a chat
     *                              message
     * @param peerRelations         relations with other peers. This object will be updated during the execution, as we add or reject peers
     * @param customFSMs            list of factories for building custom FSMs, each with a different name
     * @param dataAccessorContainer container for sharing lists of data with peers using the provided list synchronization methods (optional, null if not used)
     */
    public PeerClient(
            PeerID ownPeerID,
            String serverURL,
            PeerEncryption peerEncryption,
            NetworkConfiguration networkConfiguration,
            GeneralEvents generalEvents,
            ConnectionEvents connectionEvents,
            ResourceTransferEvents resourceTransferEvents,
            PeersPersonalData peersPersonalData,
            TransferStatistics transferStatistics,
            PeerRelations peerRelations,
            Map<String, PeerFSMFactory> customFSMs,
            DataAccessorContainer dataAccessorContainer,
            ErrorHandler errorHandler) {
        this.ownPeerID = ownPeerID;
        this.peerEncryption = peerEncryption;
        this.generalEvents = new GeneralEventsBridge(generalEvents);
        this.peersPersonalData = peersPersonalData;
        this.peerRelations = peerRelations;
        this.customFSMs = customFSMs;

        connectedPeers = new ConnectedPeers(ChannelConstants.REQUEST_DISPATCHER_CHANNEL, ChannelConstants.RESOURCE_STREAMING_MANAGER_CHANNEL, ChannelConstants.CONNECTION_ESTABLISHMENT_CHANNEL);
        connectedPeersMessenger = new ConnectedPeersMessenger(connectedPeers, ChannelConstants.REQUEST_DISPATCHER_CHANNEL);

        PeerClientPrivateInterface peerClientPrivateInterface = new PeerClientPrivateInterface(this);
        peerClientConnectionManager = new PeerClientConnectionManager(
                connectionEvents,
                peerClientPrivateInterface,
                connectedPeers,
                ownPeerID,
                serverURL,
                networkConfiguration,
                peerRelations);
        resourceStreamingManager = new ResourceStreamingManager(ownPeerID, resourceTransferEvents, connectedPeersMessenger, transferStatistics, ResourceStreamingManager.DEFAULT_PART_SELECTION_ACCURACY);
        // initialize the list synchronizer utility (better here than in the client side)
        dataSynchronizer = new DataSynchronizer(this, dataAccessorContainer);
        PeerClient.errorHandler = new ErrorHandlerBridge(this, errorHandler);
        // add custom FSMs for list synchronizing service, in case the client uses it
        addOwnCustomFSMs(this.customFSMs);
    }


    /**
     * This method disconnects and stops all created resources. It should be invoked when the peer engine is no longer going to be used
     * <p/>
     * The method is blocking, in the sense that when it concludes, all connections are closed, and there will be no further changes in the engine
     */
    public void stop() {
        resourceStreamingManager.stop();
        peerClientConnectionManager.stop();
        connectedPeers.stop();
        generalEvents.stop();
    }

    private void addOwnCustomFSMs(Map<String, PeerFSMFactory> customFSMs) {
        if (customFSMs.containsKey(DataSynchServerFSM.CUSTOM_FSM_NAME)) {
            System.err.println("Reserved custom FSM name: " + DataSynchServerFSM.CUSTOM_FSM_NAME);
        }
        customFSMs.put(DataSynchServerFSM.CUSTOM_FSM_NAME, new DataSynchServerFSMFactory(getDataSynchronizer()));
//        if (customFSMs.containsKey(ListSynchronizerServerFSM.CUSTOM_FSM_NAME)) {
//            System.err.println("Reserved custom FSM name: " + ListSynchronizerServerFSM.CUSTOM_FSM_NAME);
//        }
//        customFSMs.put(ListSynchronizerServerFSM.CUSTOM_FSM_NAME, new ListSynchronizerServerFSMFactory(getListSynchronizer()));
//        if (customFSMs.containsKey(ElementSynchronizerServerFSM.CUSTOM_FSM_NAME)) {
//            System.err.println("Reserved custom FSM name: " + ElementSynchronizerServerFSM.CUSTOM_FSM_NAME);
//        }
//        customFSMs.put(ElementSynchronizerServerFSM.CUSTOM_FSM_NAME, new ElementSynchronizerServerFSMFactory(getListSynchronizer()));
    }

    public static void reportError(String message, Object... data) {
        if (errorHandler != null) {
            ErrorFactory.reportError(errorHandler, message, data);
        } else {
            ErrorLog.reportError(ERROR_LOG, message, data);
        }
    }

    public DataSynchronizer getDataSynchronizer() {
        return dataSynchronizer;
    }

    public synchronized void connect() {
        peerClientConnectionManager.setWishForConnection(true);
    }


    public synchronized void disconnect() {
        peerClientConnectionManager.setWishForConnection(false);
    }

    public synchronized PeerID getOwnPeerID() {
        return ownPeerID;
    }

    public PeerEncryption getPeerEncryption() {
        return peerEncryption;
    }

    public synchronized NetworkConfiguration getNetworkConfiguration() {
        return peerClientConnectionManager.buildNetworkConfiguration();
    }

    public synchronized PeersPersonalData getPeersPersonalData() {
        return peersPersonalData;
    }

    public synchronized int getListeningPort() {
        return peerClientConnectionManager.getListeningPort();
    }

    public synchronized void setListeningPort(int port) {
        peerClientConnectionManager.setListeningPort(port);
    }

    public synchronized State getConnectionState() {
        return peerClientConnectionManager.getConnectionState();
    }

    public synchronized PeerRelations getPeerRelations() {
        return peerRelations;
    }

    public synchronized boolean isFriendPeer(PeerID peerID) {
        return peerRelations.isFriendPeer(peerID);
    }

    public synchronized boolean isBlockedPeer(PeerID peerID) {
        return peerRelations.isBlockedPeer(peerID);
    }

    public synchronized boolean isNonRegisteredPeer(PeerID peerID) {
        return peerRelations.isNonRegisteredPeer(peerID);
    }

    public synchronized Set<PeerID> getFriendPeers() {
        return peerRelations.getFriendPeers();
    }

    public synchronized void addFriendPeer(final PeerID peerID) {
        peerRelations.addFriendPeer(peerID);
        searchFriends();
        if (connectedPeers.getPeerConnectionStatus(peerID) == ConnectionStatus.UNVALIDATED) {
            connectedPeers.setPeerConnectionStatus(peerID, ConnectionStatus.CORRECT);
            sendObjectMessage(peerID, new ValidationMessage());
        }
        generalEvents.peerAddedAsFriend(peerID, peerRelations);
    }

    public synchronized void removeFriendPeer(final PeerID peerID) {
        peerRelations.removeFriendPeer(peerID);
        searchFriends();
        generalEvents.peerRemovedAsFriend(peerID, peerRelations);
    }

    public synchronized Set<PeerID> getBlockedPeers() {
        return peerRelations.getBlockedPeers();
    }

    public synchronized void addBlockedPeer(final PeerID peerID) {
        peerRelations.addBlockedPeer(peerID);
        searchFriends();
        generalEvents.peerAddedAsBlocked(peerID, peerRelations);
    }

    public synchronized void removeBlockedPeer(final PeerID peerID) {
        peerRelations.removeBlockedPeer(peerID);
        generalEvents.peerRemovedAsBlocked(peerID, peerRelations);
    }

    public PeerID getNextConnectedPeer(PeerID peerID) {
        return connectedPeers.getNextConnectedPeer(peerID);
    }

    /**
     * This method forces the peer engine to search for connected friends. In principle, it is not necessary to use
     * this method, since this search is automatically performed when the PeerClient connects to a PeerServer. Still,
     * there might be cases in which it is recommendable (to search for a friend peer who has not listed us as friend,
     * since he will not try to connect to us, etc)
     */
    public void searchFriends() {
        peerClientConnectionManager.searchFriends();
    }

    /**
     * Adds a store containing resources that we share to the rest of peers. It is used for handling download requests
     * incoming from other peers
     *
     * @param name  name of the resource store
     * @param store implementation of the resource store, for requesting resources to our client
     */
    public synchronized void addLocalResourceStore(String name, ResourceStore store) {
        resourceStreamingManager.addLocalResourceStore(name, store);
    }

    /**
     * Sets the local general resource store
     *
     * @param generalResourceStore general resource store
     */
    public synchronized void setLocalGeneralResourceStore(GeneralResourceStore generalResourceStore) {
        resourceStreamingManager.setLocalGeneralResourceStore(generalResourceStore);
    }

    /**
     * Adds a store of resources shared to us by other peers. It it used to handle downloads from other peers
     *
     * @param name              name of the resource store
     * @param foreignStoreShare peers share for letting us know the share of resources of each peer
     */
    public synchronized void addForeignResourceStore(String name, ForeignStoreShare foreignStoreShare) {
        resourceStreamingManager.addForeignResourceStore(name, foreignStoreShare);
    }

    /**
     * Removes an already defined local store
     *
     * @param name name of the store to remove
     */
    public synchronized void removeLocalResourceStore(String name) {
        resourceStreamingManager.removeLocalResourceStore(name);
    }

    /**
     * Removes the local general resource store, so only the registered stores will be used
     */
    public synchronized void removeLocalGeneralResourceStore() {
        resourceStreamingManager.removeLocalGeneralResourceStore();
    }

    /**
     * Removes an already defined foreign store
     *
     * @param name name of the store to remove
     */
    public synchronized void removeForeignResourceStore(String name) {
        resourceStreamingManager.removeForeignResourceStore(name);
    }

    /**
     * Initiates the process for downloading a resource from a defined global store. The data streaming manager will
     * try to get the resource from very peer sharing it (he will look in the related peers share to find appropriate
     * peers)
     *
     * @param resourceStoreName                   name of the store allocating the resource
     * @param resourceID                          ID of the resource
     * @param resourceWriter                      object in charge of writing the resource
     * @param downloadProgressNotificationHandler handler for receiving notifications concerning this download
     * @param streamingNeed                       the need for streaming this file (0: no need, 1: max need). The higher the need,
     *                                            the greater efforts that the scheduler will do for downloading the first parts
     *                                            of the resource before the last parts. Can hamper total download efficience
     * @param totalHash                           hexadecimal value for the total resource hash (null if not used)
     * @param totalHashAlgorithm                  algorithm for calculating the total hash (null if not used)
     * @return a DownloadManager object for controlling this download, or null if the download could not be created
     * (due to the resource store name given not corresponding to any existing resource store)
     */
    public synchronized DownloadManager downloadResource(
            String resourceStoreName,
            String resourceID,
            ResourceWriter resourceWriter,
            DownloadProgressNotificationHandler downloadProgressNotificationHandler,
            double streamingNeed,
            String totalHash,
            String totalHashAlgorithm) throws NotAliveException {
        return resourceStreamingManager.downloadResource(resourceStoreName, resourceID, resourceWriter, downloadProgressNotificationHandler, streamingNeed, totalHash, totalHashAlgorithm);
    }

    /**
     * Initiates the process for downloading a resource from a specific peer. In this case it is also necessary to
     * specify the target store. However, it is not required that we have this store updated (not even registered) with
     * the resources shared on it
     *
     * @param serverPeerID                        ID of the Peer from which the resource is to be downloaded
     * @param resourceStoreName                   name of the individual store to access
     * @param resourceID                          ID of the resource
     * @param resourceWriter                      object in charge of writing the resource
     * @param downloadProgressNotificationHandler handler for receiving notifications concerning this download
     * @param streamingNeed                       the need for streaming this file (0: no need, 1: max need). The higher the need,
     *                                            the greater efforts that the scheduler will do for downloading the first parts
     *                                            of the resource before the last parts. Can hamper total download efficiency
     * @return a DownloadManager object for controlling this download, or null if the download could not be created
     * (due to the resource store name given not corresponding to any existing resource store)
     */
    public synchronized DownloadManager downloadResource(
            PeerID serverPeerID,
            String resourceStoreName,
            String resourceID,
            ResourceWriter resourceWriter,
            DownloadProgressNotificationHandler downloadProgressNotificationHandler,
            double streamingNeed,
            String totalHash,
            String totalHashAlgorithm) throws NotAliveException {
        return resourceStreamingManager.downloadResource(serverPeerID, resourceStoreName, resourceID, resourceWriter, downloadProgressNotificationHandler, streamingNeed, totalHash, totalHashAlgorithm);
    }

    public synchronized Float getMaxDesiredDownloadSpeed() {
        return resourceStreamingManager.getMaxDesiredDownloadSpeed();
    }

    public synchronized void setMaxDesiredDownloadSpeed(Float totalMaxDesiredSpeed) {
        resourceStreamingManager.setMaxDesiredDownloadSpeed(totalMaxDesiredSpeed);
    }

    public synchronized Float getMaxDesiredUploadSpeed() {
        return resourceStreamingManager.getMaxDesiredUploadSpeed();
    }

    public synchronized void setMaxDesiredUploadSpeed(Float totalMaxDesiredSpeed) {
        resourceStreamingManager.setMaxDesiredUploadSpeed(totalMaxDesiredSpeed);
    }

    public double getDownloadPartSelectionAccuracy() {
        return resourceStreamingManager.getAccuracy();
    }

    public void setDownloadPartSelectionAccuracy(double accuracy) {
        resourceStreamingManager.setAccuracy(accuracy);
    }

    /**
     * Retrieves a shallow copy of the active downloads for a specific resource store
     *
     * @return a shallow copy of the active downloads of a store
     */
    public List<DownloadManager> getVisibleDownloads(String store) {
        return resourceStreamingManager.getDownloadsManager().getDownloads(store);
    }

    public List<DownloadManager> getAllDownloads() {
        return resourceStreamingManager.getDownloadsManager().getAllDownloads();
    }

    /**
     * Set the timer for periodic downloads notifications
     *
     * @param millis time in millis for the timer
     */
    public void setVisibleDownloadsTimer(long millis) {
        resourceStreamingManager.getDownloadsManager().setTimer(millis);
    }

    /**
     * Stops notifying downloads
     */
    public void stopVisibleDownloadsTimer() {
        resourceStreamingManager.getDownloadsManager().stopTimer();
    }

    /**
     * Retrieves a shallow copy of the active uploads for a specific resource store
     *
     * @return a shallow copy of the active uploads of a store
     */
    public List<UploadManager> getVisibleUploads(String store) {
        return resourceStreamingManager.getUploadsManager().getUploads(store);
    }

    /**
     * Set the timer for periodic uploads notifications
     *
     * @param millis time in millis for the timer
     */
    public void setVisibleUploadsTimer(long millis) {
        resourceStreamingManager.getUploadsManager().setTimer(millis);
    }

    /**
     * Stops notifying uploads
     */
    public void stopVisibleUploadsTimer() {
        resourceStreamingManager.getUploadsManager().stopTimer();
    }

    void newPeerConnected(final PeerID peerID, final ChannelConnectionPoint ccp, final ConnectionStatus status) {
        // first notify the resource streaming manager, so it sets up the necessary FSMs for receiving resource data. Then, notify the client
        dataSynchronizer.getDataAccessorContainer().peerConnected(peerID);
        resourceStreamingManager.newPeerConnected(ccp);
        generalEvents.newPeerConnected(peerID, status);
        // send the other peer own nick, to ensure he has our latest value
        sendObjectMessage(peerID, new NewNickMessage(peersPersonalData.getOwnNick()));
    }

    /**
     * This method allows to register a new custom FSM in this PeerClient. Custom FSMs allows client of the peer engine
     * package to establish custom communication protocols with other peers
     *
     * @param peerID        the ID of the peer with which this FSM must communicate
     * @param peerFSMAction the actions to be invoked by the new peer FSM (this is the real behaviour of the FSM)
     * @param serverFSMName the name of the corresponding FSM at the server with which the new FSM will communicate.
     *                      This FSM allows the RequestDispatcher at the other peer to build the correct FSM
     * @param <T>           class used for representing the state in the new FSM
     * @return true if the FSM was correctly set up, false otherwise (due to no available channels, client can try later)
     * @throws UnavailablePeerException the given PeerID does not correspond to a connected peer
     */
    public synchronized <T> UniqueIdentifier registerCustomFSM(PeerID peerID, PeerFSMAction<T> peerFSMAction, String serverFSMName) throws UnavailablePeerException {
        // do not allow to register FSM for peers who are not ready yet
        if (connectedPeers.isConnectedPeer(peerID)) {
            Byte assignedChannel = connectedPeers.requestChannel(peerID);
            if (assignedChannel == null) {
                return null;
            }
            ChannelConnectionPoint ccp = connectedPeers.getPeerChannelConnectionPoint(peerID);
            CustomPeerFSM<T> customPeerFSM = new CustomPeerFSM<>(peerFSMAction, serverFSMName, assignedChannel);
            UniqueIdentifier id = ccp.registerGenericFSM(customPeerFSM, "UnnamedCustomPeerFSM", assignedChannel);
            if (id != null) {
                peerFSMAction.setID(id);
            }
            return id;
        } else {
            throw new UnavailablePeerException();
        }
    }

    /**
     * Same as registerCustomFSM method, but with timeout functionality
     *
     * @param peerID             the ID of the peer with which this FSM must communicate
     * @param peerTimedFSMAction the actions to be invoked by the new peer FSM (this is the real behaviour of the FSM)
     * @param serverFSMName      the name of the corresponding FSM at the server with which the new FSM will communicate.
     *                           This FSM allows the RequestDispatcher at the other peer to build the correct FSM
     * @param timeoutMillis      timeout of the new FSM, in milliseconds
     * @param <T>                class used for representing the state in the new FSM
     * @return true if the FSM was correctly set up, false otherwise (due to no available channels, client can try later)
     * @throws UnavailablePeerException the given PeerID does not correspond to a connected peer
     */
    public synchronized <T> UniqueIdentifier registerTimedCustomFSM(PeerID peerID, PeerTimedFSMAction<T> peerTimedFSMAction, String serverFSMName, long timeoutMillis) throws UnavailablePeerException {
        if (connectedPeers.isConnectedPeer(peerID)) {
            Byte assignedChannel = connectedPeers.requestChannel(peerID);
            if (assignedChannel == null) {
                return null;
            }
            ChannelConnectionPoint ccp = connectedPeers.getPeerChannelConnectionPoint(peerID);
            CustomTimedPeerFSM<T> customTimedPeerFSM = new CustomTimedPeerFSM<>(peerTimedFSMAction, serverFSMName, assignedChannel);
            UniqueIdentifier id = ccp.registerTimedFSM(customTimedPeerFSM, timeoutMillis, "UnnamedCustomTimedPeerFSM", assignedChannel);
            if (id != null) {
                peerTimedFSMAction.setID(id);
            }
            return id;
        } else {
            throw new UnavailablePeerException();
        }
    }

    synchronized void requestServerCustomFSM(RequestFromPeerToPeer requestFromPeerToPeer, String serverFSMName, PeerID peerID, ChannelConnectionPoint ccp, byte outgoingChannel) {
        if (customFSMs.containsKey(serverFSMName)) {
            // requestFromPeerToPeer a channel for the new required custom FSM
            // the channel should be sent in the init method of the custom FSM, not our responsibility
            PeerFSMAction<?> peerFSMAction = customFSMs.get(serverFSMName).buildPeerFSMAction(peerID, connectedPeers.getPeerConnectionStatus(peerID));
            if (peerFSMAction != null) {
                Byte assignedChannel = connectedPeers.requestChannel(peerID);
                if (assignedChannel != null) {
                    // set up custom FSM
                    // non-timed
                    if (customFSMs.get(serverFSMName).getTimeoutMillis() == null) {
                        @SuppressWarnings({"unchecked"})
                        CustomPeerFSM customPeerFSM = new CustomPeerFSM(peerFSMAction, assignedChannel, outgoingChannel);
                        try {
                            UniqueIdentifier id = ccp.registerGenericFSM(customPeerFSM, "UnnamedCustomPeerFSM", assignedChannel);
                            peerFSMAction.setID(id);
                        } catch (Exception e) {
                            reportError("Could not register FSM due to exception", requestFromPeerToPeer, assignedChannel, customFSMs, ccp);
                            ccp.write(outgoingChannel, new ObjectListWrapper(PeerFSMServerResponse.REQUEST_DENIED, null));
                        }
                    }
                    // timed
                    else {
                        @SuppressWarnings({"unchecked"})
                        CustomTimedPeerFSM customTimedPeerFSM = new CustomTimedPeerFSM((PeerTimedFSMAction) peerFSMAction, assignedChannel, outgoingChannel);
                        ccp.registerTimedFSM(customTimedPeerFSM, customFSMs.get(serverFSMName).getTimeoutMillis(), "UnnamedCustomTimedPeerFSM", assignedChannel);
                    }
                } else {
                    ccp.write(outgoingChannel, new ObjectListWrapper(PeerFSMServerResponse.UNAVAILABLE_CHANNEL, null));
                }
            } else {
                ccp.write(outgoingChannel, new ObjectListWrapper(PeerFSMServerResponse.REQUEST_DENIED, null));
            }
        } else {
            ccp.write(outgoingChannel, new ObjectListWrapper(PeerFSMServerResponse.UNRECOGNIZED_FSM, null));
        }
    }

    /**
     * Sends an object message to a connected peer. If the given peer is not among the list of connected peers, the
     * message will be ignored
     *
     * @param peerID  ID of the peer to which the message is to be sent
     * @param message string message to send
     */
    public void sendObjectMessage(PeerID peerID, Serializable message) {
        connectedPeersMessenger.sendObjectRequest(peerID, message);
    }

    /**
     * Sends an object message to all connected peers
     *
     * @param message string message to send to all connected peers
     */
    public void broadcastObjectMessage(Serializable message) {
        connectedPeersMessenger.broadcastObjectRequest(message);
    }

    /**
     * Changes our own nick and broadcasts it to the rest of peers
     *
     * @param newNick the new nick
     */
    public void setNick(String newNick) {
        if (peersPersonalData.setOwnNick(newNick)) {
            // broadcast new nick to connected peers
            broadcastObjectMessage(new NewNickMessage(newNick));
        }
    }

    /**
     * This method allows the PeerRequestDispatcherFSM of a connected peer to report that his peer has sent us a chat
     * message
     *
     * @param peerID  ID of the peer who sent the message
     * @param message string message received
     */
    synchronized void newObjectMessageReceived(final PeerID peerID, final Object message) {
        if (connectedPeers.isConnectedPeer(peerID)) {
            if (message instanceof ValidationMessage) {
                // validation messages are handled here
                if (connectedPeers.getPeerConnectionStatus(peerID) == ConnectionStatus.WAITING_FOR_REMOTE_VALIDATION) {
                    connectedPeers.setPeerConnectionStatus(peerID, ConnectionStatus.CORRECT);
                    generalEvents.peerValidatedUs(peerID);
                }
            } else if (message instanceof NewNickMessage) {
                // new nick from other peer received
                final NewNickMessage newNickMessage = (NewNickMessage) message;
                if (peersPersonalData.setPeersNicks(peerID, newNickMessage.nick)) {
                    generalEvents.newPeerNick(peerID, newNickMessage.nick);
                }
            } else {
                generalEvents.newObjectMessage(peerID, message);
            }
        }
    }

    /**
     * Retrieves the connection status from a connected peer. The connection status indicates if this peer is a friend of us and we are a friend
     * of him (CORRECT), he is not a friend of us but we are a friend of him (UNVALIDATED) or he is our friend but we are waiting for
     * him to make us his friend (WAITING_FOR_REMOTE_VALIDATION)
     *
     * @param peerID peer whose connection status we want to retrieve
     * @return the connection status of the given peer, or null if we are not connected to this peer
     */
    public synchronized ConnectionStatus getPeerConnectionStatus(PeerID peerID) {
        return connectedPeers.getPeerConnectionStatus(peerID);
    }

    public synchronized void subscribeToConnectedPeers(UniqueIdentifier receiverID, NotificationReceiver notificationReceiver) {
        connectedPeers.subscribe(receiverID, notificationReceiver);
    }

    public synchronized void subscribeToConnectedPeers(UniqueIdentifier receiverID, NotificationReceiver notificationReceiver, long millis, double timeFactorAtEachEvent, int limit) {
        connectedPeers.subscribe(receiverID, notificationReceiver, millis, timeFactorAtEachEvent, limit);
    }

    /**
     * This methods erases a connected peer (due to this peer disconnecting from us, or we from him, or due to an error)
     *
     * @param peerID peer that was disconnected
     * @param error  error that provoked the disconnection (null if no error)
     */
    synchronized void peerDisconnected(PeerID peerID, CommError error) {
        dataSynchronizer.getDataAccessorContainer().peerDisconnected(peerID);
        generalEvents.peerDisconnected(peerID, error);
    }
}
