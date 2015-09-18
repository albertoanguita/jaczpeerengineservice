package jacz.peerengineservice.util.datatransfer.master;

import jacz.peerengineservice.util.datatransfer.ResourceStreamingManager;
import jacz.peerengineservice.util.datatransfer.slave.ResourceChunk;
import jacz.util.hash.HashFunction;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.io.object_serialization.ObjectListWrapper;
import jacz.util.numeric.ContinuousDegree;
import jacz.util.numeric.LongRange;
import jacz.util.numeric.NumericUtil;
import jacz.util.numeric.RangeSet;
import jacz.util.stochastic.StochasticUtil;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * This class serves the MasterResourceStreamer by telling him which parts of the resource ask to each active slave.
 * <p/>
 * The methods of the ResourcePartScheduler are not thread-safe, so concurrency issues must be dealt with externally.
 */
class ResourcePartScheduler {

    private class AssignedPartHash {

        private final LongRange hashRange;

        private final ResourcePart remainingHash;

        private final byte[] correctHash;

        /**
         * Digested message so far
         */
        private HashFunction hashFunction;

        private AssignedPartHash(LongRange hashRange, byte[] correctHash, String hashAlgorithm) {
            this.hashRange = hashRange;
            remainingHash = new ResourcePart(hashRange);
            this.correctHash = correctHash;
            try {
                hashFunction = new HashFunction(hashAlgorithm);
            } catch (NoSuchAlgorithmException e) {
                masterResourceStreamer.reportInvalidHashAlgorithm(hashRange, hashAlgorithm);
                hashFunction = null;
            }
        }

        private LongRange getHashRange() {
            return hashRange;
        }

        private void digestData(ResourceChunk resourceChunk) {
            if (hashFunction != null) {
                // digest data
                hashFunction.update(resourceChunk.getData());
                remainingHash.remove(resourceChunk.getSegment());
            } else {
                // wrong chunk received, stop digesting
                hashFunction = null;
            }
        }

        private boolean isHashCheckingComplete() {
            return remainingHash.isEmpty();
        }

        private boolean isCorrectHash() {
            return Arrays.equals(correctHash, hashFunction.digest());
        }
    }

    /**
     * Data about each slave that the ResourcePartScheduler handles for its calculations. The data stored here includes
     * the part shared as well as some statistics necessary for the assignation algorithm (last known speed of
     * transmission)
     */
    private class SlaveData {

        /**
         * Identifier for this slave data
         */
        private final UniqueIdentifier id;

        /**
         * Shared segments by this slave (including downloaded or assigned ones)
         */
        private ResourcePart sharedPart;

        /**
         * Shared and not yet downloaded (nor assigned) part. Always a subset of sharedPart
         */
        private ResourcePart usefulPart;

        /**
         * Resource part currently assigned to this slave. Subset of sharedPart but no intersection with usefulPart
         */
        private ResourcePart assignedPart;

        /**
         * Hash of the assignment. The hash is received before any assignment chunk, and is checked after the assignment has been fully received
         * Null means that no hash is set to the currently assigned part
         */
        private AssignedPartHash assignedPartHash;

        SlaveData(UniqueIdentifier id) {
            this(id, new ResourcePart(), new ResourcePart(), new ResourcePart());
        }

        SlaveData(UniqueIdentifier id, ResourcePart sharedPart, ResourcePart usefulPart, ResourcePart assignedPart) {
            this.id = id;
            setSharedPart(sharedPart, usefulPart);
            this.assignedPart = assignedPart;
            assignedPartHash = null;
        }

        /**
         * Sets the share for this slave
         *
         * @param sharedPart part shared
         * @param usefulPart part of the share that is useful
         */
        void setSharedPart(ResourcePart sharedPart, ResourcePart usefulPart) {
            this.sharedPart = sharedPart;
            this.usefulPart = usefulPart;
        }

        /**
         * Sets a segment as assigned. This segment is no longer useful
         *
         * @param assignment assigned segment
         */
        void assignSegment(LongRange assignment) {
            assignedPart.add(assignment);
            usefulPart.remove(assignment);
        }

