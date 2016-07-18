package jacz.peerengineservice.util.datatransfer;

import org.aanguita.jtcpserver.channel.ChannelConnectionPoint;
import jacz.peerengineservice.NotAliveException;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.client.connection.ConnectedPeersMessenger;
import jacz.peerengineservice.client.connection.peers.PeerConnectionManager;
import jacz.peerengineservice.util.ChannelConstants;
import jacz.peerengineservice.util.ForeignStoreShare;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.peerengineservice.util.datatransfer.master.DownloadState;
import jacz.peerengineservice.util.datatransfer.master.MasterResourceStreamer;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceProvider;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceWriter;
import jacz.peerengineservice.util.datatransfer.slave.SlaveResourceStreamer;
import jacz.peerengineservice.util.datatransfer.slave.UploadManager;
import org.aanguita.jacuzzi.concurrency.ManuallyRemovedElementBag;
import org.aanguita.jacuzzi.concurrency.task_executor.ThreadExecutor;
import org.aanguita.jacuzzi.concurrency.timer.Timer;
import org.aanguita.jacuzzi.concurrency.timer.TimerAction;
import org.aanguita.jacuzzi.io.serialization.MutableOffset;
import org.aanguita.jacuzzi.io.serialization.ObjectListWrapper;
import org.aanguita.jacuzzi.io.serialization.Serializer;
import org.aanguita.jacuzzi.lists.DoubleElementArrayList;
import org.aanguita.jacuzzi.lists.tuple.Duple;
import org.aanguita.jacuzzi.queues.event_processing.MessageHandler;
import org.aanguita.jacuzzi.queues.event_processing.MessageProcessor;
import org.aanguita.jacuzzi.sets.availableelements.AvailableElementsShort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class maintains connections with all connected peers in order to send and receive files at request. Every time
 * a new peer connects, our ResourceStreamingManager connects with the corresponding ResourceStreamingManager. They
 * will be able to ask each other for files, and inform about the transfer capabilities.
 * <p>
 * The peer client can ask the ResourceStreamingManager to download files. The ResourceStreamingManager will look
 * for peers that share that file and organize the download. A file is actually downloaded by a
 * MasterResourceStreamer, which is created by this ResourceStreamingManager class
 * <p>
 * Part selection algorithm:
 * Each download is treated in order to improve efficiency and reduce total download time. For this, there is a
 * specific algorithm that works with several parameters. In general, this algorithm tells a download process which
 * part it should ask to the peers that are sharing the resource. The general norm is to select from each peer parts
 * which other peers are not sharing (to avoid not being able to "use" a peer because another peer gave us the parts
 * that it was sharing). Parts offered by a peer but being shared by other peers are penalized (the more peers that
 * share a part, the more it is penalized). Peers which have very little to offer (below a 15% of the total remaining
 * for download) impose even more penalization.
 * <p>
 * On top of all this is the streaming need of a resource: if the user
 * downloads a movie and wants to watch it on streaming, the algorithm will reward the parts that are first. For a
 * maximum value of streaming need (1.0), the part sharing penalization is almost completely forgotten. For the
 * minimum value (0.0) no care of streaming is taken.
 * <p>
 * There is an additional consideration in the part selection: noise. To avoid situations in which one peer is
 * offering a resource and n peers are taking it from him, and all peers are selecting the same part, a random noise
 * is introduced. This noise will randomize to some extent the part selection process (unless streaming need is
 * very high, or the part share penalization is to high).
 * <p>
 * Finally, the part calculation can be performed more or less accurately. The peer engine is configured on start
 * with an accuracy value for part selection which indicates the amount of parts that are evaluated. For a minimum
 * accuracy (0.0), one hundred parts are considered. For the maximum accuracy (1.0), 20000 parts are evaluated. The
 * user must take into account the memory and processor availability for these calculations, and set the accuracy
 * value accordingly (NOTE: as of now, accuracy is fixed to 0.5, and future plans include the implementation of an
 * automatic benchmark procedure for evaluating the best accuracy value)
 */
public class ResourceStreamingManager {

    private class RemotePeerStakeholder extends GenericPriorityManagerStakeholder {

        private final PeerId peerId;

        public RemotePeerStakeholder(PeerId peerId) {
            this.peerId = peerId;
        }

//        @Override
//        public String toString() {
//            return this.getClass().toString() + "-" + peerId.toString();
//        }

        @Override
        public String toString() {
            return "RemotePeerStakeholder{" +
                    "peerId=" + peerId +
                    '}';
        }

        public float getPriority() {
            return 1f;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RemotePeerStakeholder that = (RemotePeerStakeholder) o;

            return peerId.equals(that.peerId);
        }

        @Override
        public int hashCode() {
            return peerId.hashCode();
        }
    }

    /**
     * Object version of an object message plus a subchannel, so it can be used in the FSM api of the util library
     */
    private static class SubchannelObjectMessage implements Serializable {

        final short subchannel;

        final Object message;

        public SubchannelObjectMessage(short subchannel, Object message) {
            this.subchannel = subchannel;
            this.message = message;
        }
    }

    /**
     * Static methods for encoding and decoding data[] messages with subchannel
     */
    private static class SubchannelDataMessage {

