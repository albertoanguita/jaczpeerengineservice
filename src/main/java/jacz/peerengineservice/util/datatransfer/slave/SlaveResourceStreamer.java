package jacz.peerengineservice.util.datatransfer.slave;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.datatransfer.GenericPriorityManagerRegulatedResource;
import jacz.peerengineservice.util.datatransfer.ResourceStreamingManager;
import jacz.peerengineservice.util.datatransfer.master.MasterMessage;
import jacz.peerengineservice.util.datatransfer.master.ResourcePart;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceReader;
import jacz.util.concurrency.timer.TimerAction;
import jacz.util.concurrency.timer.Timer;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.identifier.UniqueIdentifierFactory;
import jacz.util.io.serialization.ObjectListWrapper;
import jacz.util.numeric.range.LongRange;
import jacz.util.numeric.range.LongRangeQueue;
import jacz.util.queues.event_processing.MessageProcessor;

import java.io.IOException;
import java.util.List;

/**
 * This class handles a slave that serves a resource to a master
 */
public class SlaveResourceStreamer extends GenericPriorityManagerRegulatedResource implements ResourceStreamingManager.SubchannelOwner, TimerAction {

    static class RemovedRange {

        final LongRange range;

        RemovedRange(LongRange range) {
            this.range = range;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RemovedRange that = (RemovedRange) o;

            if (!range.equals(that.range)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = range.hashCode();
            result = 31 * result;
            return result;
        }
    }

    static class ResourceSegmentQueue {

        private final LongRangeQueue queue;

        private long currentHashSize;

        private long amountOfCurrentHashSent;

        ResourceSegmentQueue() {
            queue = new LongRangeQueue();
            amountOfCurrentHashSent = 0;
            currentHashSize = 0;
        }

        RemovedRange remove(long preferredBlockSize) {
            if (!queue.isEmpty() && queue.getRanges().get(0) == stopMessage) {
                // issue stop order
                return new RemovedRange((LongRange) queue.remove(stopMessage.size()));
            }
            long maxChunkSize;
            if (currentHashSize > 0) {
                // the limit is the current hash size
                maxChunkSize = Math.min(preferredBlockSize, currentHashSize - amountOfCurrentHashSent);
            } else {
                maxChunkSize = preferredBlockSize;
            }
            LongRange removedRange = (LongRange) queue.remove(maxChunkSize);
            amountOfCurrentHashSent += removedRange.size();
            if (amountOfCurrentHashSent == currentHashSize) {
                // the segment for the current hash has been fully sent -> reset
                amountOfCurrentHashSent = 0;
                currentHashSize = 0;
            }
            return new RemovedRange(removedRange);
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

    final static LongRange stopMessage = new LongRange(-1L, 0L);

    static final String HASH_ALGORITHM = "SHA-256";

    private final UniqueIdentifier id;

    private final jacz.peerengineservice.util.datatransfer.ResourceStreamingManager resourceStreamingManager;

    private short incomingChannel;

    private boolean initialized;

    private ResourceReader resourceReader;

    private PeerId otherPeer;

    private short outgoingChannel;

    private SlaveMessageReader messageReader;

    private ResourceSegmentQueue resourceSegmentQueue;

    private boolean alive;

    /**
     * Timer for controlling timeouts
     */
    private final Timer timeoutTimer;

    /**
     * This fields provides data about the peer to which we serve and about the resource that we serve
     */
    private final jacz.peerengineservice.util.datatransfer.ResourceRequest resourceRequest;

    public SlaveResourceStreamer(jacz.peerengineservice.util.datatransfer.ResourceStreamingManager resourceStreamingManager, jacz.peerengineservice.util.datatransfer.ResourceRequest request) {
        id = UniqueIdentifierFactory.getOneStaticIdentifier();
        this.resourceStreamingManager = resourceStreamingManager;
        this.resourceRequest = request;
        timeoutTimer = new Timer(SURVIVE_TIME_MILLIS, this);
        initialized = false;
        alive = true;
    }

    public synchronized void initialize(ResourceReader resourceReader, PeerId otherPeer, short incomingChannel, short outgoingChannel) {
        this.resourceReader = resourceReader;
        this.otherPeer = otherPeer;
        this.incomingChannel = incomingChannel;
        this.outgoingChannel = outgoingChannel;
        resourceSegmentQueue = new ResourceSegmentQueue();
        SlaveMessageHandler messageHandler = new SlaveMessageHandler(resourceStreamingManager, otherPeer, outgoingChannel);
        messageReader = new SlaveMessageReader(this, resourceSegmentQueue, resourceReader, messageHandler);
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
                            resourceStreamingManager.write(otherPeer, outgoingChannel, SlaveMessage.generateResourceSizeMessage(resourceReader.length()), false);
                        } catch (IOException e) {
                            die(true);
                        }
                        break;

                    case REPORT_AVAILABLE_SEGMENTS:
                        // send the master our available resource part
                        try {
                            ResourcePart resourcePart = new ResourcePart(resourceReader.availableSegments());
                            resourceStreamingManager.write(otherPeer, outgoingChannel, SlaveMessage.generateResourceAvailabilityMessage(resourcePart), false);
                        } catch (IOException e) {
                            die(true);
                        }
                        break;

                    case REPORT_ASSIGNED_SEGMENTS:
                        ResourcePart resourcePart = new ResourcePart(resourceSegmentQueue.getRanges());
                        resourceStreamingManager.write(otherPeer, outgoingChannel, SlaveMessage.generateAssignedSegmentsMessage(resourcePart), false);
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
                            } else {
                                // the reader does not have this segment -> report master
                                resourceStreamingManager.write(otherPeer, outgoingChannel, SlaveMessage.generateUnavailableSegmentsMessage(), false);
                            }
                        } catch (IOException e) {
                            die(true);
                        }
                        break;

                    case HARD_THROTTLE:
                        hardThrottle(masterMessage.throttle);
                        break;

                    case SOFT_THROTTLE:
                        softThrottle();
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
    public float getAchievedSpeed() {
        return messageReader.getAchievedSpeed();
    }

    public void hardThrottle(float variation) {
        messageReader.hardThrottle(variation);
    }

    public void softThrottle() {
        messageReader.softThrottle();
    }

    public short getIncomingChannel() {
        return incomingChannel;
    }

    public jacz.peerengineservice.util.datatransfer.ResourceRequest getResourceRequest() {
        return resourceRequest;
    }

    synchronized void die(boolean mustReportMaster) {
        if (alive) {
            // not always it is needed to report the master. If the die cause is that the master reported us that
            // he himself has died, we should not report him our own death
            if (mustReportMaster) {
                // send a message to the master reporting that we die
                resourceStreamingManager.write(otherPeer, outgoingChannel, SlaveMessage.generateDiedMessage(), false);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SlaveResourceStreamer that = (SlaveResourceStreamer) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }

    @Override
    public float getPriority() {
        return 1;
    }
}
