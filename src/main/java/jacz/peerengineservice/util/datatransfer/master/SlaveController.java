package jacz.peerengineservice.util.datatransfer.master;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.datatransfer.GenericPriorityManagerRegulatedResource;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceLink;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceProvider;
import jacz.peerengineservice.util.datatransfer.slave.SlaveMessage;
import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.concurrency.task_executor.ParallelTaskExecutor;
import jacz.util.concurrency.timer.SimpleTimerAction;
import jacz.util.concurrency.timer.Timer;
import jacz.util.date_time.RemainingTimeAction;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.identifier.UniqueIdentifierFactory;
import jacz.util.io.serialization.ObjectListWrapper;
import jacz.util.numeric.range.LongRange;

/**
 * This private class stores information about one active slave, and handles some messages from such slave.
 * <p/>
 * It is also in charge of requesting parts of the resource to download to the corresponding scheduler.
 * <p/>
 * Concurrency -> all OK:
 * The slave controller gets input from several different threads and objects. The process message and pause/resume methods all come from
 * different threads, but these are always from the MasterResourceStreamer, and blocking this object. These calls either die at the slave
 * controller or go back to the MasterResourcesStreamer.
 * There are several timers for controlling the behavior of the slave controller, but all their calls die at the slave controller itself, except
 * for one: timeoutTimer for dying. This call goes to the MasterResourcesStreamer, but it is properly parallelized, so it is OK.
 */
public class SlaveController extends GenericPriorityManagerRegulatedResource implements SimpleTimerAction, RemainingTimeAction {

    /**
     * States of each SlaveController
     */
    private enum State {
        // not running yet, awaiting confirmation message from the slave
        AWAITING_REQUEST_RESPONSE,
        // running and receiving data
        RECEIVING_DATA
    }


    /**
     * Time in millis that a slave is allowed to live without producing any signal
     */
    private static final long SLAVE_TIMEOUT_MILLIS = 20000;

    private static final long MILLIS_TO_MEASURE_SPEED = 5000;

    private static final int MILLIS_ALLOWED_OUT_OF_SPEED_RANGE = 15000;

    private static final long MILLIS_REMAINING_FOR_REPORT = 20000;

    /**
     * Must be smaller than slave's own timeout, or timeout will happen before
     */
    private static final long MILLIS_FOR_ASSIGNMENT_RECHECK = SLAVE_TIMEOUT_MILLIS * 3 / 4;

    /**
     * Time between periodical segment availability requests
     */
    private static final long MILLIS_FOR_AUTOMATIC_SEGMENT_AVAILABILITY_REQUEST = SLAVE_TIMEOUT_MILLIS * 5;

    /**
     * Unique identifier required for some tasks (segment assignment)
     */
    private final UniqueIdentifier id;

    /**
     * The master resource streamer for which this slave data works
     */
    private final MasterResourceStreamer masterResourceStreamer;

    /**
     * Boolean indicated whether the size of the resource is already known. If not, this controller will request
     * the size to the resource link
     */
    private boolean sizeIsKnown;

    /**
     * The resource link for the resource being downloaded
     */
    private final ResourceLink resourceLink;

    /**
     * The provider of the resource
     */
    private final ResourceProvider resourceProvider;

    /**
     * The provider id of the resource provider
     */
    private final PeerID resourceProviderId;

    /**
     * The subchannel through which the associated slave transmits data
     */
    private final short subchannel;

    /**
     * The state of this slave controller
     */
    private State state;

    /**
     * Object for controlling the resource segments assigned to this slave, as well as monitoring the parameters
     * associated to this transfer (speed and time remaining for completion of assignment)
     */
    private ResourceSegmentQueueWithMonitoring resourceSegmentQueueWithMonitoring;

    /**
     * The timer for controlling if the slave has not answered the initial request in the specified time. This
     * timer is also used to control slave's timeouts (once it is not used for the initial requests, it will be
     * used to control timeouts)
     */
    private final Timer timeoutTimer;

    /**
     * The timer for keeping the resource link alive (might need regular feedback not to die)
     */
    private final Timer resourceLinkTimeoutTimer;

    /**
     * Timer for submitting a new assignation request to the scheduler. Used when we got no assignation and we
     * still want one
     */
    private Timer requestAssignationTimer;

    /**
     * Timer for submitting available segment requests. We issue these periodical requests in case some self
     * reports from the slave get lost in the transmission, producing starvation on both sides
     */
    private final Timer requestAvailableSegmentsTimer;

