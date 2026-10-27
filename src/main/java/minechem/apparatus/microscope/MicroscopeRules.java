package minechem.apparatus.microscope;

/**
 * Rules of the microscope: eleven slots, the input (slot 0, any item), the journal (slot 1, journals only), and nine
 * output slots (2 to 10) that refuse every item. Inspecting an item that has a recipe adds it to the journal. Stacks are
 * limited to 1.
 */
public final class MicroscopeRules {
    /** The number of slots: 11. */
    public static final int SLOTS = 11;
    /** The item under the microscope. */
    public static final int INPUT_SLOT = 0;
    /** The journal that receives the inspected items. */
    public static final int JOURNAL_SLOT = 1;
    /** The output grid, a 3 by 3 block that accepts nothing. */
    public static final int FIRST_OUTPUT_SLOT = 2;
    public static final int OUTPUT_SLOTS = 9;
    /** The stack limit of the microscope's slots. */
    public static final int STACK_LIMIT = 1;

    private MicroscopeRules() {
    }

    /** The input takes anything, the journal slot takes a journal, the outputs take nothing. */
    public static boolean canPlace(int slot, boolean isJournal) {
        if (slot == INPUT_SLOT) {
            return true;
        }
        return slot == JOURNAL_SLOT && isJournal;
    }

    /**
     * An item goes into the journal only when a journal is in its slot and the item has a synthesis or a
     * decomposer recipe.
     */
    public static boolean inspectionAddsToJournal(boolean journalPresent, boolean hasRecipe) {
        return journalPresent && hasRecipe;
    }

    /**
     * The whole stack is taken, whatever amount was asked for: the requested amount is ignored.
     */
    public static int taken(int requested, int stackSize) {
        return stackSize;
    }
}