        public static byte[] encode(short subchannel, byte[] data) {
            return Serializer.addArrays(Serializer.serialize(subchannel), data);
        }

        public static SubchannelDataMessage decode(byte[] bytes) {
            MutableOffset mutableOffset = new MutableOffset();
            short subchannel = Serializer.deserializeShortValue(bytes, mutableOffset);
            byte[] data = Serializer.deserializeRest(bytes, mutableOffset);
            return new SubchannelDataMessage(subchannel, data);
        }

        final short subchannel;

        final byte[] data;

        SubchannelDataMessage(short subchannel, byte[] data) {
            this.subchannel = subchannel;
            this.data = data;
        }
    }

    /**
     * This class stores and manages the currently active downloads, indexed by store and resource id. We use this
     * set to find the downloads when there is a report about change of conditions in a store and a resourceID (either
     * peers were added or removed).
     * <p>
     * This set stores only general downloads, with no specific peer. The class is also responsible for periodically
     * notifying the resource streaming manager for the need of updating provides, regardless of any changes in stores
     */
    private class ActiveDownloadSet implements TimerAction {

        /**
         * Set of active downloads, indexed by store name first, and resource id second
         * <p>
         * For each resource, a set of master resource streamers is maintained, since the same resource can be downloaded by several masters
         */
        private final Map<String, Map<String, Map<String, MasterResourceStreamer>>> activeDownloads;

        /**
         * The resource streaming manager that owns the active download set
         */
        private final ResourceStreamingManager resourceStreamingManager;

        /**
         * Timer for periodically updating the providers of all active downloads
         */
        private final Timer generalProviderUpdateTimer;

        private final AtomicBoolean alive;

        private final String threadExecutorClientId;

        private ActiveDownloadSet(ResourceStreamingManager resourceStreamingManager) {
            activeDownloads = new HashMap<>();
            this.resourceStreamingManager = resourceStreamingManager;
            generalProviderUpdateTimer = new Timer(ResourceStreamingManager.MILLIS_FOR_GENERAL_PROVIDER_UPDATE, this, "ActiveDownloadSet");
            alive = new AtomicBoolean(true);
            threadExecutorClientId = ThreadExecutor.registerClient(this.getClass().getName());
        }

        /**
         * Adds a new download to the set of active downloads
         *
         * @param masterResourceStreamer the master resource streamer of the new download
         */
        private synchronized void addDownload(MasterResourceStreamer masterResourceStreamer) {
            String storeName = masterResourceStreamer.getStoreName();
            String resourceID = masterResourceStreamer.getResourceId();
            if (!activeDownloads.containsKey(storeName)) {
                activeDownloads.put(storeName, new HashMap<>());
            }
            Map<String, Map<String, MasterResourceStreamer>> resourceIDMap = activeDownloads.get(storeName);
            if (!resourceIDMap.containsKey(resourceID)) {
                resourceIDMap.put(resourceID, new HashMap<>());
            }
            resourceIDMap.get(resourceID).put(masterResourceStreamer.getId(), masterResourceStreamer);
        }

        /**
         * Returns the list of master resource streamers associated to one store and one resource id
         *
         * @param storeName  store
         * @param resourceID resource
         * @return collections of master resource streamers that are downloading the specified resource (an empty collection if the resource is
         * not being downloaded)
         */
        private synchronized Collection<MasterResourceStreamer> getDownload(String storeName, String resourceID) {
            try {
                return activeDownloads.get(storeName).get(resourceID).values();
            } catch (NullPointerException e) {
                return new ArrayList<>();
            }
        }

        /**
         * Removes one master resource streamer from the active downloads
         *
         * @param masterResourceStreamer the master to remove
         */
        private synchronized void removeDownload(MasterResourceStreamer masterResourceStreamer) {
            String storeName = masterResourceStreamer.getStoreName();
            String resourceID = masterResourceStreamer.getResourceId();
            try {
                activeDownloads.get(storeName).get(resourceID).remove(masterResourceStreamer.getId());
                if (activeDownloads.get(storeName).get(resourceID).isEmpty()) {
                    activeDownloads.get(storeName).remove(resourceID);
                }
                if (activeDownloads.get(storeName).isEmpty()) {
                    activeDownloads.remove(storeName);
                }
            } catch (NullPointerException e) {
                // ignore
            }
        }

        @Override
        /**
         * This method performs an update on the available providers for all active downloads
         */
        public synchronized Long wakeUp(Timer timer) {
            // copy the active downloads map so it can be modified while performing the provider updates
            final Map<String, Map<String, Map<String, MasterResourceStreamer>>> activeDownloadsCopy = new HashMap<>();
            for (String storeName : activeDownloads.keySet()) {
                activeDownloadsCopy.put(storeName, new HashMap<>(activeDownloads.get(storeName)));
            }
            ThreadExecutor.submit(() -> {
                for (String storeName : activeDownloadsCopy.keySet()) {
                    Map<String, Map<String, MasterResourceStreamer>> activeDownloadsForOneStore = activeDownloadsCopy.get(storeName);
                    for (String resourceID : activeDownloadsForOneStore.keySet()) {
                        resourceStreamingManager.reportProvidersForOneActiveDownload(storeName, resourceID);
                    }
                }
            });
            // the timer never dies
            return null;
        }