    /**
     * Object in charge of assigning us the parts of the resource to download
     */
    private final ResourcePartScheduler resourcePartScheduler;

    /**
     * Whether this slave is active (true) or paused (false)
     */
    private boolean active;

    /**
     * Whether this resource is alive (it is not alive when the master tells him to stop, and it cannot be revived)
     */
    private boolean alive;

    SlaveController(MasterResourceStreamer masterResourceStreamer, boolean sizeIsKnown, ResourceLink resourceLink, ResourceProvider resourceProvider, short subchannel, long requestLifeMillis, ResourcePartScheduler resourcePartScheduler, boolean active) {
        id = UniqueIdentifierFactory.getOneStaticIdentifier();
        this.masterResourceStreamer = masterResourceStreamer;
        this.sizeIsKnown = sizeIsKnown;
        this.resourceLink = resourceLink;
        this.resourceProvider = resourceProvider;
        this.resourceProviderId = resourceProvider.getPeerID();
        this.subchannel = subchannel;
        state = State.AWAITING_REQUEST_RESPONSE;
        timeoutTimer = new Timer(requestLifeMillis, this, true, "MasterResourceStreamer/timeoutTimer");
        resourceLinkTimeoutTimer = new Timer((resourceLink.surviveTimeMillis() * 2) / 3, this, false, "resourceLinkTimeoutTimer");
        requestAssignationTimer = null;
        requestAvailableSegmentsTimer = new Timer(MILLIS_FOR_AUTOMATIC_SEGMENT_AVAILABILITY_REQUEST, this);
        resourceSegmentQueueWithMonitoring = new ResourceSegmentQueueWithMonitoring(MILLIS_TO_MEASURE_SPEED, this, new LongRange(null, null), MILLIS_ALLOWED_OUT_OF_SPEED_RANGE, MILLIS_REMAINING_FOR_REPORT);
        this.resourcePartScheduler = resourcePartScheduler;
        this.resourcePartScheduler.addSlave(this);
        this.active = active;
        alive = true;
    }

    UniqueIdentifier getId() {
        return id;
    }

    ResourceLink getResourceLink() {
        return resourceLink;
    }

    ResourceProvider getResourceProvider() {
        return resourceProvider;
    }

    PeerID getResourceProviderId() {
        return resourceProviderId;
    }

