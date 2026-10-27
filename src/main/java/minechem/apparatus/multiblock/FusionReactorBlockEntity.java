package minechem.apparatus.multiblock;

import com.mojang.serialization.Codec;
import minechem.chemical.Element;
import minechem.helper.Jenkins;
import minechem.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Block entity of the manager of the Chemical Fusion Chamber. Two elements go into the
 * left and right slots; they are fused into the element whose atomic number is their sum, which goes into the output slot.
 *
 * <p>A fusion takes two steps: once the inputs can be fused, the energy for them is stored ({@link #canProcess}),
 * then the energy is used and the inputs are consumed. The energy is {@code (left + right) * FUSION_MULTIPLIER}.
 *
 * <p>The inputs are consumed even when the output holds another element. canProcess is a static field shared by every
 * chamber: one chamber's flag is seen by the others, and each chamber saves the shared value and loads it back.
 */
public class FusionReactorBlockEntity extends ReactorBlockEntity {
    @Override
    public float radiationReductionFactor() {
        return 1.0F;
    }

    /** Energy storage capacity. */
    public static final int ENERGY_CAPACITY = 100000;
    /** The energy per unit of atomic number. */
    public static final int FUSION_MULTIPLIER = 100;

    /** Slot of the left input. */
    public static final int INPUT_LEFT = 0;
    /** Slot of the right input. */
    public static final int INPUT_RIGHT = 1;
    /** Slot of the output. */
    public static final int OUTPUT = 2;
    /** The number of slots; the fourth is not used by the chamber's screen. */
    public static final int SIZE = 4;

    /** Shared by every fusion chamber (see the class comment). */
    private static boolean canProcess;

    public FusionReactorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.FUSION_REACTOR.get(), pos, state, ReactorBlueprint.FUSION, ENERGY_CAPACITY, SIZE);
    }

    /** The atomic number the two inputs fuse into, or 0 when they cannot be fused. */
    public int fusedNumber() {
        Element left = elementOf(getItem(INPUT_LEFT));
        Element right = elementOf(getItem(INPUT_RIGHT));
        if (left == null || right == null) {
            return 0;
        }
        return ReactorRules.fusedOf(left.atomicNumber, right.atomicNumber);
    }

    /** The energy a fusion needs: 0 when the inputs cannot be fused. */
    public int energyNeeded() {
        int fused = fusedNumber();
        return fused > 0 ? fused * FUSION_MULTIPLIER : 0;
    }

    @Override
    protected void tickMachine(Level level) {
        int fused = fusedNumber();
        // The start needs more stored energy than the fusion needs, even when power use is disabled
        // (Config.POWER_USE_ENABLED); only the energy use below is skipped then.
        if (!canProcess && energyNeeded() < getEnergy().getAmountAsInt() && fused > 0 && canOutput(fused)) {
            canProcess = true;
        }
        if (canProcess && fused == 0) {
            // The inputs changed while the fusion was waiting: nothing is fused and the flag is cleared.
            canProcess = false;
        } else if (canProcess && useEnergy(energyNeeded())) {
            fuseInputs(fused);
            removeInputs();
            canProcess = false;
        }
    }

    /** A new element in the output, or one more of the same; another element leaves the output as it is. */
    private void fuseInputs(int fused) {
        ItemStack output = getItem(OUTPUT);
        if (output.isEmpty()) {
            setItem(OUTPUT, elementStack(fused, 1));
        } else if (elementOf(output) != null && elementOf(output).atomicNumber == fused) {
            output.grow(1);
            setChanged();
        } else {
            canProcess = false;
        }
    }

    /** The output is empty, or holds the same element with room for one more. */
    private boolean canOutput(int fused) {
        ItemStack output = getItem(OUTPUT);
        if (output.isEmpty()) {
            return true;
        }
        Element held = elementOf(output);
        return held != null && held.atomicNumber == fused && output.getCount() < STACK_LIMIT;
    }

    private void removeInputs() {
        removeItem(INPUT_LEFT, 1);
        removeItem(INPUT_RIGHT, 1);
    }

    @Override
    protected int[] slotsForFace(Direction side) {
        // side / 2 selects the slots (down and up: the output; north and south: left; west and east: right).
        return switch (side.ordinal() / 2) {
            case 0 -> new int[]{OUTPUT};
            case 1 -> new int[]{INPUT_LEFT};
            case 2 -> new int[]{INPUT_RIGHT};
            default -> new int[0];
        };
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return (slot == INPUT_LEFT || slot == INPUT_RIGHT) && elementOf(stack) != null;
    }

    @Override
    protected void saveMachine(ValueOutput output) {
        output.store("canProcess", Codec.BOOL, canProcess);
    }

    @Override
    protected void loadMachine(ValueInput input) {
        canProcess = input.read("canProcess", Codec.BOOL).orElse(false);
    }
}
