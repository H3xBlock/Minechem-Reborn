package minechem.radiation;

/**
 * A container that reduces the radiation damage done while it is open.
 * The factor is the share of the damage it removes (0.4 removes 40 %, 1.0 removes all of it), as returned by
 * radiationReductionFactor. The base damage, the item and the player are not passed: each factor is a constant.
 */
public interface IRadiationShield {
    float radiationReductionFactor();
}
