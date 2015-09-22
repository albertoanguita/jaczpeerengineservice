package jacz.peerengineservice.util.datatransfer;

import jacz.util.AI.resource_distribution.PriorityResourceDistribution;
import jacz.util.concurrency.timer.SimpleTimerAction;
import jacz.util.concurrency.timer.Timer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Alberto on 21/09/2015.
 */
public class GenericPriorityManagerNew implements SimpleTimerAction {

//    public static enum Throttle {
//        VERY_LOW,
//        LOW,
//        MEDIUM,
//        HIGH,
//        VERY_HIGH
//    }


    private static final long WAIT_TIME = 2500;

    private final Map<GenericPriorityManagerStakeholder, Set<GenericPriorityManagerRegulatedResource>> resources;

    private Float totalMaxDesiredSpeed;

    private final Timer timer;

    private final boolean regulateOnlyStakeholderLevel;


    public GenericPriorityManagerNew(boolean regulateOnlyStakeholderLevel) {
        resources = new HashMap<>();
        totalMaxDesiredSpeed = null;
        timer = new Timer(WAIT_TIME, this);
        this.regulateOnlyStakeholderLevel = regulateOnlyStakeholderLevel;
    }


    public synchronized void update() {
        // first update variations for the resources of each stakeholder
        for (Map.Entry<GenericPriorityManagerStakeholder, Set<GenericPriorityManagerRegulatedResource>> stakeholderResources : resources.entrySet()) {
            float stakeholderSpeed = PriorityResourceDistribution.distributeResources(stakeholderResources.getValue());
            stakeholderResources.getKey().setTotalSpeed(stakeholderSpeed);
        }

        // update variations of stakeholders
        PriorityResourceDistribution.distributeResources(resources.keySet(), totalMaxDesiredSpeed);

        // transmit variations to resources
        for (Map.Entry<GenericPriorityManagerStakeholder, Set<GenericPriorityManagerRegulatedResource>> stakeholderResources : resources.entrySet()) {
            float stakeholderVariation = stakeholderResources.getKey().getVariation();
            for (GenericPriorityManagerRegulatedResource regulatedResource : stakeholderResources.getValue()) {
                float variation = stakeholderVariation;
                if (!regulateOnlyStakeholderLevel) {
                    variation *= regulatedResource.getVariation();
                }
                System.out.println("UPLOAD VARIATION: " + variation);
                if (variation < 1f) {
                    regulatedResource.throttle(variation);
                }
            }
        }
    }

//    private Throttle calculateThrottle(float variation) {
//        Throttle throttle = null;
//        if (variation < 1f) {
//            throttle
//        }
//    }

    public synchronized Float getTotalMaxDesiredSpeed() {
        return totalMaxDesiredSpeed;
    }

    public synchronized void setTotalMaxDesiredSpeed(Float totalMaxDesiredSpeed) {
        this.totalMaxDesiredSpeed = totalMaxDesiredSpeed;
    }

    public synchronized void addRegulatedResource(GenericPriorityManagerStakeholder stakeholder, GenericPriorityManagerRegulatedResource regulatedResource) {
        if (!resources.containsKey(stakeholder)) {
            resources.put(stakeholder, new HashSet<GenericPriorityManagerRegulatedResource>());
        }
        resources.get(stakeholder).add(regulatedResource);
    }

    public synchronized void removeRegulatedResource(GenericPriorityManagerStakeholder stakeholder, GenericPriorityManagerRegulatedResource regulatedResource) {
        resources.get(stakeholder).remove(regulatedResource);
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
        timer.kill();
    }
}