        public synchronized void stop() {
            if (alive.get()) {
                alive.set(false);
                generalProviderUpdateTimer.stop();
                ThreadExecutor.shutdownClient(threadExecutorClientId);
            }
        }
    }


    /**
     * The incoming subchannel assignments for all our data streaming stuff. 16-bit subchannels are defined for routing the data transmission
     * messages. The class employs the MessageProcessor schema in jacz.util to handle the incoming messages.
     * <p>
     * Each resource master streamer will generally use several channels, one for each slave. This allows him to differentiate incoming packages.
     * This allows a total of 2^16 active transfers, but this should always be enough
     * <p>
     * This class handles the subchannel assignment, providing new subchannels upon master requests, and freeing no
     * longer used subchannels. At initialization time, we can assign a list of already occupied subchannels so they
     * are not assigned to anyone else
     * <p>
     * This class also handles concurrent message processing for the assigned channels, delivering each incoming message to the appropriate
     * owner
     */
    private class SubchannelManager {

        /**
         * Inner implementation of the MessageHandler interface, to deal with incoming messages. One of this is created for each subchannel
         */
        private class MessageHandlerImpl implements MessageHandler {

            private final short subChannel;

            private final SubchannelOwner subchannelOwner;

            private MessageHandlerImpl(short subChannel, SubchannelOwner subchannelOwner) {
                this.subChannel = subChannel;
                this.subchannelOwner = subchannelOwner;
            }

            @Override
            public void handleMessage(Object message) {
                if (message instanceof ByteArrayObject) {
                    subchannelOwner.processMessage(subChannel, ((ByteArrayObject) message).data);
                } else {
                    subchannelOwner.processMessage(subChannel, message);
                }
            }

            @Override
            public void finalizeHandler() {
                // ignore
            }
        }

        /**
         * An object representation of a byte array, so it can be used in the message processor api
         */
        private final class ByteArrayObject {

            private final byte[] data;

            private ByteArrayObject(byte[] data) {
                this.data = data;
            }
        }

        /**
         * Data stored for each occupied subchannel
         */
        private class SubchannelData {

            private final SubchannelOwner subchannelOwner;

            private final MessageProcessor messageProcessor;

            private SubchannelData(SubchannelOwner subchannelOwner, MessageProcessor messageProcessor) {
                this.subchannelOwner = subchannelOwner;
                this.messageProcessor = messageProcessor;
            }
        }

        /**
         * Table with assigned subchannels and their corresponding owners and message processors
         * <p>
         * Each subchannel will define a message processor schema with only the message handler thread. We will take care of feeding
         * the processor with messages. The default queue capacity is used
         */
        private final Map<Short, SubchannelData> assignedSubchannels;

        /**
         * List of subchannels owned by each subchannel owner
         */
        private final Map<SubchannelOwner, Set<Short>> subchannelsForEachOwner;

        /**
         * Free subchannels, for assigning new subchannels upon request
         */
        private final AvailableElementsShort availableSubchannels;

        /**
         * Indicates if we are still alive. We can only assign subchannels if we are alive. Otherwise all requests are rejected
         */
        private boolean alive;

        /**
         * Constructor
         *
         * @param occupiedSubchannels subchannels that must never be assigned. They are tagged as occupied at construction time
         */
        private SubchannelManager(DoubleElementArrayList<Short, SubchannelOwner> occupiedSubchannels) {
            assignedSubchannels = new HashMap<>();
            subchannelsForEachOwner = new HashMap<>();
            availableSubchannels = new AvailableElementsShort(occupiedSubchannels.cloneFirstList().toArray(new Short[occupiedSubchannels.size()]));
            for (int i = 0; i < occupiedSubchannels.size(); i++) {
                putOwnerInSubchannel(occupiedSubchannels.getFirst(i), occupiedSubchannels.getSecond(i));
            }
            alive = true;
        }

        /**
         * Request for a free subchannel
         *
         * @param owner the requesting owner
         * @return the subchannel value if there is a free subchannel, or null if the request could not be fulfilled (no free subchannels)
         */
        public synchronized Short requestSubchannel(SubchannelOwner owner) {
            if (alive) {
                Short subchannel = availableSubchannels.requestElement();
                if (subchannel != null) {
                    // successful request -> assign this subchannel to the given master
                    putOwnerInSubchannel(subchannel, owner);
                }
                return subchannel;
            } else {
                return null;
            }
        }

        /**
         * Assigns an owner to a subchannel
         *
         * @param subchannel subchannel to be assigned
         * @param owner      owner of the subchannel
         */
        private synchronized void putOwnerInSubchannel(Short subchannel, SubchannelOwner owner) {
            assignedSubchannels.put(subchannel, new SubchannelData(owner, new MessageProcessor("MessageProcessor - " + subchannel, new MessageHandlerImpl(subchannel, owner))));
            assignedSubchannels.get(subchannel).messageProcessor.start();
            if (!subchannelsForEachOwner.containsKey(owner)) {
                subchannelsForEachOwner.put(owner, new HashSet<>(1));
            }
            subchannelsForEachOwner.get(owner).add(subchannel);
        }

