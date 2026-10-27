package minechem.fluid;

import minechem.radiation.FluidRadiationData;
import minechem.radiation.RadiationState;
import minechem.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A chemical's bucket that keeps its radioactivity when it empties. The state is carried from the
 * stack to the placed fluid, see {@link FluidRadiationData}.
 */
public class RadioactiveBucketItem extends BucketItem {
    /** The state of the stack being emptied by {@link #use}, for the placement that the vanilla use makes. */
    private static final ThreadLocal<RadiationState> EMPTYING = new ThreadLocal<>();

    public RadioactiveBucketItem(Fluid content, Properties properties) {
        super(content, properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        RadiationState state = player.getItemInHand(hand).get(ItemRegistry.RADIATION.get());
        EMPTYING.set(state);
        try {
            return super.use(level, player, hand);
        } finally {
            EMPTYING.remove();
        }
    }

    /**
     * The radiation clock writes the bucket's RADIATION component every player tick (RadiationTicker): only a change of slot or
     * of item replays the re-equip animation (see ChemicalItem.shouldCauseReequipAnimation).
     */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !newStack.is(this);
    }

    @Override
    public boolean emptyContents(LivingEntity user, Level level, BlockPos pos, BlockHitResult hit) {
        boolean placed = super.emptyContents(user, level, pos, hit);
        RadiationState state = EMPTYING.get();
        if (placed && state != null && level instanceof ServerLevel server) {
            FluidRadiationData.get(server).put(pos, state);
        }
        return placed;
    }
}
