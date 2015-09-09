package jacz.peerengine.util.datatransfer.slave;

import jacz.peerengine.util.datatransfer.*;
import jacz.peerengine.util.datatransfer.master.MasterMessage;
import jacz.peerengine.util.datatransfer.master.ResourcePart;
import jacz.util.concurrency.timer.SimpleTimerAction;
import jacz.util.concurrency.timer.Timer;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.identifier.UniqueIdentifierFactory;
import jacz.util.io.object_serialization.ObjectListWrapper;
import jacz.util.numeric.LongRange;
import jacz.util.numeric.RangeQueue;
import jacz.util.queues.event_processing.MessageHandler;
import jacz.util.queues.event_processing.MessageProcessor;
import jacz.peerengine.PeerID;
import jacz.peerengine.util.datatransfer.resource_accession.ResourceReader;

import java.io.IOException;
import java.util.List;

/**
 * This class handles a slave that serves a resource to a master
 *
 * todo notify the resource that we finished (either completer, or error or timeout)
 */
public class SlaveResourceStreamer implements jacz.peerengine.util.datatransfer.ResourceStreamingManager.SubchannelOwner, SimpleTimerAction, jacz.peerengine.util.datatransfer.GenericPriorityManager.RegulatedResource {

    static class RemovedRange {

        final LongRange range;

        final boolean isHash;

        RemovedRange(LongRange range, boolean isHash) {
            this.range = range;
            this.isHash = isHash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RemovedRange that = (RemovedRange) o;

            if (isHash != that.isHash) return false;
            if (!range.equals(that.range)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = range.hashCode();
            result = 31 * result + (isHash ? 1 : 0);
            return result;
        }
    }

    static class ResourceSegmentQueue {

        private final RangeQueue<LongRange, Long> queue;

        private final Long preferredIntermediateHashesSize;

        private long currentHashSize;

        private long amountOfCurrentHashSent;

        ResourceSegmentQueue(Long preferredIntermediateHashesSize) {
            queue = new RangeQueue<>();
            this.preferredIntermediateHashesSize = preferredIntermediateHashesSize;
            amountOfCurrentHashSent = 0;
            currentHashSize = 0;
        }

        RemovedRange remove(long preferredBlockSize) {
            if (!queue.isEmpty() && queue.getRanges().get(0) == stopMessage) {
                // issue stop order
                return new RemovedRange(queue.remove(stopMessage.size()), false);
            }
            if (preferredIntermediateHashesSize != null && currentHashSize == 0) {
                LongRange rangeForHash = queue.peek(preferredIntermediateHashesSize);
                currentHashSize = rangeForHash.size();
                amountOfCurrentHashSent = 0;
                return new RemovedRange(rangeForHash, true);
            } else {
                long maxChunkSize;
                if (currentHashSize > 0) {
                    // the limit is the current hash size
                    maxChunkSize = Math.min(preferredBlockSize, currentHashSize - amountOfCurrentHashSent);
                } else {
                    maxChunkSize = preferredBlockSize;
                }
                LongRange removedRange = queue.remove(maxChunkSize);
                amountOfCurrentHashSent += removedRange.size();
                if (amountOfCurrentHashSent == currentHashSize) {
                    // the segment for the current hash has been fully sent -> reset
                    amountOfCurrentHashSent = 0;
                    currentHashSize = 0;
                }
                return new RemovedRange(removedRange, false);
            }
        }

        boolean isEmpty() {
            return queue.isEmpty();
        }

        void clear() {
            queue.clear();
        }

        void add(LongRange range) {
            queue.add(range);
        }

        List<LongRange> getRanges() {
            return queue.getRanges();
        }
    }

    /**
     * The time in millis that this slaves can exist without receiving any input
     */
    public static final long SURVIVE_TIME_MILLIS = 30000;

    final static LongRange stopMessage = new LongRange(-1l, 0l);

    static final String HASH_ALGORITHM = "SHA-256";