        public synchronized void processMessage(short subchannel, Object message) {
            if (assignedSubchannels.containsKey(subchannel)) {
                try {
                    assignedSubchannels.get(subchannel).messageProcessor.addMessage(message);
                } catch (InterruptedException e) {
                    // ignore, cannot happen
                }
            }
        }

        public synchronized void processMessage(short subchannel, byte[] data) {
            if (assignedSubchannels.containsKey(subchannel)) {
                try {
                    assignedSubchannels.get(subchannel).messageProcessor.addMessage(new ByteArrayObject(data));
                } catch (InterruptedException e) {
                    // ignore, cannot happen
                }
            }
        }

        /**
         * An occupied subchannel is freed
         *
         * @param subchannel the subchannel to free
         */
        public synchronized void freeSubchannel(short subchannel) {
            if (assignedSubchannels.containsKey(subchannel)) {
                SubchannelData subchannelData = assignedSubchannels.get(subchannel);
                subchannelData.messageProcessor.stop();
                subchannelsForEachOwner.get(subchannelData.subchannelOwner).remove(subchannel);
                if (subchannelsForEachOwner.get(subchannelData.subchannelOwner).isEmpty()) {
                    subchannelsForEachOwner.remove(subchannelData.subchannelOwner);
                }
                assignedSubchannels.remove(subchannel);
                availableSubchannels.freeElement(subchannel);
            }
        }

        public synchronized void freeAllSubchannelsFromOwner(SubchannelOwner subchannelOwner) {
            if (subchannelsForEachOwner.containsKey(subchannelOwner)) {
                Collection<Short> subchannelsToFree = new HashSet<>(subchannelsForEachOwner.get(subchannelOwner));
                for (Short subchannel : subchannelsToFree) {
                    freeSubchannel(subchannel);
                }
            }
        }

        public synchronized void stop() {
            // free all occupied subchannels, and not allow any further requests
            Set<SubchannelOwner> owners = new HashSet<>(subchannelsForEachOwner.keySet());
            for (SubchannelOwner owner : owners) {
                freeAllSubchannels(owner);
            }
            alive = false;
        }
    }

    /**
     * This interface represents an entity which can own a subchannel. Currently, three entities can hold a subchannel:
     * a master resource streamer, a slave resource streamer and a slave request manager. This interface includes the
     * message processing methods that those entities must implement
     */
    public interface SubchannelOwner {

        void processMessage(short subchannel, Object message);

        void processMessage(short subchannel, byte[] data);
    }

    /**
     * This class implements a subchannel owner that will own the subchannel for slave requests. It will handle all
     * slave requests. There will be only one instance of this class, for handling requests directed to a specific
     * subchannel for slave requests
     */
    private class SlaveRequestsManager implements SubchannelOwner {

        private ResourceStreamingManager resourceStreamingManager;

        private SlaveRequestsManager(ResourceStreamingManager resourceStreamingManager) {
            this.resourceStreamingManager = resourceStreamingManager;
        }

        @Override
        public void processMessage(short subchannel, Object message) {
            // new request for a slave
            if (message instanceof ResourceRequest) {
                ResourceRequest resourceRequest = (ResourceRequest) message;
                resourceStreamingManager.requestNewResource(resourceRequest);
            }
        }

        @Override
        public void processMessage(short subchannel, byte[] data) {
            // ignore these messages, as all requests are object messages
        }
    }

    private final static Logger logger = LoggerFactory.getLogger(PeerConnectionManager.class);

    /**
     * Subchannel for both requesting slaves to other peers and for receiving requests for slaves from other peers
     */
    public static final short SLAVE_GRANT_SUBCHANNEL = 0;

    private static final long MILLIS_FOR_GENERAL_PROVIDER_UPDATE = 15000;

    private final PeerId ownPeerId;

    private final ResourceTransferEventsBridge resourceTransferEventsBridge;

    private final ConnectedPeersMessenger connectedPeersMessenger;

    /**
     * The incoming subchannel assignments, for each active master resource streamer.
     * <p>
     * Non-persistent
     */
    private final SubchannelManager subchannelManager;

    /**
     * The manager of all registered resource stores
     * <p>
     * Non-persistent
     */
    private final LocalShareManager localShareManager;

    /**
     * The manager for resources shared to us by other peers
     * <p>
     * Non-persistent
     */
    private final ForeignShareManager foreignShareManager;

    /**
     * Active downloads from foreign stores (with no specific peer)
     */
    private final ActiveDownloadSet activeDownloadSet;

    /**
     * Manager for the currently existing downloads (downloads can be made visible, so we get periodic notifications on their progress).
     */
    private final DownloadsManager downloadsManager;

    /**
     * Manager of the currently existing uploads
     */
    private final UploadsManager uploadsManager;

    /**
     * Interface for retrieving accuracy
     */
    private final TransfersConfig transfersConfig;

    /**
     * Manager for controlling upload speeds of resource transfers
     */
    private final GenericPriorityManager uploadPriorityManager;

    /**
     * Manager for controlling download speeds of resource transfers
     */
    private final GenericPriorityManager downloadPriorityManager;

    private final TransferStatistics transferStatistics;

    private final Lock writeDataLock;

