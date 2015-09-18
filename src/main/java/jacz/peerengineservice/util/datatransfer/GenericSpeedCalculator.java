package jacz.peerengineservice.util.datatransfer;

import jacz.util.AI.resource_distribution.AdaptiveResourceDistributor2;
import jacz.util.lists.Duple;
import jacz.util.lists.Four_Tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic speed calculator, for both downloads and uploads
 * <p/>
 * This class contains static methods that allow calculating the upload speeds that must be
 * assigned to each active transfer. The goal is to assign delivery speeds for each active transfer in an equitable and
 * optimal way. To do this, old assigned and achieved speeds are analyzed. We do not want to assign to much speed, to
 * avoid exceeding the total allowed upload speed or to avoid choking our upload speed. If in the last run we achieved
 * lower speeds than expected, the assigned speeds will be lowered. If the achieved speeds surpassed a given threshold,
 * the assigned speeds will be increased.
 * <p/>
 * The total assigned speed is distributed among the individual peers. In principle, each peer gets an equal slice
 * of the total assigned speed. But if any peer wishes to get less, or in the last run was not able to achieve that
 * much speed, he will get a smaller slice (the remaining is given to the other peers). We start the distribution by
 * the peers that achieved less speed, since they are more probably going to require less speed. Nevertheless, if
 * at the end there is remaining speed to distribute, nothing is done with it, leaving it for subsequent runs.
 * <p/>
 * Finally, the assigned speed to each peer is distributed among their individual active transfers. Peers can
 * specify a desired distribution among these transfers (if not specified, it is distributed uniformly).
 * <p/>
 * The arguments of the method are given as an inner defined class which contains all needed values.
 * As a result of the calculations, an object of the inner defined class Result containing the total assigned speed,
 * the speed assigned to each peer, and the speed assigned to each transfer is returned. There is no specific way
 * to determine the individual transfers of each peer, the method returns lists with speed limits in the same order
 * as given in the input.
 */
public class GenericSpeedCalculator {

    /**
     * The data that receives the main method of this class. This inner class defines all the
     * attributes needed for the upload speed calculations. These are:
     * <p/>
     * - Total desired speed
     * - Last total assigned speed
     * - Total desired speed for each active peer
     * - Last total assigned speed for each active peer
     * - Last achieved speeds (for each peer and for each resource)
     * - Requested division for each peer (the proportion of speed that he wants to assign to each resource). A null
     * value for a peer, or a not appearing peer, indicate that no request is made
     */
    static class Input<T> {

        Float totalMaxDesiredSpeed;

        float lastTotalAssignedSpeed;

        /**
         * We maintain this list of peer IDs because we need an ordered list of the peers (results of calculation are given as a list that
         * follows the same order)
         */
        final List<T> stakeholderList;

        final Map<T, Float> stakeholderProportion;

        Map<T, List<Float>> previousToLastPeerAssignedSpeeds;

        Map<T, List<Float>> lastPeerAssignedSpeeds;

        final Map<T, List<Float>> lastPeerAchievedSpeeds;

        final Map<T, List<Float>> requestedProportion;

        Input() {
            totalMaxDesiredSpeed = null;
            lastTotalAssignedSpeed = 0f;
            stakeholderList = new ArrayList<>();
            stakeholderProportion = new HashMap<>();
            previousToLastPeerAssignedSpeeds = new HashMap<>();
            lastPeerAssignedSpeeds = new HashMap<>();
            lastPeerAchievedSpeeds = new HashMap<>();
            requestedProportion = new HashMap<>();
        }

        void addResource(T stakeholder, float previousToLastAssignedSpeed, float lastAssignedSpeed, float lastAchievedSpeed, float priority) {
            if (!stakeholderList.contains(stakeholder)) {
                stakeholderList.add(stakeholder);
            }
            if (!stakeholderProportion.containsKey(stakeholder)) {
                // we put a random value here. It will be updated before the next calculation by the generic priority manager
                stakeholderProportion.put(stakeholder, 1f);
            }
            if (!previousToLastPeerAssignedSpeeds.containsKey(stakeholder)) {
                previousToLastPeerAssignedSpeeds.put(stakeholder, new ArrayList<Float>());
            }
            previousToLastPeerAssignedSpeeds.get(stakeholder).add(previousToLastAssignedSpeed);
            if (!lastPeerAssignedSpeeds.containsKey(stakeholder)) {
                lastPeerAssignedSpeeds.put(stakeholder, new ArrayList<Float>());
            }
            lastPeerAssignedSpeeds.get(stakeholder).add(lastAssignedSpeed);
            if (!lastPeerAchievedSpeeds.containsKey(stakeholder)) {
                lastPeerAchievedSpeeds.put(stakeholder, new ArrayList<Float>());
            }
            lastPeerAchievedSpeeds.get(stakeholder).add(lastAchievedSpeed);
            if (!requestedProportion.containsKey(stakeholder)) {
                requestedProportion.put(stakeholder, new ArrayList<Float>());
            }
            requestedProportion.get(stakeholder).add(priority);
        }

