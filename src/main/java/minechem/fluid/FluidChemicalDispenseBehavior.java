package minechem.fluid;

import minechem.chemical.ChemicalBase;
import minechem.helper.Jenkins;
import minechem.item.chemical.ChemicalItem;
import minechem.radiation.FluidRadiationData;
import minechem.radiation.RadiationState;
import minechem.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The chemical tubes out of a dispenser. Eight tubes make one unit of fluid.
 *
 * <ul>
 *   <li>An empty tube in front of a fluid source: the eight tubes come from the dispensed stack and the other empty tubes of the
 *       dispenser; the fluid is removed and eight filled tubes go into the dispenser, or drop when there is no room.</li>
 *   <li>A filled tube in front of a block that is not solid: the block becomes the chemical's liquid, eight filled tubes are used
 *       and eight empty tubes come back.</li>
 * </ul>
 *
 * <p>A chemical is one item (see {@link ChemicalItem}) and the empty tube is a separate item, so the two cases are told apart
 * by item. Radioactivity follows the fluid as in {@link ChemicalBucketDispenseBehavior}.
 */
public final class FluidChemicalDispenseBehavior extends DefaultDispenseItemBehavior {
    /** Tubes per unit of fluid. */
    private static final int TUBES_PER_UNIT = 8;
    private static final String WATER = "Water";
    private static final FluidChemicalDispenseBehavior INSTANCE = new FluidChemicalDispenseBehavior();

    private FluidChemicalDispenseBehavior() {
    }

    /** Registers the behaviour for the empty tube and the chemical item; called at common setup. */
    public static void register() {
        DispenserBlock.registerBehavior(ItemRegistry.EMPTY_TUBE.get(), INSTANCE);
        DispenserBlock.registerBehavior(ItemRegistry.CHEMICAL.get(), INSTANCE);
    }

    @Override
    protected ItemStack execute(BlockSource source, ItemStack stack) {
        ServerLevel level = source.level();
        Direction facing = source.state().getValue(DispenserBlock.FACING);
        BlockPos front = source.pos().relative(facing);
        Container container = source.blockEntity();
        if (stack.is(ItemRegistry.EMPTY_TUBE.get())) {
            return drain(level, front, facing, container, source, stack);
        }
        if (stack.is(ItemRegistry.CHEMICAL.get())) {
            return fill(level, front, facing, container, source, stack);
        }
        return stack;
    }

    private ItemStack drain(ServerLevel level, BlockPos front, Direction facing, Container container, BlockSource source, ItemStack stack) {
        if (!level.getFluidState(front).isSource()) {
            return stack;
        }
        ChemicalBase chemical = chemicalOf(level.getBlockState(front).getBlock());
        if (chemical == null || countOthers(container, stack, ItemRegistry.EMPTY_TUBE.get()) + stack.getCount() < TUBES_PER_UNIT) {
            return stack;
        }
        ItemStack remainder = takeTubes(container, stack, ItemRegistry.EMPTY_TUBE.get(), TUBES_PER_UNIT);
        ItemStack filled = ChemicalItem.stackFor(chemical);
        filled.setCount(TUBES_PER_UNIT);
        RadiationState radiation = FluidRadiationData.get(level).take(front);
        if (radiation != null) {
            filled.set(ItemRegistry.RADIATION.get(), radiation);
        }
        level.removeBlock(front, false);
        spill(level, source, facing, insertOthers(container, stack, filled));
        return remainder;
    }

    private ItemStack fill(ServerLevel level, BlockPos front, Direction facing, Container container, BlockSource source, ItemStack stack) {
        BlockState state = level.getBlockState(front);
        ChemicalBase chemical = ChemicalItem.getChemicalBase(stack);
        Block liquid = liquidOf(chemical);
        if (liquid == null || (!state.isAir() && state.isSolid())) {
            return stack;
        }
        if (countOthers(container, stack, ItemRegistry.CHEMICAL.get(), chemical) + stack.getCount() < TUBES_PER_UNIT) {
            return stack;
        }
        ItemStack remainder = takeTubes(container, stack, ItemRegistry.CHEMICAL.get(), chemical, TUBES_PER_UNIT);
        if (!state.isAir()) {
            level.destroyBlock(front, true);
        }
        level.setBlock(front, liquid.defaultBlockState(), Block.UPDATE_ALL);
        RadiationState radiation = stack.get(ItemRegistry.RADIATION.get());
        if (radiation != null) {
            FluidRadiationData.get(level).put(front, radiation);
        }
        ItemStack empties = new ItemStack(ItemRegistry.EMPTY_TUBE.get(), TUBES_PER_UNIT);
        spill(level, source, facing, insertOthers(container, stack, empties));
        return remainder;
    }