    /**
     * Whether this resource streaming manager is alive or not. If not alive, no new requests will be accepted
     * (writes, stores, downloads).
     * <p>
     * Once it becomes inactive, it cannot be activated anymore
     */
    private final AtomicBoolean alive;

    private final String threadExecutorClientId;

    public ResourceStreamingManager(
            PeerId ownPeerId,
            ResourceTransferEvents resourceTransferEvents,
            ConnectedPeersMessenger connectedPeersMessenger,
            String transferStatisticsPath,
            TransfersConfig transfersConfig) throws IOException {
        this.ownPeerId = ownPeerId;
        this.resourceTransferEventsBridge = new ResourceTransferEventsBridge(resourceTransferEvents);
        this.connectedPeersMessenger = connectedPeersMessenger;
        DoubleElementArrayList<Short, SubchannelOwner> occupiedSubchannels = new DoubleElementArrayList<>(1);
        occupiedSubchannels.add(SLAVE_GRANT_SUBCHANNEL, new SlaveRequestsManager(this));
        subchannelManager = new SubchannelManager(occupiedSubchannels);
        localShareManager = new LocalShareManager();
        foreignShareManager = new ForeignShareManager(this);
        activeDownloadSet = new ActiveDownloadSet(this);
        downloadsManager = new DownloadsManager(this.resourceTransferEventsBridge);
        uploadsManager = new UploadsManager(this.resourceTransferEventsBridge);
        this.transfersConfig = transfersConfig;
        uploadPriorityManager = new GenericPriorityManager(this.transfersConfig::getMaxUploadSpeed, true);
        downloadPriorityManager = new GenericPriorityManager(this.transfersConfig::getMaxDownloadSpeed, true);
        this.transferStatistics = new TransferStatistics(transferStatisticsPath);
        writeDataLock = new ReentrantLock(true);
        alive = new AtomicBoolean(true);
        ManuallyRemovedElementBag.getInstance(PeerClient.MANUAL_REMOVE_BAG).createElement(this.getClass().getName());
        threadExecutorClientId = ThreadExecutor.registerClient(this.getClass().getName());
    }


    /**
     * Requests an incoming subchannel for a specific master resource streamer
     *
     * @param owner the owner requesting the subchannel
     * @return the assigned subchannel, or null of no subchannel was assigned
     */
    public synchronized Short requestIncomingSubchannel(SubchannelOwner owner) {
        if (alive.get()) {
            return subchannelManager.requestSubchannel(owner);
        } else {
            return null;
        }
    }

    public synchronized void freeSubchannel(short subchannel) {
        subchannelManager.freeSubchannel(subchannel);
    }

    public synchronized void freeAllSubchannels(SubchannelOwner owner) {
        subchannelManager.freeAllSubchannelsFromOwner(owner);
    }

    public void newPeerConnected(ChannelConnectionPoint ccp) {
        PeerDataReceiver peerDataReceiver = new PeerDataReceiver(this);
        ccp.registerGenericFSM(peerDataReceiver, "PeerDataReceiver", ChannelConstants.RESOURCE_STREAMING_MANAGER_CHANNEL);
    }

    public long write(PeerId destinationPeer, short subchannel, Object message) {
        return connectedPeersMessenger.sendObjectMessage(destinationPeer, ChannelConstants.RESOURCE_STREAMING_MANAGER_CHANNEL, new SubchannelObjectMessage(subchannel, message), true);
    }

    public long write(PeerId destinationPeer, short subchannel, byte[] message, boolean isData) {
        return write(destinationPeer, subchannel, message, isData, true);
    }

    public long write(PeerId destinationPeer, short subchannel, byte[] message, boolean isData, boolean flush) {
        if (isData) {
            transferStatistics.addUploadedBytes(message.length);
        }
        return connectedPeersMessenger.sendDataMessage(destinationPeer, ChannelConstants.RESOURCE_STREAMING_MANAGER_CHANNEL, SubchannelDataMessage.encode(subchannel, message), flush);
    }

    public long flush(PeerId destinationPeer) {
        return connectedPeersMessenger.flush(destinationPeer);
    }

    synchronized void processMessage(Object o) {
        SubchannelObjectMessage message = (SubchannelObjectMessage) o;
        subchannelManager.processMessage(message.subchannel, message.message);
    }

    synchronized void processMessage(byte[] bytes) {
        SubchannelDataMessage subchannelDataMessage = SubchannelDataMessage.decode(bytes);
        subchannelManager.processMessage(subchannelDataMessage.subchannel, subchannelDataMessage.data);
    }

    public TransferStatistics getTransferStatistics() {
        return transferStatistics;
    }

    /**
     * Retrieves the manager of visible downloads
     *
     * @return the manager of visible downloads
     */
    public DownloadsManager getDownloadsManager() {
        return downloadsManager;
    }

    /**
     * Retrieves the manager of visible uploads
     *
     * @return the manager of visible uploads
     */
    public UploadsManager getUploadsManager() {
        return uploadsManager;
    }

    /**
     * Adds a store containing resources that we share to the rest of peers. It is used for handling download requests
     * incoming from other peers
     *
     * @param name  name of the resource store
     * @param store implementation of the resource store, for requesting resources to our client
     */
    public synchronized void addLocalResourceStore(String name, ResourceStore store) {
        resourceTransferEventsBridge.addLocalResourceStore(name);
        localShareManager.addStore(name, store);
    }