        void removeResource(T resource, int position) {
            previousToLastPeerAssignedSpeeds.get(resource).remove(position);
            if (previousToLastPeerAssignedSpeeds.get(resource).size() == 0) {
                previousToLastPeerAssignedSpeeds.remove(resource);
                // also remove peer from peer list and from totalMaxDesiredPeerSpeed
                stakeholderList.remove(resource);
                stakeholderProportion.remove(resource);
            }
            lastPeerAssignedSpeeds.get(resource).remove(position);
            if (lastPeerAssignedSpeeds.get(resource).size() == 0) {
                lastPeerAssignedSpeeds.remove(resource);
            }
            lastPeerAchievedSpeeds.get(resource).remove(position);
            if (lastPeerAchievedSpeeds.get(resource).size() == 0) {
                lastPeerAchievedSpeeds.remove(resource);
            }
            requestedProportion.get(resource).remove(position);
            if (requestedProportion.get(resource).size() == 0) {
                requestedProportion.remove(resource);
            }
        }

        @Override
        public String toString() {
            return "Input{" +
                    "totalMaxDesiredSpeed=" + totalMaxDesiredSpeed +
                    ", lastTotalAssignedSpeed=" + lastTotalAssignedSpeed +
                    ", stakeholderList=" + stakeholderList +
                    ", stakeholderProportion=" + stakeholderProportion +
                    ", previousToLastPeerAssignedSpeeds=" + previousToLastPeerAssignedSpeeds +
                    ", lastPeerAssignedSpeeds=" + lastPeerAssignedSpeeds +
                    ", lastPeerAchievedSpeeds=" + lastPeerAchievedSpeeds +
                    ", requestedProportion=" + requestedProportion +
                    '}';
        }
    }

    static class Result<T> {

        float totalAssignedSpeed;

        List<Float> totalAssignedPeerSpeed;

        Map<T, List<Float>> assignedSpeeds;

        Float variation;

        Result(List<Float> totalAssignedPeerSpeed, Map<T, List<Float>> assignedSpeeds, Float variation) {
            totalAssignedSpeed = 0;
            for (Float speed : totalAssignedPeerSpeed) {
                totalAssignedSpeed += speed;
            }
            this.totalAssignedPeerSpeed = totalAssignedPeerSpeed;
            this.assignedSpeeds = assignedSpeeds;
            this.variation = variation;
        }

        @Override
        public String toString() {
            return "Upload speed calculation result\n" +
                    "-------------------------------\n" +
                    "totalAssignedSpeed: " + totalAssignedSpeed + "\n" +
                    "assignedPeerSpeeds: " + totalAssignedPeerSpeed + "\n" +
                    "assignedSpeeds: " + assignedSpeeds + "\n" +
                    "variation: " + variation + "\n" +
                    "-------------------------------\n";
        }
    }

    private static final float THRESHOLD = 0.95f;

    private static final float LOWERING_PERCENTAGE = 0.3f;

    /**
     * Distributes speeds among peers and their individual transfers
     *
     * @param input input parameters
     * @return a Result object containing the calculated speed distributions
     */
    @SuppressWarnings({"unchecked"})
    static <T> Result<T> calculateSpeeds(Input<T> input) {
        // first we deliver speeds for each peer. Then we deliver in each peer transfers
        // prior to any calculations, we calculate cumulative speeds for each peer
        Four_Tuple<List<Float>, List<Float>, List<Float>, List<Float>> cumulativeLists = calculateCumulativeSpeeds(input);
        List<Float> previousToLastCumulativeAssignedSpeeds = cumulativeLists.element1;
        List<Float> lastCumulativeAssignedSpeeds = cumulativeLists.element2;
        List<Float> lastCumulativeAchievedSpeeds = cumulativeLists.element3;
        List<Float> stakeholderProportion = cumulativeLists.element4;

        Duple<List<Float>, Float> totalSpeedResult = totalPeerSpeedCalculation(input, previousToLastCumulativeAssignedSpeeds, lastCumulativeAssignedSpeeds, lastCumulativeAchievedSpeeds, stakeholderProportion);
        List<Float> totalAssignedPeerSpeed = totalSpeedResult.element1;
        Float variation = totalSpeedResult.element2;

        Map<T, List<Float>> assignedSpeeds = new HashMap<>();
        for (int i = 0; i < input.stakeholderList.size(); i++) {
            T stakeholder = input.stakeholderList.get(i);
            List<Float> speeds = individualTransfersSpeedCalculation(
                    totalAssignedPeerSpeed.get(i),
                    input.previousToLastPeerAssignedSpeeds.get(stakeholder),
                    input.lastPeerAssignedSpeeds.get(stakeholder),
                    input.lastPeerAchievedSpeeds.get(stakeholder),
                    input.requestedProportion.get(stakeholder));
            assignedSpeeds.put(stakeholder, speeds);
        }
        return new Result(totalAssignedPeerSpeed, assignedSpeeds, variation);
    }

