package jacz.peerengineservice.util.datatransfer;

import org.aanguita.jacuzzi.AI.resource_distribution.PriorityResourceDistribution;

/**
 * We look for specified priorities among stakeholders (if uploads, all same priority)
 *
 * downloads -> one resource downloaded
 * uploads -> peer requesting us some resources
 */
public abstract class GenericPriorityManagerStakeholder extends PriorityResourceDistribution.ResourceData {

    private float totalSpeed;

    public GenericPriorityManagerStakeholder() {
        totalSpeed = 0f;
    }

    public float getConsumption() {
        return totalSpeed;
    }

    public void setTotalSpeed(float totalSpeed) {
        this.totalSpeed = totalSpeed;
    }
}