        /**
         * Clears the assignation of this slave
         */
        void clearAssignation() {
            assignedPart.clear();
            clearSegmentHash();
        }

        void setSegmentHash(LongRange segment, byte[] hash, String hashAlgorithm) {
            assignedPartHash = new AssignedPartHash(segment, hash, hashAlgorithm);
        }

        /**
         * A given range that was assigned has been successfully downloaded -> remove it from assignation.
         *
         * @param resourceChunk downloaded chunk
         */
        void assignedSegmentDownloaded(ResourceChunk resourceChunk) {
            assignedPart.remove(resourceChunk.getSegment());
            if (assignedPartHash != null) {
                assignedPartHash.digestData(resourceChunk);
            }
        }

        /**
         * @return true if this chunk completed a hash checking
         */
        boolean isHashCheckingComplete() {
            return assignedPartHash != null && assignedPartHash.isHashCheckingComplete();
        }

        boolean isCorrectHash() {
            return assignedPartHash != null && assignedPartHash.isCorrectHash();
        }

        LongRange getHashSegment() {
            return assignedPartHash.getHashRange();
        }

        void clearSegmentHash() {
            assignedPartHash = null;
        }

        /**
         * A given range is no longer useful (probably because it was assigned to this or any other slave)
         *
         * @param segment the no longer useful segment
         */
        void segmentIsNoLongerUseful(LongRange segment) {
            usefulPart.remove(segment);
        }

