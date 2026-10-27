package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class PolytoolTypeMercury extends PolytoolUpgradeType {
    @Override
    public int atomicNumber() {
        return 80;
    }

    @Override
    public void onBlockDestroyed(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        // Only players are warned: a non-player above a lava source does nothing.
        if (!(entity instanceof Player player)) {
            return;
        }
        int search = (int) (4 * power + 1);
        for (int i = 0; i < search; i++) {
            FluidState fluid = level.getFluidState(pos.below(i));
            if (fluid.getType().is(FluidTags.LAVA)) {
                player.sendSystemMessage(Component.literal("WARNING: LAVA UNDERNEATH").withStyle(ChatFormatting.RED));
                break;
            }
        }
    }

    @Override
    public String getDescription() {
        return "Warns of lava underneath";
    }
}