    /**
     * Sets the local general resource store
     *
     * @param generalResourceStore general resource store
     */
    public synchronized void setLocalGeneralResourceStore(GeneralResourceStore generalResourceStore) {
        resourceTransferEventsBridge.setLocalGeneralResourceStore();
        localShareManager.setGeneralStore(generalResourceStore);
    }

    /**
     * Adds a store of resources shared to us by other peers. It it used to handle downloads from other peers
     *
     * @param name              name of the resource store
     * @param foreignStoreShare peers share for letting us know the share of resources of each peer
     */
    public synchronized void addForeignResourceStore(String name, ForeignStoreShare foreignStoreShare) {
        resourceTransferEventsBridge.addForeignResourceStore(name);
        foreignShareManager.addStore(name, foreignStoreShare);
    }

    /**
     * Removes an already defined local store
     *
     * @param name name of the store to remove
     */
    public synchronized void removeLocalResourceStore(String name) {
        resourceTransferEventsBridge.removeLocalResourceStore(name);
        localShareManager.removeStore(name);
    }

    /**
     * Removes the local general resource store, so only the registered stores will be used
     */
    public synchronized void removeLocalGeneralResourceStore() {
        resourceTransferEventsBridge.removeLocalGeneralResourceStore();
        localShareManager.setGeneralStore(null);
    }

    /**
     * Removes an already defined foreign store
     *
     * @param name name of the store to remove
     */
    public synchronized void removeForeignResourceStore(String name) {
        resourceTransferEventsBridge.removeForeignResourceStore(name);
        foreignShareManager.removeStore(name);
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
     *                                            of the resource before the last parts. Can hamper total download efficiency
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
        if (alive.get()) {
            // the download is created even if there is no matching global resource store
            Duple<MasterResourceStreamer, DownloadManager> masterAndDM = createMasterResourceStreamer(
                    null,
                    resourceStoreName,
                    resourceID,
                    resourceWriter,
                    downloadProgressNotificationHandler,
                    streamingNeed,
                    totalHash,
                    totalHashAlgorithm, () -> resourceTransferEventsBridge.globalDownloadInitiated(resourceStoreName, resourceID, streamingNeed, totalHash, totalHashAlgorithm));
            return masterAndDM.element2;
        } else {
            throw new NotAliveException();
        }
    }

    /**
     * Initiates the process for downloading a resource from a specific peer. In this case it is also necessary to
     * specify the target store. However, it is not required that we have this store updated (not even registered) with
     * the resources shared on it
     *
     * @param serverPeerId                        ID of the Peer from which the resource is to be downloaded
     * @param resourceStoreName                   name of the individual store to access
     * @param resourceID                          ID of the resource
     * @param resourceWriter                      object in charge of writing the resource
     * @param downloadProgressNotificationHandler handler for receiving notifications concerning this download
     * @param streamingNeed                       the need for streaming this file (0: no need, 1: max need). The higher the need,
     *                                            the greater efforts that the scheduler will do for downloading the first parts
     *                                            of the resource before the last parts. Can hamper total download efficiency
     * @param totalHash                           hexadecimal value for the total resource hash (null if not used)
     * @param totalHashAlgorithm                  algorithm for calculating the total hash (null if not used)
     * @return a DownloadManager object for controlling this download, or null if the download could not be created
     * (due to the resource store name given not corresponding to any existing resource store)
     */
    public synchronized DownloadManager downloadResource(
            PeerId serverPeerId,
            String resourceStoreName,
            String resourceID,
            ResourceWriter resourceWriter,
            DownloadProgressNotificationHandler downloadProgressNotificationHandler,
            double streamingNeed,
            String totalHash,
            String totalHashAlgorithm) throws NotAliveException {
        if (alive.get()) {
            Duple<MasterResourceStreamer, DownloadManager> masterAndDM = createMasterResourceStreamer(
                    serverPeerId,
                    resourceStoreName,
                    resourceID,
                    resourceWriter,
                    downloadProgressNotificationHandler,
                    streamingNeed,
                    totalHash,
                    totalHashAlgorithm, () -> resourceTransferEventsBridge.peerDownloadInitiated(serverPeerId, resourceStoreName, resourceID, streamingNeed, totalHash, totalHashAlgorithm));
            reportResourceProviderForPeerSpecificDownload(serverPeerId, masterAndDM.element1);
            return masterAndDM.element2;
        } else {
            throw new NotAliveException();
        }
    }

    private Duple<MasterResourceStreamer, DownloadManager> createMasterResourceStreamer(
            PeerId serverPeerId,
            String resourceStoreName,
            String resourceID,
            ResourceWriter resourceWriter,
            DownloadProgressNotificationHandler downloadProgressNotificationHandler,
            double streamingNeed,
            String totalHash,
            String totalHashAlgorithm,
            Runnable reportAction) {
        MasterResourceStreamer masterResourceStreamer =
                new MasterResourceStreamer(
                        this,
                        transfersConfig,
                        serverPeerId,
                        resourceStoreName,
                        resourceID,
                        resourceWriter,
                        downloadProgressNotificationHandler,
                        streamingNeed,
                        totalHash,
                        totalHashAlgorithm);
        activateMasterResourceStreamer(masterResourceStreamer, reportAction);
        return new Duple<>(masterResourceStreamer, masterResourceStreamer.getDownloadManager());
    }

