package minechem.devtools;

import minechem.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A development pickaxe: when it mines a block, the 5 x 5 square of blocks behind it (in the plane across the look direction) is
 * mined too, except the uranium ore, which is left in place. Only the dev client run registers it (see {@link DevTools}).
 */
public class DevPickaxeItem extends Item {
    /** Blocks on each side of the mined block: 2 gives a 5 x 5 square. */
    private static final int HALF = 2;

    public DevPickaxeItem(Properties properties) {
        super(properties.pickaxe(ToolMaterial.NETHERITE, 1F, -2.8F).stacksTo(1));
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!level.isClientSide() && miner instanceof Player player) {
            Direction.Axis across = lookDirection(player).getAxis();
            for (int a = -HALF; a <= HALF; a++) {
                for (int b = -HALF; b <= HALF; b++) {
                    if (a == 0 && b == 0) {
                        continue;
                    }
                    BlockPos target = switch (across) {
                        case X -> pos.offset(0, a, b);
                        case Y -> pos.offset(a, 0, b);
                        case Z -> pos.offset(a, b, 0);
                    };
                    BlockState other = level.getBlockState(target);
                    if (other.isAir() || other.is(BlockRegistry.URANIUM_ORE.get()) || other.getDestroySpeed(level, target) < 0) {
                        continue;
                    }
                    level.destroyBlock(target, true, player);
                }
            }
        }
        return super.mineBlock(stack, level, state, pos, miner);
    }

    /** The direction the player looks in, rounded to the nearest of the six. */
    private static Direction lookDirection(Player player) {
        var look = player.getLookAngle();
        return Direction.getApproximateNearest(look.x, look.y, look.z);
    }
}
