package minechem.fluid;

import minechem.radiation.FluidRadiationData;
import minechem.radiation.RadiationState;
import minechem.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * A chemical's bucket out of a dispenser: when the block in front is not solid it is replaced by the chemical's liquid,
 * keeping the bucket's radioactivity, and the bucket comes back empty. A solid block in front, or a stack that is not a
 * chemical bucket, is left alone.
 *
 * <p>The replaced block is removed without its drop, as the vanilla bucket placement does.
 */
public final class ChemicalBucketDispenseBehavior extends DefaultDispenseItemBehavior {
    private static final ChemicalBucketDispenseBehavior INSTANCE = new ChemicalBucketDispenseBehavior();

    private ChemicalBucketDispenseBehavior() {
    }

    /** Registers the behaviour for every chemical bucket; called at common setup, after the items are registered. */
    public static void register() {
        for (MoleculeFluids.Entry entry : MoleculeFluids.entries()) {
            DispenserBlock.registerBehavior(entry.bucket().get(), INSTANCE);
        }
        for (ElementFluids.Entry entry : ElementFluids.entries()) {
            DispenserBlock.registerBehavior(entry.bucket().get(), INSTANCE);
        }
    }

    @Override
    protected ItemStack execute(BlockSource source, ItemStack stack) {
        if (!(stack.getItem() instanceof BucketItem bucket)) {
            return stack;
        }
        BlockPos target = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
        BlockState front = source.level().getBlockState(target);
        if (front.isSolid()) {
            return stack;
        }
        Fluid content = bucket.getContent();
        source.level().setBlock(target, content.defaultFluidState().createLegacyBlock(), 3);
        RadiationState radiation = stack.get(ItemRegistry.RADIATION.get());
        if (radiation != null) {
            FluidRadiationData.get(source.level()).put(target, radiation);
        }
        return consumeWithRemainder(source, stack, new ItemStack(Items.BUCKET));
    }
}