    public void activateMasterResourceStreamer(MasterResourceStreamer masterResourceStreamer, Runnable reportAction) {
        if (masterResourceStreamer.getState() != DownloadState.STOPPED) {
            // the download is active
            activeDownloadSet.addDownload(masterResourceStreamer);
            downloadsManager.addDownload(masterResourceStreamer.getStoreName(), masterResourceStreamer.getId(), masterResourceStreamer.getDownloadManager());
            reportAction.run();
        }
    }

//    public synchronized Float getMaxDesiredDownloadSpeed() {
//        return downloadPriorityManager.getTotalMaxDesiredSpeed();
//    }
//
//    public synchronized void setMaxDesiredDownloadSpeed(Float totalMaxDesiredSpeed) {
//        resourceTransferEventsBridge.setMaxDesiredDownloadSpeed(totalMaxDesiredSpeed);
//        downloadPriorityManager.setTotalMaxDesiredSpeed(totalMaxDesiredSpeed);
//    }
//
//    public synchronized Float getMaxDesiredUploadSpeed() {
//        return uploadPriorityManager.getTotalMaxDesiredSpeed();
//    }
//
//    public synchronized void setMaxDesiredUploadSpeed(Float totalMaxDesiredSpeed) {
//        resourceTransferEventsBridge.setMaxDesiredUploadSpeed(totalMaxDesiredSpeed);
//        uploadPriorityManager.setTotalMaxDesiredSpeed(totalMaxDesiredSpeed);
//    }
//
//    public double getAccuracy() {
//        synchronized (accuracy) {
//            return accuracy.getValue();
//        }
//    }
//
//    public void setAccuracy(double accuracy) {
//        resourceTransferEventsBridge.setAccuracy(accuracy);
//        synchronized (this.accuracy) {
//            this.accuracy.setDegree(accuracy);
//        }
//    }

    public void reportDownloadedSize(PeerId peerId, long bytes) {
        transferStatistics.addDownloadedBytes(bytes);
    }

    /**
     * This method tells the resource streaming manager to invoke the stop() method on all active downloads. It does
     * not close any other resources, and subsequent downloads can be invoked
     * <p>
     * The method blocks until all resources are properly stopped. Downloads and uploads are stopped.
     */
    public void stop() {
        logger.info("STOP ISSUED");
        if (alive.getAndSet(false)) {
            logger.info("STOPPING...");
            resourceTransferEventsBridge.stop();
            transferStatistics.stop();
            Collection<Future> futureCollection;
            synchronized (this) {
                // subchannel assignments
                downloadsManager.stop();
                futureCollection = new HashSet<>();
                for (final DownloadManager downloadManager : downloadsManager.getAllDownloads()) {
                    // this call is parallelized to avoid triple interlock between the ResourceStreamingManager, the MasterResourceStreamer and the
                    // DownloadManager
                    Future future = ThreadExecutor.submit(downloadManager::stopDueToFinishedSession);
                    futureCollection.add(future);
                }
            }
            for (Future future : futureCollection) {
                try {
                    future.get();
                } catch (Exception e) {
                    // ignore exceptions
                }
            }
            synchronized (this) {
                futureCollection.clear();
                uploadPriorityManager.stop();
                downloadPriorityManager.stop();
                foreignShareManager.stop();
                activeDownloadSet.stop();
                subchannelManager.stop();
                uploadsManager.stop();
                for (final UploadManager uploadManager : uploadsManager.getAllUploads()) {
                    Future future = ThreadExecutor.submit(uploadManager::stop);
                    futureCollection.add(future);
                }
            }
            for (Future future : futureCollection) {
                try {
                    future.get();
                } catch (Exception e) {
                    // ignore exceptions
                }
            }
            ManuallyRemovedElementBag.getInstance(PeerClient.MANUAL_REMOVE_BAG).destroyElement(this.getClass().getName());
            ThreadExecutor.shutdownClient(threadExecutorClientId);
            logger.info("STOPPED");
        }
    }

    public synchronized void removeDownload(MasterResourceStreamer masterResourceStreamer) {
        activeDownloadSet.removeDownload(masterResourceStreamer);
        downloadsManager.removeDownload(masterResourceStreamer.getStoreName(), masterResourceStreamer.getId());
    }


    synchronized void reportProvidersShareChanges(String resourceStoreName, List<String> affectedResources) {
        for (String resource : affectedResources) {
            reportProvidersForOneActiveDownload(resourceStoreName, resource);
        }
    }