        /**
         * A set of ranges is again useful (because it was cleared from this or any other slave assignation). We must
         * only set to useful the parts that the slave is actually sharing
         *
         * @param part resource part that is useful again
         */
        void partIsAgainUseful(ResourcePart part) {
            ResourcePart usefulPartInThisSlave = sharedPart.intersection(part);
            usefulPart.add(usefulPartInThisSlave.getRanges());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SlaveData slaveData = (SlaveData) o;
            return id.equals(slaveData.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    /**
     * Class that represents candidate blocks for assignations to slaves
     */
    private class CandidateBlock implements Comparable<CandidateBlock> {

        /**
         * Block position of this candidate block
         */
        private final long position;

        /**
         * Score achieved by this candidate block (higher is better)
         */
        private final double score;

        private CandidateBlock(long position, double score) {
            this.position = position;
            this.score = score;
        }

        @Override
        public int compareTo(CandidateBlock otherCandidateBlock) {
            // higher score means before
            if (score > otherCandidateBlock.score) {
                return -1;
            } else if (score < otherCandidateBlock.score) {
                return 1;
            } else {
                return 0;
            }
        }

        @Override
        public String toString() {
            return "Candidate block: " + position + "(@" + score + ")";
        }
    }

    public static enum NoAssignationCause {
        LOW_SPEED,
        SIZE_NOT_KNOWN,
        SLAVE_NOT_FOUND,
        ERROR_IN_ASSESSMENT,
        NO_USEFUL_PARTS
    }

    /**
     * Average time in seconds that we want a slave transferring its assignation. Used to calculate the size
     * of assignations.
     */
    private static final long ESTIMATED_ASSIGNATION_TIME = 100;

    /**
     * slaves under this speed will be disregarded -> no assignation is given to slaves under this value
     */
    private static final double MIN_ALLOWED_SPEED = 4d;

    /**
     * Minimum preferred size in bytes for assignations (4KB). Assigned segments cannot be smaller than this (unless
     * there is no other choice)
     */
    private static final long MINIMUM_PREFERRED_ASSIGNATION_SIZE = 4096l;

    private static final int MOST_ACCURATE_BLOCK_COUNT = 20000;

    private static final int LEAST_ACCURATE_BLOCK_COUNT = 20;

    private static final long LEAST_BLOCK_SIZE = 4096l;


    private static final double MAX_STREAMING_NEED_MODIFICATION = 100.0d;

    private static final double MAX_REGULAR_SHARE_MODIFICATION = 0.1d;

    private static final double SHARE_BELOW_WHICH_AVOID = 0.15d;

    private static final double MAX_LOW_SHARE_MODIFICATION = 0.01d;

    private static final double MAX_NOISE_PROBABILITY = 0.4d;

    private static final double MIN_NOISE_PROBABILITY = 1.0d;

    private static final int MAXIMUM_CANDIDATE_DEPTH = 12;


    /**
     * MasterResourceStreamer owning this scheduler
     */
    private final MasterResourceStreamer masterResourceStreamer;

    /**
     * Resource streaming manager that created this download (for retrieving the accuracy value)
     */
    private final ResourceStreamingManager resourceStreamingManager;

    /**
     * Active slaves that share parts of the resource being downloaded
     */
    private final Map<UniqueIdentifier, SlaveData> activeSlaves;

    /**
     * Size of the resource being downloaded (null means unknown)
     */
    private Long resourceSize;

    /**
     * Part of the resource still not downloaded and unassigned
     */
    private ResourcePart remainingPart;

    /**
     * Part of the resource currently assigned, but not yet downloaded by the slaves
     */
    private final ResourcePart assignedPart;

    /**
     * This degree indicates the need of the client for a streamed download of the resource, i.e. the need for the
     * first segments to be downloaded first. 1.0 indicates the maximum priority for the first segments, while 0.0
     * indicates no higher priority for any segment
     */
    private ContinuousDegree streamingNeed;

    ResourcePartScheduler(MasterResourceStreamer masterResourceStreamer, ResourceStreamingManager resourceStreamingManager, Long resourceSize, RangeSet<LongRange, Long> ownedPart, double streamingNeed) {
        this.masterResourceStreamer = masterResourceStreamer;
        this.resourceStreamingManager = resourceStreamingManager;
        this.resourceSize = resourceSize;
        initializeRemainingPart(ownedPart);
        assignedPart = new ResourcePart();
        activeSlaves = new HashMap<UniqueIdentifier, SlaveData>(0);
        this.streamingNeed = new ContinuousDegree(streamingNeed);
    }

    private void initializeRemainingPart(RangeSet<LongRange, Long> ownedPart) {
        if (sizeIsKnown()) {
            remainingPart = new ResourcePart(new LongRange((long) 0, resourceSize - 1));
            remainingPart.remove(ownedPart);
        } else {
            remainingPart = null;
        }
    }

    private synchronized boolean sizeIsKnown() {
        return resourceSize != null;
    }

    synchronized void reportResourceSize(long size) {
        if (!sizeIsKnown()) {
            resourceSize = size;
            remainingPart = new ResourcePart(new LongRange((long) 0, resourceSize - 1));
        }
    }

    synchronized void reportSegmentHash(SlaveController slaveController, byte[] correctHash, String hashAlgorithm, LongRange hashSegment) {
        if (checkSizeIsKnown()) {
            if (activeSlaves.containsKey(slaveController.getId())) {
                SlaveData slaveData = activeSlaves.get(slaveController.getId());
                slaveData.setSegmentHash(hashSegment, correctHash, hashAlgorithm);
            }
        }
    }

    synchronized void reportDownloadedSegment(SlaveController slaveController, ResourceChunk resourceChunk) {
        LongRange downloadedSegment = resourceChunk.getSegment();
        if (checkSizeIsKnown()) {
            if (activeSlaves.containsKey(slaveController.getId())) {
                assignedPart.remove(downloadedSegment);
                masterResourceStreamer.reportDownloadedSegment(slaveController.getResourceProvider(), downloadedSegment);
                SlaveData slaveData = activeSlaves.get(slaveController.getId());
                slaveData.assignedSegmentDownloaded(resourceChunk);
                if (slaveData.isHashCheckingComplete()) {
                    // hash segment completed -> check correct hash
                    if (slaveData.isCorrectHash()) {
                        // the hash matches, everything is ok
                        masterResourceStreamer.reportCorrectIntermediateHash(slaveData.getHashSegment());
                    } else {
                        // incorrect hash! discard the hash segment (put it as remaining part again)
                        masterResourceStreamer.reportFailedIntermediateHash(slaveData.getHashSegment());
                        remainingPart.add(slaveData.getHashSegment());
                    }
                    slaveData.clearSegmentHash();
                }
                if (downloadIsComplete()) {
                    // no need to parallelize these MasterResourceStreamer calls because the master resource streamer is owning always this thread
                    masterResourceStreamer.reportDownloadComplete();
                }
            }
        }
    }

    synchronized ResourcePart getAssignedPart(SlaveController slaveController) {
        if (checkSizeIsKnown()) {
            if (activeSlaves.containsKey(slaveController.getId())) {
                return activeSlaves.get(slaveController.getId()).assignedPart;
            }
        }
        return null;
    }

    private boolean downloadIsComplete() {
        return remainingPart != null && remainingPart.isEmpty() && assignedPart.isEmpty();
    }

    private boolean checkSizeIsKnown() {
        return sizeIsKnown();
    }

    synchronized void addSlave(SlaveController slaveController) {
        activeSlaves.put(slaveController.getId(), new SlaveData(slaveController.getId()));
        masterResourceStreamer.reportAddedProvider(slaveController.getResourceProvider());
    }

    synchronized void setSlaveShare(SlaveController slaveController, ResourcePart sharedPart) {
        if (checkSizeIsKnown()) {
            if (activeSlaves.containsKey(slaveController.getId())) {
                ResourcePart usefulPart = sharedPart.intersection(remainingPart);
                masterResourceStreamer.reportSetProviderShare(slaveController.getResourceProvider(), sharedPart);
                activeSlaves.get(slaveController.getId()).setSharedPart(sharedPart, usefulPart);
            }
        }
    }

    synchronized void removeSlave(SlaveController slaveController) {
        if (activeSlaves.containsKey(slaveController.getId())) {
            if (sizeIsKnown()) {
                // transfer its assignment to the remaining part (if size is unknown, there is no assignment for
                // this slave for sure)
                removeCurrentAssignment(slaveController, false);
            }
            activeSlaves.remove(slaveController.getId());
            // if the download is complete, do not notify this provider removal, since such notification pretends
            // to alert about unexpected provider removals
            if (!downloadIsComplete()) {
                masterResourceStreamer.reportRemovedProvider(slaveController.getResourceProvider());
            }
        }
    }

    synchronized void removeCurrentAssignment(SlaveController slaveController) {
        removeCurrentAssignment(slaveController, true);
    }

    private void removeCurrentAssignment(SlaveController slaveController, boolean notifyDownloadManager) {
        if (sizeIsKnown()) {
            if (activeSlaves.containsKey(slaveController.getId())) {
                SlaveData slaveData = activeSlaves.get(slaveController.getId());
                remainingPart.add(slaveData.assignedPart.getRanges());
                assignedPart.remove(slaveData.assignedPart.getRanges());
                for (SlaveData anotherSlaveData : activeSlaves.values()) {
                    anotherSlaveData.partIsAgainUseful(slaveData.assignedPart);
                }
                slaveData.clearAssignation();
                if (notifyDownloadManager) {
                    masterResourceStreamer.reportClearedProviderAssignation(slaveController.getResourceProvider());
                }
            }
        }
    }

    private void addAssignmentToSlave(UniqueIdentifier slaveID, LongRange assignedSegment) {
        if (checkSizeIsKnown()) {
            if (activeSlaves.containsKey(slaveID)) {
                remainingPart.remove(assignedSegment);
                assignedPart.add(assignedSegment);
                activeSlaves.get(slaveID).assignSegment(assignedSegment);
            }
        }
    }

    synchronized double getStreamingNeed() {
        return streamingNeed.getValue();
    }

    synchronized void setStreamingNeed(double streamingNeed) {
        this.streamingNeed = new ContinuousDegree(streamingNeed);
    }

    /**
     * Assigns a segment to a given slave
     *
     * @param slaveController the slave controller requesting new assignation
     * @param averageSpeed    last known average speed of this slave. This value is used to calculate the size of the
     *                        assignment (the faster, the bigger assignment)
     * @return if everything went ok, an ObjectListWrapper containing the assigned segment and the allowed speed
     *         range. If there was any issue, and ObjectListWrapper containing the cause of the issue
     *         (a NoAssignationCause value)
     */
    synchronized ObjectListWrapper requestAssignation(SlaveController slaveController, Double averageSpeed) {
        // we have to find the most adequate segment to be assigned to the given slave. There are a series of
        // parameters to take into account in this decision. The idea is to check in the available segments of this
        // slave which is more beneficial for the download. The download is beneficial if we obtain as soon as possible
        // the segments that are more urgent to us (the initial ones if we want to stream) but also favour that
        // every slave will provide as much data as possible. It is always good to assign to a slave a segment which
        // other slaves do not have. It is also good to avoid to assign a segment if it forms part of the share of a
        // slave and this slave's share is very small. It is also good to introduce some degree of randomness, so
        // not all peers download the same segments...

        // no segments are assigned until we know the total size of the resource
        if (checkSizeIsKnown()) {
            UniqueIdentifier slaveID = slaveController.getId();
            if (activeSlaves.containsKey(slaveID)) {
                // this slave is too slow -> do not assign anything and remove previous assignation
                if (slaveTooSlow(activeSlaves.get(slaveID), averageSpeed)) {
                    return new ObjectListWrapper(NoAssignationCause.LOW_SPEED);
                }

                long preferredSize = 0;
                if (averageSpeed != null) {
                    preferredSize = averageSpeed.longValue() * ESTIMATED_ASSIGNATION_TIME;
                }
                preferredSize = Math.max(preferredSize, MINIMUM_PREFERRED_ASSIGNATION_SIZE);

                // calculate which segments can this slave be assigned, and the block size for evaluating the segments
                ResourcePart assignableSegments = activeSlaves.get(slaveID).usefulPart;
                long assignablePartSize = assignableSegments.size();

                // this slave does not have any useful parts -> no assignation
                if (assignablePartSize == 0) {
                    return new ObjectListWrapper(NoAssignationCause.NO_USEFUL_PARTS);
                }

                Long selectedPosition = null;
                if (streamingNeed.isMax()) {
                    // for maximum streaming need we skip all calculations and simply get the first block
                    selectedPosition = assignableSegments.getPosition(0);
                } else {
                    // in the other case, evaluate all candidates and select the best
                    int blockCount = calculateBlockCount(resourceStreamingManager.getAccuracy(), assignablePartSize);

                    // prepare the active slaves for proper evaluation. Use only those that are not sharing the complete
                    // resource. We keep count of how many slaves are sharing all the resource (speed up calculations)
                    // and which share just part of the resource (in these, we distinguish from those with little share
                    // and those with more than little share (false -> big share, true -> low share)
                    Map<SlaveData, Boolean> slavesNotSharingAll = new HashMap<SlaveData, Boolean>(0);
                    int slaveSharingAllCount = 0;
                    int totalSlaveCount = activeSlaves.size() - 1;
                    for (UniqueIdentifier aSlaveID : activeSlaves.keySet()) {
                        // skip the slave of this assignation, only count the rest
                        if (aSlaveID.equals(slaveID)) {
                            continue;
                        }
                        SlaveData aSlaveData = activeSlaves.get(aSlaveID);
                        if (aSlaveData.sharedPart.getRanges().size() == 1 && aSlaveData.sharedPart.size() == resourceSize) {
                            slaveSharingAllCount++;
                        } else {
                            // here we store all slaves that don't share all the resource. We also mark if their useful share
                            // is below a certain value, for subsequent calculations
                            if ((double) aSlaveData.usefulPart.size() <= SHARE_BELOW_WHICH_AVOID * (double) remainingPart.size()) {
                                slavesNotSharingAll.put(aSlaveData, true);
                            } else {
                                slavesNotSharingAll.put(aSlaveData, false);
                            }
                        }
                    }
                    // initial score: all block candidates start with the same score (higher -> better)
                    // successive modifiers will act proportionally on the score value
                    double initialScore = 1.0d;

                    // this queue stores the candidates blocks, ordering them from higher to lower score
                    PriorityQueue<CandidateBlock> blockCandidates = new PriorityQueue<CandidateBlock>();

                    for (int i = 0; i < blockCount; i++) {
                        // part of the total assignable part that this block covers (in terms of fraction, e.g. 3/10 ->
                        // 4th of ten blocks)
                        double fraction = (double) i / (double) blockCount;
                        // offset byte in the assignable part of the slave for this block (approximate)
                        long offset = (long) (((double) assignablePartSize) * fraction);
                        // the absolute position in the total resource for this offset
                        long position = assignableSegments.getPosition(offset);

                        // now this position must be evaluated
                        double score = applyStreamingModifier(initialScore, fraction, streamingNeed);

                        int slaveShareCountHighShare = slaveSharingAllCount;
                        int slaveShareCountLowShare = 0;
                        for (SlaveData aSlaveData : slavesNotSharingAll.keySet()) {
                            if (aSlaveData.sharedPart.contains(position)) {
                                if (slavesNotSharingAll.get(aSlaveData)) {
                                    slaveShareCountLowShare++;
                                } else {
                                    slaveShareCountHighShare++;
                                }
                            }
                        }
                        score = penaltyForSlaveSharing(score, totalSlaveCount, slaveShareCountHighShare, slaveShareCountLowShare);
                        blockCandidates.add(new CandidateBlock(position, score));
                    }
                    selectedPosition = selectBlock(blockCandidates, streamingNeed);
                }
                if (selectedPosition != null) {
                    LongRange assignedSegment = assignableSegments.getSegmentAroundPosition(selectedPosition, preferredSize);
                    if (assignedSegment != null) {
                        long minSpeed = 0;
                        if (averageSpeed != null) {
                            minSpeed = averageSpeed.longValue() / 2;
                        }
                        LongRange allowedSpeedRange = new LongRange(minSpeed, null);
                        masterResourceStreamer.reportAssignedProviderSegment(slaveController.getResourceProvider(), assignedSegment);
                        updateAttributesDueToNewAssignment(slaveID, assignedSegment);
                        return new ObjectListWrapper(assignedSegment, allowedSpeedRange);
                    } else {
                        return new ObjectListWrapper(NoAssignationCause.ERROR_IN_ASSESSMENT);
                    }
                } else {
                    return new ObjectListWrapper(NoAssignationCause.ERROR_IN_ASSESSMENT);
                }
            } else {
                return new ObjectListWrapper(NoAssignationCause.SLAVE_NOT_FOUND);
            }
        } else {
            return new ObjectListWrapper(NoAssignationCause.SIZE_NOT_KNOWN);
        }
    }

    private void updateAttributesDueToNewAssignment(UniqueIdentifier slaveID, LongRange assignedSegment) {
        addAssignmentToSlave(slaveID, assignedSegment);
        // this segment is no longer useful for all other slaves
        for (SlaveData slaveData : activeSlaves.values()) {
            slaveData.segmentIsNoLongerUseful(assignedSegment);
        }
    }

    private boolean slaveTooSlow(SlaveData slaveData, Double averageSpeed) {
        // the slave is too slow when he has a non-zero assignment (so he should be transferring now) and his average speed is below a min allowed
        return !slaveData.assignedPart.isEmpty() && averageSpeed != null && averageSpeed < MIN_ALLOWED_SPEED;
    }

    private static int calculateBlockCount(double accuracy, long assignablePartSize) {
        // the block count runs linearly from the minimum to the maximum, depending on the value of accuracy
        // block size cannot be smaller that LEAST_BLOCK_SIZE. Resize them if needed
        // Finally, block count cannot be zero (at least, 1)
        int blockCount = LEAST_ACCURATE_BLOCK_COUNT + (int) (accuracy * (double) (MOST_ACCURATE_BLOCK_COUNT - LEAST_ACCURATE_BLOCK_COUNT));
        long blockSize = assignablePartSize / (long) blockCount;
        if (blockSize < LEAST_BLOCK_SIZE) {
            blockCount = (int) (assignablePartSize / LEAST_BLOCK_SIZE);
        }
        if (blockCount < 1) {
            blockCount = 1;
        }
        return blockCount;
    }

    private static double applyStreamingModifier(double score, double fraction, ContinuousDegree streamingNeed) {
        // zero streaming need makes no changes in the score
        // 1.0 streaming need multiplies by MAX_STREAMING_NEED_MODIFICATION the score for the leftest block
        // the rightest block is never affected
        double modifier = NumericUtil.linearTranslation(fraction, 0d, 1d, MAX_STREAMING_NEED_MODIFICATION, 1d);
        modifier = NumericUtil.linearTranslation(streamingNeed.getValue(), 0d, 1d, 1d, modifier);
        return score * modifier;
    }

    private static double penaltyForSlaveSharing(double score, int totalSlaveCount, int slaveShareCountHighShare, int slaveShareCountLowShare) {
        // low share slaves penalize more than high share slaves. First evaluate the maximum penalty from the
        // ratio of low share slaves vs high share slaves
        // then take into account the total number of slaves
        if (slaveShareCountLowShare + slaveShareCountHighShare == 0 || totalSlaveCount == 0) {
            // if no slaves sharing, or no other slave, do not modify the score (subsequent calculations would
            // produce a div zero error)
            return score;
        }
        double lowShareVSAllShareRatio = (double) slaveShareCountLowShare / ((double) slaveShareCountLowShare + (double) slaveShareCountHighShare);
        double modifier = NumericUtil.linearTranslation(lowShareVSAllShareRatio, 0d, 1d, MAX_REGULAR_SHARE_MODIFICATION, MAX_LOW_SHARE_MODIFICATION);
        double shareVSTotalRatio = ((double) slaveShareCountLowShare + (double) slaveShareCountHighShare) / (double) totalSlaveCount;
        modifier = NumericUtil.linearTranslation(shareVSTotalRatio, 0d, 1d, 1d, modifier);
        return score * modifier;
    }

    private static Long selectBlock(PriorityQueue<CandidateBlock> blockCandidates, ContinuousDegree streamingNeed) {
        // the selected block is the one at the peek position of the candidate queue
        // however, noise can affect this selection. It can provoke that the first candidates are discarded
        // noise affects depending on streamingNeed. For 0 streamingNeed, the noise is maximum, for 1 streamingNeed the noise is minimum
        // maximum noise implies that each candidate has almost half chances (40%) of being selected rather than the candidates behind it
        // minimum noise means that the first candidate has 100% chances of being selected
        // other values modify this chance linearly
        CandidateBlock selectedCandidateBlock = null;
        if (streamingNeed.isMax()) {
            selectedCandidateBlock = blockCandidates.peek();
        } else {
            double chanceForFirstCandidate = NumericUtil.linearTranslation(streamingNeed.getValue(), streamingNeed.getMin(), streamingNeed.getMax(), MAX_NOISE_PROBABILITY, MIN_NOISE_PROBABILITY);
            int count = 0;
            while (count < MAXIMUM_CANDIDATE_DEPTH && !blockCandidates.isEmpty()) {
                if (StochasticUtil.random(0d, 1d) <= chanceForFirstCandidate) {
                    // peek candidate has been selected
                    selectedCandidateBlock = blockCandidates.peek();
                    break;
                } else {
                    // peek candidate was not lucky (store it in case the queue gets empty or we reach the maximum depth)
                    selectedCandidateBlock = blockCandidates.remove();
                }
                count++;
            }
        }
        if (selectedCandidateBlock != null) {
            return selectedCandidateBlock.position;
        } else {
            return null;
        }
    }
}