    /**
     * The initial uploading speed (25KB allows files around 100kb to be transferred quickly, and still does not heavily affect other transfers)
     * todo maybe this value should be variable, depending on the last transfers (average speed of each individual transfer)
     */
    public static final float INITIAL_SPEED = 25600;

    private final UniqueIdentifier id;

    private final jacz.peerengine.util.datatransfer.ResourceStreamingManager resourceStreamingManager;

    private short incomingChannel;

    private boolean initialized;

    private ResourceReader resourceReader;

    private PeerID otherPeer;

    private short outgoingChannel;

    private SlaveMessageReader messageReader;

    private ResourceSegmentQueue resourceSegmentQueue;

    /**
     * The speed that the other end has requested us to transmit at. This speed can never be surpassed
     * If null, it means that the master still has not provided us with a value. We set it at the initial speed
     *
     * This value also acts as priority request by the master
     */
    private Float masterRequestedMaxSpeed;

    private boolean alive;

    /**
     * Timer for controlling timeouts
     */
    private final Timer timeoutTimer;

    /**
     * This fields provides data about the peer to which we serve and about the resource that we serve
     */
    private final jacz.peerengine.util.datatransfer.ResourceRequest resourceRequest;

    private jacz.peerengine.util.datatransfer.slave.UploadSessionStatistics uploadSessionStatistics;


    public SlaveResourceStreamer(jacz.peerengine.util.datatransfer.ResourceStreamingManager resourceStreamingManager, jacz.peerengine.util.datatransfer.ResourceRequest request) {
        id = UniqueIdentifierFactory.getOneStaticIdentifier();
        this.resourceStreamingManager = resourceStreamingManager;
        this.resourceRequest = request;
        timeoutTimer = new Timer(SURVIVE_TIME_MILLIS, this);
        masterRequestedMaxSpeed = INITIAL_SPEED;
        initialized = false;
        alive = true;
    }

    public synchronized void initialize(ResourceReader resourceReader, PeerID otherPeer, short incomingChannel, short outgoingChannel, jacz.peerengine.util.datatransfer.slave.UploadManager uploadManager) {
        this.resourceReader = resourceReader;
        this.otherPeer = otherPeer;
        this.incomingChannel = incomingChannel;
        this.outgoingChannel = outgoingChannel;
        resourceSegmentQueue = new ResourceSegmentQueue(resourceRequest.getPreferredIntermediateHashesSize());
        messageReader = new SlaveMessageReader(this, resourceSegmentQueue, resourceReader, INITIAL_SPEED);
        uploadSessionStatistics = uploadManager.getUploadSessionStatistics();
        MessageHandler messageHandler = new jacz.peerengine.util.datatransfer.slave.SlaveMessageHandler(resourceStreamingManager, otherPeer, outgoingChannel, uploadSessionStatistics);
        MessageProcessor dataSender = new MessageProcessor(messageReader, messageHandler, false);
        dataSender.start();
        sendInitializationMessage(incomingChannel);
        initialized = true;
    }

    private void sendInitializationMessage(short incomingChannel) {
        ObjectListWrapper message = new ObjectListWrapper(true, incomingChannel);
        resourceStreamingManager.write(otherPeer, outgoingChannel, message);
    }


    @Override
    public synchronized void processMessage(short subchannel, Object message) {
        timeoutTimer.reset(SURVIVE_TIME_MILLIS);
    }