    private void reportProvidersForOneActiveDownload(String resourceStoreName, String resourceID) {
        ForeignStoreShare foreignStoreShare = foreignShareManager.getResourceProviderShare(resourceStoreName);
        Set<ResourceProvider> resourceProviders = null;
        if (foreignStoreShare != null) {
            Set<PeerId> peersSharing = foreignStoreShare.getForeignPeerShares(resourceID);
            resourceProviders = new HashSet<>(peersSharing.size());
            for (PeerId peerId : peersSharing) {
                ResourceProvider peerResourceProvider = generateResourceProvider(peerId);
                resourceProviders.add(peerResourceProvider);
            }
            // report the new providers to the client
        }
        if (resourceProviders != null) {
            for (MasterResourceStreamer masterResourceStreamer : activeDownloadSet.getDownload(resourceStoreName, resourceID)) {
                if (masterResourceStreamer.getSpecificPeerDownload() == null) {
                    // group download -> give the assessed provider set
                    masterResourceStreamer.reportAvailableResourceProviders(resourceProviders);
                }
            }
        }
        // specific peer download do not need a foreign share registered, so check them apart of all previous calculations
        for (MasterResourceStreamer masterResourceStreamer : activeDownloadSet.getDownload(resourceStoreName, resourceID)) {
            if (masterResourceStreamer.getSpecificPeerDownload() != null) {
                // specific download -> generate
                reportResourceProviderForPeerSpecificDownload(masterResourceStreamer.getSpecificPeerDownload(), masterResourceStreamer);
            }
        }
    }

    private void reportResourceProviderForPeerSpecificDownload(PeerId serverPeerId, MasterResourceStreamer masterResourceStreamer) {
        ResourceProvider resourceProvider = generateResourceProvider(serverPeerId);
        List<ResourceProvider> providerList = new ArrayList<>(1);
        providerList.add(resourceProvider);
        masterResourceStreamer.reportAvailableResourceProviders(providerList);
    }

    private ResourceProvider generateResourceProvider(PeerId peerId) {
        return new ResourceProvider(ownPeerId, peerId, this);
    }

    private void requestNewResource(ResourceRequest request) {
        ResourceStoreResponse response = getResourceRequestResponse(request);
        processResourceRequestResponse(request, response);
    }

    private ResourceStoreResponse getResourceRequestResponse(ResourceRequest request) {
        ResourceStore resourceStore = localShareManager.getStore(request.getStoreName());
        if (resourceStore != null) {
            return resourceStore.requestResource(request.getRequestingPeer(), request.getResourceID());
        } else {
            GeneralResourceStore generalResourceStore = localShareManager.getGeneralResourceStore();
            if (generalResourceStore != null) {
                // try with the general resource store
                return generalResourceStore.requestResource(request.getStoreName(), request.getRequestingPeer(), request.getResourceID());
            }
        }
        return null;
    }

    private void processResourceRequestResponse(final ResourceRequest request, ResourceStoreResponse response) {
        if (response != null && response.getResponse() == ResourceStoreResponse.Response.REQUEST_APPROVED) {
            SlaveResourceStreamer slave = new SlaveResourceStreamer(this, request);
            UploadManager uploadManager = new UploadManager(slave);
            Short incomingSubchannel = subchannelManager.requestSubchannel(slave);
            if (incomingSubchannel != null) {
                resourceTransferEventsBridge.approveResourceRequest(request, response);
                slave.initialize(response.getResourceReader(), request.getRequestingPeer(), incomingSubchannel, request.getSubchannel());
                uploadPriorityManager.addRegulatedResource(new RemotePeerStakeholder(request.getRequestingPeer()), slave);
                uploadsManager.addUpload(request.getStoreName(), slave.getId(), uploadManager);
            } else {
                // no subchannels available for this slave (the slave will eventually timeout and die,no need to notify him)
                resourceTransferEventsBridge.denyUnavailableSubchannelResourceRequest(request, response);
                denyRequest(request);
            }
        } else {
            // request was not approved by the user, or incorrect store
            resourceTransferEventsBridge.deniedResourceRequest(request, response);
            denyRequest(request);
        }

    }

    private void denyRequest(ResourceRequest resourceRequest) {
        ObjectListWrapper denial = new ObjectListWrapper(false);
        connectedPeersMessenger.sendObjectMessage(resourceRequest.getRequestingPeer(), ChannelConstants.RESOURCE_STREAMING_MANAGER_CHANNEL, new SubchannelObjectMessage(resourceRequest.getSubchannel(), denial), true);
    }

    /**
     * This method allows SlaveResourceStreamers owned by this ResourceStreaming manager that they just died
     *
     * @param slave the slave that just died
     */
    public void reportDeadSlaveResourceStreamer(SlaveResourceStreamer slave) {
        uploadPriorityManager.removeRegulatedResource(new RemotePeerStakeholder(slave.getResourceRequest().getRequestingPeer()), slave);
        uploadsManager.removeUpload(slave.getResourceRequest().getStoreName(), slave.getId());
        freeSubchannel(slave.getIncomingChannel());
    }

    public GenericPriorityManager getDownloadPriorityManager() {
        return downloadPriorityManager;
    }

    public void acquireWriteDataLock() {
        writeDataLock.lock();
    }

    public void releaseWriteDataLock() {
        writeDataLock.unlock();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            stop();
        } finally {
            super.finalize();
        }
    }

//    private void sout(String method, int count) {
//        String time = DateTime.getFormattedCurrentDateTime(DateTime.DateTimeElement.hh, ":", DateTime.DateTimeElement.mm, ":", DateTime.DateTimeElement.ss);
//        String state = (count >= 0) ? Integer.toString(count) : "END";
//        //System.out.println(time + " - " + method + ", " + state);
//    }

}