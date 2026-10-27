package minechem.apparatus.leadedchest;

/**
 * Rules of the leaded chest: nine slots, and every item stacks to 64 in them, whatever its own limit.
 * Anything may be put in.
 */
public final class LeadedChestSlots {
    /** The inventory has 9 slots. */
    public static final int SLOTS = 9;

    /** The stack limit of the chest's slots. */
    public static final int STACK_LIMIT = 64;

    private LeadedChestSlots() {
    }

    /** The count a slot keeps: a larger stack is cut to the limit. */
    public static int clamp(int count) {
        return Math.min(count, STACK_LIMIT);
    }
}
