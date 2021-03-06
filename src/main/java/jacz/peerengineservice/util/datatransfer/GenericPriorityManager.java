package jacz.peerengineservice.util.datatransfer;

import org.aanguita.jacuzzi.AI.resource_distribution.PriorityResourceDistribution;
import org.aanguita.jacuzzi.concurrency.timer.TimerAction;
import org.aanguita.jacuzzi.concurrency.timer.Timer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Alberto on 21/09/2015.
 *
 * todo check possibility of updating communications to java NIO socketchannel (@FUTURE@)
 * http://tutorials.jenkov.com/java-nio/socketchannel.html
 */
public class GenericPriorityManager implements TimerAction {

    public interface MaxDesiredSpeedInterface {

        Float getMaxDesiredSpeed();
    }

    private static final long WAIT_TIME = 2500;

    private final Map<GenericPriorityManagerStakeholder, Set<GenericPriorityManagerRegulatedResource>> resources;

    private final Timer timer;

    private final MaxDesiredSpeedInterface maxDesiredSpeedInterface;

    private final boolean regulateOnlyStakeholderLevel;


    public GenericPriorityManager(MaxDesiredSpeedInterface maxDesiredSpeedInterface, boolean regulateOnlyStakeholderLevel) {
        resources = new HashMap<>();
        timer = new Timer(WAIT_TIME, this);
        this.maxDesiredSpeedInterface = maxDesiredSpeedInterface;
        this.regulateOnlyStakeholderLevel = regulateOnlyStakeholderLevel;
    }


    public synchronized void update() {
        // first update variations for the resources of each stakeholder
        for (Map.Entry<GenericPriorityManagerStakeholder, Set<GenericPriorityManagerRegulatedResource>> stakeholderResources : resources.entrySet()) {
            float stakeholderSpeed = PriorityResourceDistribution.distributeResources(stakeholderResources.getValue());
            stakeholderResources.getKey().setTotalSpeed(stakeholderSpeed);
        }

        // update variations of stakeholders
        float totalConsumption = PriorityResourceDistribution.distributeResources(resources.keySet(), maxDesiredSpeedInterface.getMaxDesiredSpeed());
        boolean maxSpeedReached = maxDesiredSpeedInterface.getMaxDesiredSpeed() != null && totalConsumption > maxDesiredSpeedInterface.getMaxDesiredSpeed();

        // transmit variations to resources
        for (Map.Entry<GenericPriorityManagerStakeholder, Set<GenericPriorityManagerRegulatedResource>> stakeholderResources : resources.entrySet()) {
            float stakeholderVariation = stakeholderResources.getKey().getVariation();
            for (GenericPriorityManagerRegulatedResource regulatedResource : stakeholderResources.getValue()) {
                float variation = stakeholderVariation;
                if (!regulateOnlyStakeholderLevel) {
                    variation *= regulatedResource.getVariation();
                }
                if (variation < 1f) {
                    if (maxSpeedReached) {
                        regulatedResource.hardThrottle(variation);
                    } else {
                        regulatedResource.softThrottle();
                    }
                }
            }
        }
    }

    public synchronized void addRegulatedResource(GenericPriorityManagerStakeholder stakeholder, GenericPriorityManagerRegulatedResource regulatedResource) {
        if (!resources.containsKey(stakeholder)) {
            resources.put(stakeholder, new HashSet<>());
        }
        resources.get(stakeholder).add(regulatedResource);
    }

    public synchronized void removeRegulatedResource(GenericPriorityManagerStakeholder stakeholder, GenericPriorityManagerRegulatedResource regulatedResource) {
        resources.get(stakeholder).remove(regulatedResource);
        if (resources.get(stakeholder).isEmpty()) {
            resources.remove(stakeholder);
        }
    }


    @Override
    public synchronized Long wakeUp(Timer timer) {
        // priorities are recalculated, and values are transferred to the corresponding slaves
        // this timer never ends, but time until next call is recalculated based on how many changes
        // resulted
        update();
        return null;
    }

    synchronized void stop() {
        timer.stop();
    }
}
