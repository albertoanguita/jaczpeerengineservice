package jacz.peerengine.util.datatransfer;

import jacz.util.concurrency.timer.SimpleTimerAction;
import jacz.util.concurrency.timer.Timer;
import jacz.util.maps.DoubleMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class handles the speed limits to be applied to masters and slaves. It keeps track of all active masters and slaves and
 * handles incoming priority changes requests. It is also in charge of periodically recalculating the download and upload
 * speed limits and transferring the values to the corresponding regulated resources.
 * <p/>
 * Data is maintained in lists rather than maps because that is the expected form by the resource distribution algorithm used. This way we
 * only have to reorganize data when a new transfer is added. These lists are updated before each new assessment.
 *
 * todo avoid case were speeds get stuck in 0. Put a min value in algorithm
 */
public class GenericPriorityManager implements SimpleTimerAction {

    public interface Stakeholder {

        public String getStakeholderId();

        public float getStakeholderPriority();
    }

    public interface RegulatedResource {

        public String getStringId();

        public float getPriority();

        public Float getAchievedSpeed();

        public void setSpeed(float speed);
    }

    /**
     * Class for identifying each slave (id of the requesting peer and id of the resource)
     */
    private static class RegulatedResourceIdentifier {

        private final String stakeholderId;

        private final String regulatedResourceId;

        private RegulatedResourceIdentifier(String stakeholderId, String regulatedResourceId) {
            this.stakeholderId = stakeholderId;
            this.regulatedResourceId = regulatedResourceId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RegulatedResourceIdentifier)) return false;

            RegulatedResourceIdentifier that = (RegulatedResourceIdentifier) o;