    short getSubchannel() {
        return subchannel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SlaveController)) return false;

        SlaveController that = (SlaveController) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    private synchronized void setUpTimeoutTimer(long millis) {
        timeoutTimer.reset(millis);
    }

    /**
     * This method resets the timeout timer with the no activity check millis. Invoked for every message we
     * receive from the slave controller
     */
    private synchronized void resetTimeoutTimer() {
        setUpTimeoutTimer(SLAVE_TIMEOUT_MILLIS);
    }

    private synchronized void setUpRequestAssignationTimer() {
        // this has to be done in a separate thread to avoid synch issues
        stopRequestAssignationTimer();
        requestAssignationTimer = new Timer(MILLIS_FOR_ASSIGNMENT_RECHECK, this, true, "MasterResourceStreamer/requestAssignationTimer");
    }

    private synchronized boolean isWaitingForRequestResponse() {
        return state == State.AWAITING_REQUEST_RESPONSE;
    }

    /**
     * Initializes this slave controller with an initialization message from the corresponding slave. If the request
     * was denied, this slave controller will take care of erasing himself
     *
     * @param message initialization message received by the slave. Should be an object list wrapper containing
     *                at least one object. The first is a boolean (true if the request was accepted, false
     *                otherwise). If the request was accepted there should be a second object with the actual
     *                initialization data for the resource link
     */
    private void initialize(Object message) {
        try {
            if (message instanceof ObjectListWrapper) {
                ObjectListWrapper initMessage = (ObjectListWrapper) message;
                if (initMessage.getObjects().size() >= 1 && initMessage.getObjects().get(0) instanceof Boolean && (Boolean) initMessage.getObjects().get(0)) {
                    resourceLink.initialize(initMessage.getObjects().get(1));
                    if (sizeIsKnown) {
                        resourceLink.requestAvailableSegments();
                    } else {
                        resourceLink.requestResourceLength();
                    }
                    if (resourceLink.surviveTimeMillis() != null) {
                        resourceLinkTimeoutTimer.reset();
                    }
                    state = State.RECEIVING_DATA;
                } else {
                    // request denied
                    die(false);
                }
            } else {
                // unexpected message format (assume the slave was not created -> do not signal death message)
                die(false);
            }
        } catch (IllegalArgumentException e) {
            // we received an invalid initialization object. No death signal is sent because the resource link is not properly initialized
            // and thus cannot send messages correctly
            die(false);
        }

    }

    synchronized void processMessage(Object message) {
        // it we are still waiting for an initialization message, initialize, as it is presumably such message.
        // if not, die as every message must be received as byte[]
        if (alive) {
            resetTimeoutTimer();
            if (isWaitingForRequestResponse()) {
                initialize(message);
            } else {
                // object data was not expected here
                die(true);
            }
        }
    }

    synchronized void processMessage(byte[] data) {
        // if we are still waiting for initialization data, die as we did not expect bytes. Otherwise, process it properly
        if (alive) {
            resetTimeoutTimer();
            if (isWaitingForRequestResponse()) {
                // byte array was not expected here
                die(true);
            } else {
                SlaveMessage slaveMessage = new SlaveMessage(data);
                if (slaveMessage.messageType != null) {
                    switch (slaveMessage.messageType) {

                        case RESOURCE_CHUNK:
                            boolean correct = resourceSegmentQueueWithMonitoring.removeRange(slaveMessage.resourceChunk.getSegment());
                            if (correct) {
                                try {
                                    // we do not need to parallelize this call because the processMessage method is itself invoked from the MasterResourcesStreamer
                                    masterResourceStreamer.writeData(slaveMessage.resourceChunk);
                                    resourcePartScheduler.reportDownloadedSegment(this, slaveMessage.resourceChunk);
                                    // if we run out of assignment, ask for more
                                    if (alive && resourceSegmentQueueWithMonitoring.isEmpty()) {
                                        requestAssignment();
                                    }
                                } catch (IllegalArgumentException e) {
                                    // the received data contained errors -> kill this slave
                                    die(true);
                                }
                            }
                            break;

                        case RESOURCE_SIZE_REPORT:
                            if (!sizeIsKnown) {
                                // report the master streamer of the obtained value
                                // we do not need to parallelize this call because the processMessage method is itself invoked from the MasterResourcesStreamer
                                masterResourceStreamer.reportResourceSize(slaveMessage.resourceSize);
                            }
                            break;

                        case SEGMENT_AVAILABILITY_REPORT:
                            resourcePartScheduler.setSlaveShare(this, slaveMessage.resourcePart);
                            // if nothing is currently assigned to this slave, request an assignment
                            if (alive && resourceSegmentQueueWithMonitoring.isEmpty()) {
                                requestAssignment();
                            }
                            break;

                        case SEGMENT_ASSIGNATION_REPORT:
                            // ignore this message. See if this is useful in the future
                            break;

                        case UNAVAILABLE_SEGMENT_WARNING:
                            // we have wrong segments assigned to this slave, request again his share
                            resourceLink.requestAvailableSegments();
                            break;

                        case DIED:
                            // the slave died and notified us so
                            die(false);
                            break;
                    }
                }
            }
        }
    }

    synchronized void reportSizeIsKnown() {
        if (alive) {
            if (!sizeIsKnown) {
                sizeIsKnown = true;
                resourceLink.requestAvailableSegments();
            }
        }
    }

    synchronized void pause() {
        if (alive) {
            if (active) {
                active = false;
                eraseCurrentAssignment();
            }
        }
    }

    synchronized void resume() {
        if (alive) {
            if (!active) {
                active = true;
                requestAssignment();
            }
        }
    }

    private synchronized void stop() {
        stopTimeoutTimer();
        stopResourceLinkTimeoutTimer();
        stopRequestAssignationTimer();
        stopRequestAvailableSegmentsTimer();
        stopResourceSegmentQueueWithMonitoring();
        resourcePartScheduler.removeSlave(this);
        alive = false;
    }

    private synchronized void stopTimeoutTimer() {
        timeoutTimer.kill();
    }

    private synchronized void stopResourceLinkTimeoutTimer() {
        resourceLinkTimeoutTimer.kill();
    }

    private synchronized void stopRequestAssignationTimer() {
        if (requestAssignationTimer != null) {
            requestAssignationTimer.kill();
        }
    }

    private synchronized void stopRequestAvailableSegmentsTimer() {
        requestAvailableSegmentsTimer.kill();
    }

    private synchronized void stopResourceSegmentQueueWithMonitoring() {
        if (resourceSegmentQueueWithMonitoring != null) {
            resourceSegmentQueueWithMonitoring.stop();
            resourceSegmentQueueWithMonitoring = null;
        }
    }

    @Override
    public synchronized Long wakeUp(Timer timer) {
        if (alive) {
            if (timer == timeoutTimer) {
                // this slave died, either from too much time to answer the initial requests, or from too much time without
                // any activity (the reason does not matter at this point). The timer is killed afterwards
                die(true);
                return 0l;
            } else if (timer == requestAssignationTimer) {
                // we had marked this slave for a later request assignation (either due to the slave being too slow or not having useful parts)
                // The timer finished so we make a new request. The timer is killed afterwards
                requestAssignment();
                return 0l;
            } else if (timer == resourceLinkTimeoutTimer) {
                // ping the resource link so it does not die. The timer is reset with the same previous time
                resourceLink.ping();
                return null;
            } else if (timer == requestAvailableSegmentsTimer) {
                // request segment availability from slave (to make sure it is updated). The timer keeps running forever
                resourceLink.requestAvailableSegments();
                return null;
            }
        }
        // sometimes the timeout timer can remain alive after being stop due to the concurrentReset executing after the concurrentStop. We kill it here
        return 0l;
    }

    @Override
    public synchronized void remainingTime(long millis) {
        // time remaining is low
        if (alive) {
            requestAssignment();
        }
    }

    @Override
    public void speedAboveRange(double speed) {
        // ignore, no need for any action
    }

    @Override
    public synchronized void speedBelowRange(double speed) {
        if (alive) {
            eraseCurrentAssignment();
            requestAssignment();
        }
    }

    private void eraseCurrentAssignment() {
        resourceLink.eraseSegments();
        resourcePartScheduler.removeCurrentAssignment(this);
        resourceSegmentQueueWithMonitoring.clear();
    }

    private void requestAssignment() {
        if (alive && active) {
            ObjectListWrapper assignment = resourcePartScheduler.requestAssignation(this, resourceSegmentQueueWithMonitoring.getAverageSpeed());
            if (assignment.getObjects().size() == 2) {
                // we got something assigned
                LongRange assignedSegment = (LongRange) assignment.getObjects().get(0);
                resourceSegmentQueueWithMonitoring.add(assignedSegment);
                resourceSegmentQueueWithMonitoring.setSpeedMonitorRange((LongRange) assignment.getObjects().get(1));
                resourceLink.addNewSegment(assignedSegment);
            } else {
                ResourcePartScheduler.NoAssignationCause noAssignationCause = (ResourcePartScheduler.NoAssignationCause) assignment.getObjects().get(0);
                switch (noAssignationCause) {

                    case LOW_SPEED:
                        // remove all assignation to this slave (try later). Also reset timeout to avoid dying
                        eraseCurrentAssignment();
                        resetTimeoutTimer();
                        setUpRequestAssignationTimer();
                        break;
                    case SIZE_NOT_KNOWN:
                        // ask this slave for resource size
                        resourceLink.requestResourceLength();
                        break;
                    case SLAVE_NOT_FOUND:
                        // for some reason this slave was not registered in the scheduler -> die and create again later
                        die(true);
                        break;
                    case ERROR_IN_ASSESSMENT:
                        // error in the assignment calculation -> request available segments again
                        resourceLink.requestAvailableSegments();
                        break;
                    case NO_USEFUL_PARTS:
                        // this slave has no useful parts -> try later (maybe some part will become useful)
                        // we also reset the timeout to avoid dying for non useful parts
                        resetTimeoutTimer();
                        setUpRequestAssignationTimer();
                        break;
                }
                // schedule for later request
            }
        }
    }

    private void die(final boolean mustReportSlave) {
        ParallelTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                masterResourceStreamer.removeSlave(subchannel, mustReportSlave);
            }
        });
    }

    public synchronized void finishExecution() {
        stop();
    }

    @Override
    public float getPriority() {
        // we return a fixed value, since we want equality at this level
        return 1f;
    }

    @Override
    public float getAchievedSpeed() {
        return masterResourceStreamer.getSlaveControllerAchievedSpeed(this);
    }

    @Override
    public void hardThrottle(float variation) {
        resourceLink.hardThrottle(variation);
    }

    @Override
    public void softThrottle() {
        resourceLink.softThrottle();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        stop();
    }
}
