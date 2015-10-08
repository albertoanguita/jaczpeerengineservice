package jacz.peerengineservice.util.datatransfer;

import jacz.util.AI.resource_distribution.PriorityResourceDistribution;

/**
 * We regulate resources to achieve stakeholder equality. We must try to set the specified priorities
 * among resources as well.
 *
 * downloads -> peer that serves us the resource
 * uploads -> one resource served to peer
 */
public abstract class GenericPriorityManagerRegulatedResource extends PriorityResourceDistribution.ResourceData {

    public float getConsumption() {
        Float achievedSpeed = getAchievedSpeed();
        return achievedSpeed != null ? achievedSpeed : 0f;
    }

    public abstract Float getAchievedSpeed();

    public abstract void hardThrottle(float variation);

    public abstract void softThrottle();
}