    @Override
    public synchronized void processMessage(short subchannel, byte[] data) {
        // new message from master. First we  retrieve the order from the master, and then, if necessary, its
        // corresponding data. The first byte of the data is the Order, and the rest of bytes the additional info
        if (initialized) {
            timeoutTimer.reset(SURVIVE_TIME_MILLIS);
            if (data.length < 1) {
                // wrong data, ignore
                return;
            }
            MasterMessage masterMessage = new MasterMessage(data);
            if (masterMessage.order != null) {
                switch (masterMessage.order) {
                    case REPORT_RESOURCE_LENGTH:
                        try {
                            // send the master the resource size
                            resourceStreamingManager.write(otherPeer, outgoingChannel, jacz.peerengine.util.datatransfer.slave.SlaveMessage.generateResourceSizeMessage(resourceReader.length()));
                        } catch (IOException e) {
                            die(true);
                        }
                        break;

                    case REPORT_AVAILABLE_SEGMENTS:
                        // send the master our available resource part
                        try {
                            ResourcePart resourcePart = new ResourcePart(resourceReader.availableSegments());
                            resourceStreamingManager.write(otherPeer, outgoingChannel, jacz.peerengine.util.datatransfer.slave.SlaveMessage.generateResourceAvailabilityMessage(resourcePart));
                        } catch (IOException e) {
                            die(true);
                        }
                        break;

                    case REPORT_ASSIGNED_SEGMENTS:
                        ResourcePart resourcePart = new ResourcePart(resourceSegmentQueue.getRanges());
                        resourceStreamingManager.write(otherPeer, outgoingChannel, jacz.peerengine.util.datatransfer.slave.SlaveMessage.generateAssignedSegmentsMessage(resourcePart));
                        break;

                    case ERASE_SEGMENTS:
                        resourceSegmentQueue.clear();
                        break;

                    case ADD_NEW_SEGMENT:
                        // get first and last byte of the segment to add
                        try {
                            if (resourceReader.availableSegments().contains(masterMessage.segment)) {
                                // the reader does have this segment
                                resourceSegmentQueue.add(masterMessage.segment);
                                uploadSessionStatistics.addAssignedSegment(masterMessage.segment);
                            } else {
                                // the reader does not have this segment -> report master
                                resourceStreamingManager.write(otherPeer, outgoingChannel, jacz.peerengine.util.datatransfer.slave.SlaveMessage.generateUnavailableSegmentsMessage());
                            }
                        } catch (IOException e) {
                            die(true);
                        }
                        break;

                    case SET_SPEED:
                        masterRequestedMaxSpeed = masterMessage.speed;
                        break;

                    case PING:
                        // a ping message to keep us alive -> ignore
                        break;

                    case DIED:
                        die(false);
                        break;

                }
            }
        }
    }


    @Override
    public void setSpeed(float speed) {
        float finalSpeed = (masterRequestedMaxSpeed != null) ? Math.min(masterRequestedMaxSpeed, speed) : speed;
        messageReader.setSpeed(finalSpeed);
    }

    @Override
    public Float getAchievedSpeed() {
        return messageReader.getAchievedSpeed();
    }


    public short getIncomingChannel() {
        return incomingChannel;
    }

    public jacz.peerengine.util.datatransfer.ResourceRequest getResourceRequest() {
        return resourceRequest;
    }

    synchronized void die(boolean mustReportMaster) {
        if (alive) {
            // not always it is needed to report the master. If the die cause is that the master reported us that
            // he himself has died, we should not report him our own death
            if (mustReportMaster) {
                // send a message to the master reporting that we die
                resourceStreamingManager.write(otherPeer, outgoingChannel, jacz.peerengine.util.datatransfer.slave.SlaveMessage.generateDiedMessage());
            }
            stopProcessor();
            timeoutTimer.kill();
            resourceStreamingManager.reportDeadSlaveResourceStreamer(this);
            alive = false;
        }
    }

    private void stopProcessor() {
        resourceSegmentQueue.clear();
        resourceSegmentQueue.add(stopMessage);
    }

    @Override
    public Long wakeUp(Timer timer) {
        // too much time without receiving any input -> die
        die(true);
        return 0l;
    }

    public UniqueIdentifier getId() {
        return id;
    }

    @Override
    public String getStringId() {
        return getId().toString();
    }

    @Override
    public float getPriority() {
        return masterRequestedMaxSpeed;
    }
}
