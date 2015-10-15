package jacz.peerengineservice.util.datatransfer.master;

import jacz.util.date_time.RemainingTimeAction;
import jacz.util.date_time.SpeedMonitorWithRemainingTime;
import jacz.util.numeric.range.LongRange;
import jacz.util.numeric.range.LongRangeQueue;

import java.util.List;

/**
 * This class adds speed monitoring features to the ResourceSegmentQueue class
 */
public class ResourceSegmentQueueWithMonitoring extends LongRangeQueue {

    /**
     * The object that controls speed
     */
    private SpeedMonitorWithRemainingTime speedMeasureWithRemainingTime;

    /**
     * Maximum time used to measure speed
     */
    private long millisToMeasure;

    /**
     * The action to be invoked when the time to complete is nearly over
     */
    private RemainingTimeAction remainingTimeAction;

    /**
     * The speed ranges to control
     */
    private LongRange speedMonitorRange;

    /**
     * Time allowed to pass between the detection of a speed anomaly and its corresponding notification). If 0, it is
     * directly notified without elapse of time
     */
    private int millisForSpeedMonitoring;

    /**
     * The time under which we want to be reported
     */
    private long remainingTimeToReport;

    public ResourceSegmentQueueWithMonitoring(
            long millisToMeasure,
            RemainingTimeAction remainingTimeAction,
            LongRange speedMonitorRange,
            int millisForSpeedMonitoring,
            long remainingTimeToReport) {
        super();
        speedMeasureWithRemainingTime = null;
        this.millisToMeasure = millisToMeasure;
        this.remainingTimeAction = remainingTimeAction;
        this.speedMonitorRange = speedMonitorRange;
        this.millisForSpeedMonitoring = millisForSpeedMonitoring;
        this.remainingTimeToReport = remainingTimeToReport;
        initSpeedMeasure(millisToMeasure, remainingTimeAction, speedMonitorRange, millisForSpeedMonitoring, remainingTimeToReport);
    }

    /**
     * Begins monitoring the speed (or re-initiates it if it was already running)
     *
     * @param millisToMeasure          maximum time used to measure speed
     * @param remainingTimeAction      action to be invoked by the speed monitor
     * @param speedMonitorRange        acceptable speed range
     * @param millisForSpeedMonitoring time allowed to pass between the detection of a speed anomaly and its
     *                                 corresponding notification). If 0, it is directly notified without elapse
     *                                 of time
     * @param remainingTimeToReport    the time under which we want to be reported
     */
    private void initSpeedMeasure(long millisToMeasure, RemainingTimeAction remainingTimeAction, LongRange speedMonitorRange, int millisForSpeedMonitoring, long remainingTimeToReport) {
        // the speed measure object is initialized with 0 capacity and the given initial parameters
        stop();
        speedMeasureWithRemainingTime =
                new SpeedMonitorWithRemainingTime(
                        millisToMeasure,
                        0,
                        remainingTimeAction,
                        remainingTimeToReport,
                        speedMonitorRange,
                        millisForSpeedMonitoring,
                        "ResourceSegmentQueueWithMonitoring"
                );
    }

    /**
     * Stops the timers associated to the reporting of speed limits or remaining times. It is recommended to invoke
     * this method whenever an object of this class is no longer to be used, since there could be unexpected
     * reports
     */
    public void stop() {
        if (speedMeasureWithRemainingTime != null) {
            speedMeasureWithRemainingTime.stop();
        }
    }

    @Override
    public synchronized void clear() {
        super.clear();
        initSpeedMeasure(millisToMeasure, remainingTimeAction, speedMonitorRange, millisForSpeedMonitoring, remainingTimeToReport);
    }

    public synchronized void setSpeedMonitorRange(LongRange newSpeedMonitorRange) {
        this.speedMonitorRange = newSpeedMonitorRange;
        speedMeasureWithRemainingTime.setSpeedMonitorRange(newSpeedMonitorRange);
    }

    @Override
    public synchronized void add(LongRange range) {
        super.add(range);
        speedMeasureWithRemainingTime.addCapacity(range.size());
    }

    public synchronized void add(List<LongRange> segments) {
        long size = 0;
        for (LongRange aSegment : segments) {
            super.add(aSegment);
            size += aSegment.size();
        }
        speedMeasureWithRemainingTime.addCapacity(size);
    }

    @Override
    public LongRange remove(Long maxSize) {
        LongRange res = (LongRange) super.remove(maxSize);
        speedMeasureWithRemainingTime.addProgress(res.size());
        return res;
    }

    @Override
    public synchronized boolean removeRange(LongRange receivedRange) {
        boolean result = super.removeRange(receivedRange);
        if (result) {
            speedMeasureWithRemainingTime.addProgress(receivedRange.size());
        }
        return result;
    }

    public synchronized double getAverageSpeed() {
        return speedMeasureWithRemainingTime.getAverageSpeed();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