            if (!stakeholderId.equals(that.stakeholderId)) return false;
            //noinspection RedundantIfStatement
            if (!regulatedResourceId.equals(that.regulatedResourceId)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = stakeholderId.hashCode();
            result = 31 * result + regulatedResourceId.hashCode();
            return result;
        }
    }

    private static final long MAX_WAIT_TIME = 5000;

    // lower values provoke oscillations that make the speed limit bound instead of steadily increase
    private static final long MIN_WAIT_TIME = 1000;

    /**
     * Collection of active stakeholders
     */
    private final Map<String, Stakeholder> activeStakeholders;

    /**
     * Collection of active slaves, indexed by the slave identifier
     */
    private final Map<RegulatedResourceIdentifier, RegulatedResource> activeResources;

    /**
     * Input parameter for the GenericSpeedCalculator. We create this field because it is easier to always
     * use the same object and modify it before each assessment
     */
    private final GenericSpeedCalculator.Input<String> input;

    /**
     * Currently assigned speeds
     */
    private Map<String, List<Float>> currentAssignedSpeeds;

    private final Map<String, DoubleMap<String, Integer>> order;

    private final Timer timer;


    public GenericPriorityManager() {
        activeStakeholders = new HashMap<>();
        activeResources = new HashMap<>();
        input = new GenericSpeedCalculator.Input<>();
        currentAssignedSpeeds = new HashMap<>();
        order = new HashMap<>();
        timer = new Timer(MIN_WAIT_TIME, this);
    }

    public synchronized Float getTotalMaxDesiredSpeed() {
        return input.totalMaxDesiredSpeed;
    }

    public synchronized void setTotalMaxDesiredSpeed(Float totalMaxDesiredSpeed) {
        input.totalMaxDesiredSpeed = totalMaxDesiredSpeed;
    }

    public synchronized void addRegulatedResource(Stakeholder priorityStakeholder, RegulatedResource regulatedResource, float initialSpeeds) {
        if (!activeStakeholders.containsKey(priorityStakeholder.getStakeholderId())) {
            activeStakeholders.put(priorityStakeholder.getStakeholderId(), priorityStakeholder);
        }
        activeResources.put(new RegulatedResourceIdentifier(priorityStakeholder.getStakeholderId(), regulatedResource.getStringId()), regulatedResource);

        if (!order.containsKey(priorityStakeholder.getStakeholderId())) {
            order.put(priorityStakeholder.getStakeholderId(), new DoubleMap<String, Integer>());
        }
        int resourcePosition = order.get(priorityStakeholder.getStakeholderId()).size();
        order.get(priorityStakeholder.getStakeholderId()).put(regulatedResource.getStringId(), resourcePosition);

        input.addResource(priorityStakeholder.getStakeholderId(), initialSpeeds, initialSpeeds, initialSpeeds, regulatedResource.getPriority());
        if (!currentAssignedSpeeds.containsKey(priorityStakeholder.getStakeholderId())) {
            currentAssignedSpeeds.put(priorityStakeholder.getStakeholderId(), new ArrayList<Float>());
        }
        currentAssignedSpeeds.get(priorityStakeholder.getStakeholderId()).add(initialSpeeds);
    }

    public synchronized void removeRegulatedResource(String priorityStakeholderId, RegulatedResource regulatedResource) {
        RegulatedResourceIdentifier regulatedResourceIdentifier = new RegulatedResourceIdentifier(priorityStakeholderId, regulatedResource.getStringId());
        if (activeResources.containsKey(regulatedResourceIdentifier)) {
            activeResources.remove(regulatedResourceIdentifier);
            DoubleMap<String, Integer> peerOrder = order.get(priorityStakeholderId);
            int resourcePosition = peerOrder.get(regulatedResource.getStringId());
            peerOrder.remove(regulatedResource.getStringId());
            if (peerOrder.isEmpty()) {
                order.remove(priorityStakeholderId);
            }
            // decrease positions that are above this by 1, to in line with the removal of this element
            int i = resourcePosition + 1;
            while (peerOrder.containsReverse(i)) {
                String resourceAtThisPosition = peerOrder.getReverse(i);
                peerOrder.put(resourceAtThisPosition, i - 1);
                i++;
            }
            input.removeResource(priorityStakeholderId, resourcePosition);
            if (!currentAssignedSpeeds.containsKey(priorityStakeholderId)) {
                System.out.println("FAIL!!!");
                System.out.println(priorityStakeholderId);
                System.out.println(currentAssignedSpeeds);
            }
            currentAssignedSpeeds.get(priorityStakeholderId).remove(resourcePosition);
            if (currentAssignedSpeeds.get(priorityStakeholderId).size() == 0) {
                currentAssignedSpeeds.remove(priorityStakeholderId);
            }
        }
        // check if the stakeholder has any remaining regulated resource
        boolean found = false;
        for (RegulatedResourceIdentifier regulatedResourceIdentifier1 : activeResources.keySet()) {
            if (regulatedResourceIdentifier1.stakeholderId.equals(priorityStakeholderId)) {
                found = true;
                break;
            }
        }
        if (!found) {
            activeStakeholders.remove(priorityStakeholderId);
        }
    }


    @Override
    public synchronized Long wakeUp(Timer timer) {
        // priorities are recalculated, and values are transferred to the corresponding slaves
        // this timer never ends, but time until next call is recalculated based on how many changes
        // resulted
        updateInput();
        // gather info from slaves, update the input parameter and assess new assignations for them
        for (Map.Entry<RegulatedResourceIdentifier, RegulatedResource> entry : activeResources.entrySet()) {
            String stakeholderId = entry.getKey().stakeholderId;
            String regulatedResourceId = entry.getKey().regulatedResourceId;
            Integer position = order.get(stakeholderId).get(regulatedResourceId);
            Float lastSpeed = entry.getValue().getAchievedSpeed();
            if (lastSpeed == null) {
                // null speeds are considered zero
                lastSpeed = 0f;
            }
            input.lastPeerAchievedSpeeds.get(stakeholderId).set(position, lastSpeed);
        }
        GenericSpeedCalculator.Result<String> result = GenericSpeedCalculator.calculateSpeeds(input);
        currentAssignedSpeeds = result.assignedSpeeds;

        // feed the peers with the new assignations
        for (Map.Entry<String, List<Float>> entry : currentAssignedSpeeds.entrySet()) {
            String peerID = entry.getKey();
            DoubleMap<String, Integer> peerOrder = order.get(peerID);
            List<Float> assignedSpeeds = entry.getValue();
            for (int i = 0; i < assignedSpeeds.size(); i++) {
                String resourceID = peerOrder.getReverse(i);
                RegulatedResource regulatedResource = activeResources.get(new RegulatedResourceIdentifier(peerID, resourceID));
                regulatedResource.setSpeed(assignedSpeeds.get(i));
            }
        }
        return calculateWaitTime(result.variation);
    }

    private void updateInput() {
        // shift measured speeds and update requested priorities
        input.previousToLastPeerAssignedSpeeds = input.lastPeerAssignedSpeeds;
        input.lastPeerAssignedSpeeds = currentAssignedSpeeds;
        for (Map.Entry<String, Stakeholder> activeStakeholderEntry : activeStakeholders.entrySet()) {
            input.stakeholderProportion.put(activeStakeholderEntry.getKey(), activeStakeholderEntry.getValue().getStakeholderPriority());
        }
        for (Map.Entry<RegulatedResourceIdentifier, RegulatedResource> regulatedResourceEntry : activeResources.entrySet()) {
            String stakeholderId = regulatedResourceEntry.getKey().stakeholderId;
            String resourceId = regulatedResourceEntry.getKey().regulatedResourceId;
            int resourceOrder = order.get(stakeholderId).get(resourceId);
            input.requestedProportion.get(stakeholderId).set(resourceOrder, regulatedResourceEntry.getValue().getPriority());
        }
    }

    private static long calculateWaitTime(float variation) {
        // variation goes from 0 (no variation) to 1 (maximum variation). We apply this value linearly to assess
        // the waiting time until the next assessment (the more variation, the lesser waiting time)
        return MAX_WAIT_TIME - (long) (variation * (float) (MAX_WAIT_TIME - MIN_WAIT_TIME));
    }

    synchronized void stop() {
        timer.kill();
    }
}