    /**
     * Calculates the cumulative assignations (previousToLast and last) and achieved speeds for each peer
     *
     * @param input input parameters
     * @return a list containing the cumulative previous to last assigned speeds, last assigned
     *         speeds and last achieved speeds
     */
    private static <T> Four_Tuple<List<Float>, List<Float>, List<Float>, List<Float>> calculateCumulativeSpeeds(Input<T> input) {
        List<Float> previousToLastCumulativeAssignedSpeeds = new ArrayList<>(input.stakeholderList.size());
        List<Float> lastCumulativeAssignedSpeeds = new ArrayList<>(input.stakeholderList.size());
        List<Float> lastCumulativeAchievedSpeeds = new ArrayList<>(input.stakeholderList.size());
//        List<Float> totalMaxDesiredPeerSpeed = new ArrayList<Float>(input.totalMaxDesiredPeerSpeed.size());
        List<Float> stakeholderProportion = new ArrayList<>(input.stakeholderList.size());
        for (T stakeholder : input.stakeholderList) {
            float value = 0f;
            for (Float aValue : input.previousToLastPeerAssignedSpeeds.get(stakeholder)) {
                value += aValue;
            }
            previousToLastCumulativeAssignedSpeeds.add(value);
            value = 0f;
            for (Float aValue : input.lastPeerAssignedSpeeds.get(stakeholder)) {
                value += aValue;
            }
            lastCumulativeAssignedSpeeds.add(value);
            value = 0f;
            for (Float aValue : input.lastPeerAchievedSpeeds.get(stakeholder)) {
                value += aValue;
            }
            lastCumulativeAchievedSpeeds.add(value);
            stakeholderProportion.add(input.stakeholderProportion.get(stakeholder));
        }
        return new Four_Tuple<>(previousToLastCumulativeAssignedSpeeds, lastCumulativeAssignedSpeeds, lastCumulativeAchievedSpeeds, stakeholderProportion);
    }

    private static Duple<List<Float>, Float> totalPeerSpeedCalculation(
            Input input,
            List<Float> previousToLastCumulativeAssignedSpeeds,
            List<Float> lastCumulativeAssignedSpeeds,
            List<Float> lastCumulativeAchievedSpeeds,
            List<Float> stakeholderProportion) {
        // the total assigned speed is distributed between all the peers. We delegate in the AdaptiveResourceDistributor
        // class for this calculation. We provide a null for the wished values, since we want equitable distribution

        // generate a totalMaxDesiredPeerSpeed with nulls, because we don't want to limit individual stakeholders
        List<Float> totalMaxDesiredPeerSpeed = new ArrayList<>();
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < stakeholderProportion.size(); i++) {
            totalMaxDesiredPeerSpeed.add(null);
        }
        AdaptiveResourceDistributor2.Result adaptiveResult =
                AdaptiveResourceDistributor2.distributeResources(
                        input.totalMaxDesiredSpeed,
                        previousToLastCumulativeAssignedSpeeds,
                        lastCumulativeAssignedSpeeds,
                        lastCumulativeAchievedSpeeds,
                        stakeholderProportion,
                        totalMaxDesiredPeerSpeed,
                        THRESHOLD,
                        LOWERING_PERCENTAGE);
        return new Duple<>(adaptiveResult.assignedResources, adaptiveResult.variation);
    }

    private static List<Float> individualTransfersSpeedCalculation(Float maxDesiredSpeed, List<Float> previousToLastAssignedSpeeds, List<Float> lastAssignedSpeeds, List<Float> lastAchievedSpeeds, List<Float> requestedProportion) {
        // initialize a vector of desired speeds for each transfer. We use the value of the max total speed, since
        // all transfers want to get as much as possible
        List<Float> maxDesiredSpeedList = new ArrayList<>(lastAchievedSpeeds.size());
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < lastAchievedSpeeds.size(); i++) {
            maxDesiredSpeedList.add(maxDesiredSpeed);
        }
        AdaptiveResourceDistributor2.Result adaptiveResult =
                AdaptiveResourceDistributor2.distributeResources(
                        maxDesiredSpeed,
                        previousToLastAssignedSpeeds,
                        lastAssignedSpeeds,
                        lastAchievedSpeeds,
                        requestedProportion,
                        maxDesiredSpeedList,
                        THRESHOLD,
                        LOWERING_PERCENTAGE);
        return adaptiveResult.assignedResources;
    }
}