    /** The number of matching tubes in the dispenser, not counting the dispensed stack itself (found by identity). */
    private static int countOthers(Container container, ItemStack dispensed, net.minecraft.world.item.Item item) {
        return countOthers(container, dispensed, item, null);
    }

    private static int countOthers(Container container, ItemStack dispensed, net.minecraft.world.item.Item item, ChemicalBase chemical) {
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack other = container.getItem(slot);
            if (other != dispensed && matches(other, item, chemical)) {
                count += other.getCount();
            }
        }
        return count;
    }

    /** Takes {@code needed} tubes from the dispensed stack first, then from the other matching stacks; returns what is left of it. */
    private static ItemStack takeTubes(Container container, ItemStack dispensed, net.minecraft.world.item.Item item, int needed) {
        return takeTubes(container, dispensed, item, null, needed);
    }

    private static ItemStack takeTubes(Container container, ItemStack dispensed, net.minecraft.world.item.Item item, ChemicalBase chemical, int needed) {
        ItemStack remainder = dispensed.copy();
        int left = needed - Math.min(remainder.getCount(), needed);
        remainder.shrink(needed - left);
        for (int slot = 0; slot < container.getContainerSize() && left > 0; slot++) {
            ItemStack other = container.getItem(slot);
            if (other != dispensed && matches(other, item, chemical)) {
                int taken = Math.min(left, other.getCount());
                other.shrink(taken);
                left -= taken;
            }
        }
        return remainder;
    }

    /** Puts stacks into the dispenser's other slots, returning what did not fit. */
    private static ItemStack insertOthers(Container container, ItemStack dispensed, ItemStack incoming) {
        ItemStack left = incoming.copy();
        for (int slot = 0; slot < container.getContainerSize() && !left.isEmpty(); slot++) {
            ItemStack other = container.getItem(slot);
            if (other == dispensed) {
                continue;
            }
            if (other.isEmpty()) {
                int moved = Math.min(left.getCount(), left.getMaxStackSize());
                container.setItem(slot, left.copyWithCount(moved));
                left.shrink(moved);
            } else if (ItemStack.isSameItemSameComponents(other, left) && other.getCount() < other.getMaxStackSize()) {
                int moved = Math.min(left.getCount(), other.getMaxStackSize() - other.getCount());
                other.grow(moved);
                left.shrink(moved);
            }
        }
        return left;
    }

    private static void spill(ServerLevel level, BlockSource source, Direction facing, ItemStack stack) {
        if (!stack.isEmpty()) {
            spawnItem(level, stack, 6, facing, source.center());
        }
    }

    private static boolean matches(ItemStack stack, net.minecraft.world.item.Item item, ChemicalBase chemical) {
        if (!stack.is(item)) {
            return false;
        }
        if (chemical == null) {
            return true;
        }
        ChemicalBase other = ChemicalItem.getChemicalBase(stack);
        return other != null && other.fullName.equals(chemical.fullName);
    }

    /** The chemical a fluid block holds, or null when the block is not a chemical's liquid. */
    static ChemicalBase chemicalOf(Block block) {
        if (block == Blocks.WATER) {
            return Jenkins.get(WATER);
        }
        for (MoleculeFluids.Entry entry : MoleculeFluids.entries()) {
            if (entry.block().get() == block) {
                return Jenkins.get(entry.molecule());
            }
        }
        for (ElementFluids.Entry entry : ElementFluids.entries()) {
            if (entry.block().get() == block) {
                return ChemicalBuckets.elementBySymbol(entry.symbol());
            }
        }
        return null;
    }

    /** The block of a chemical's liquid, or null when the chemical has none. */
    static Block liquidOf(ChemicalBase chemical) {
        if (chemical == null) {
            return null;
        }
        if (WATER.equals(chemical.fullName)) {
            return Blocks.WATER;
        }
        if (chemical.isElement()) {
            for (ElementFluids.Entry entry : ElementFluids.entries()) {
                if (entry.symbol().equals(((minechem.chemical.Element) chemical).shortName)) {
                    return entry.block().get();
                }
            }
            return null;
        }
        return MoleculeFluids.blockFor(chemical.fullName);
    }
}
