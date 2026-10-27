package minechem.apparatus.multiblock;

import minechem.chemical.Element;
import minechem.helper.Jenkins;
import minechem.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity of the manager of the Chemical Fission Chamber. An element in the input
 * splits into two of the element with half its atomic number; the two go to the output. Each split costs
 * {@code atomicNumber * FISSION_MULTIPLIER} energy.
 *
 * <p>Each split adds 2 to an output that already holds an element (the output may then go
 * past 64, up to 65); the output's element must be the same as the one the split makes.
 */
public class FissionReactorBlockEntity extends ReactorBlockEntity {
    /** Energy storage capacity. */
    public static final int ENERGY_CAPACITY = 100000;
    /** The energy per unit of atomic number. */
    public static final int FISSION_MULTIPLIER = 100;

    /** Slot of the input. */
    public static final int INPUT = 0;
    /** Slot of the output. */
    public static final int OUTPUT = 2;
    /** The number of slots. */
    public static final int SIZE = 3;

    /** The atomic number that slot 1 accepts (only element 91). */
    private static final int SLOT_ONE_ELEMENT = 91;

    public FissionReactorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.FISSION_REACTOR.get(), pos, state, ReactorBlueprint.FISSION, ENERGY_CAPACITY, SIZE);
    }

    /** The energy a split of the input needs. */
    public int energyNeeded() {
        Element element = elementOf(getItem(INPUT));
        return element == null ? 0 : element.atomicNumber * FISSION_MULTIPLIER;
    }

    /**
     * The result of splitting the input: two of the element with half its atomic number, or nothing when the input is not an
     * element, is element 0, or half is not an element.
     */
    public ItemStack fissionResult() {
        Element element = elementOf(getItem(INPUT));
        if (element == null) {
            return ItemStack.EMPTY;
        }
        int half = ReactorRules.halfOf(element.atomicNumber);
        return half > 0 ? elementStack(half, 2) : ItemStack.EMPTY;
    }

    /** Whether the input can split into the output. */
    public boolean inputIsFissionable() {
        ItemStack result = fissionResult();
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = getItem(OUTPUT);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(result, output) && output.getCount() < STACK_LIMIT;
    }

    @Override
    protected void tickMachine(Level level) {
        if (!getItem(INPUT).isEmpty() && inputIsFissionable() && useEnergy(energyNeeded())) {
            addToOutput(fissionResult());
            removeItem(INPUT, 1);
        }
    }

    /** The result is put in an empty output; otherwise two more are added to it. */
    private void addToOutput(ItemStack result) {
        if (result.isEmpty()) {
            return;
        }
        ItemStack output = getItem(OUTPUT);
        if (output.isEmpty()) {
            setItem(OUTPUT, result);
        } else {
            output.grow(2);
            setChanged();
        }
    }

    /** The bottom reaches the output, every other side the input. */
    @Override
    protected int[] slotsForFace(Direction side) {
        return side == Direction.DOWN ? new int[]{OUTPUT} : new int[]{INPUT};
    }

    /** The input takes any element, slot 1 only element 91, and the output nothing. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        Element element = elementOf(stack);
        if (slot == OUTPUT || element == null) {
            return false;
        }
        return slot == INPUT || (slot == 1 && element.atomicNumber == SLOT_ONE_ELEMENT);
    }
}
