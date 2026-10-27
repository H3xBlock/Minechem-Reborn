package minechem.polytool.types;

import java.util.ArrayDeque;
import java.util.Queue;
import minechem.polytool.PolytoolUpgradeType;
import minechem.polytool.PolytoolUpgrades;
import minechem.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class PolytoolTypeIron extends PolytoolUpgradeType {
    private static final int CARBON = 6;

    // The vanilla ore tags decide which blocks are ores.
    private static boolean isOre(BlockState state) {
        return state.is(BlockTags.COAL_ORES)
            || state.is(BlockTags.COPPER_ORES)
            || state.is(BlockTags.DIAMOND_ORES)
            || state.is(BlockTags.EMERALD_ORES)
            || state.is(BlockTags.GOLD_ORES)
            || state.is(BlockTags.IRON_ORES)
            || state.is(BlockTags.LAPIS_ORES)
            || state.is(BlockTags.REDSTONE_ORES);
    }

    @Override
    public int atomicNumber() {
        return 26;
    }

    @Override
    public void onBlockDestroyed(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (level.isClientSide() || !(entity instanceof Player player) || !isOre(state)) {
            return;
        }
        float carbon = stack.getOrDefault(ItemRegistry.POLYTOOL_UPGRADES.get(), PolytoolUpgrades.EMPTY).powerOf(CARBON);
        int toMine = (int) power;
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(pos);
        while (!queue.isEmpty()) {
            BlockPos current = queue.remove();
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (level.getBlockState(next).equals(state)) {
                    breakExtraBlock(level, next, state, player, carbon);
                    queue.add(next);
                    toMine--;
                    if (toMine <= 0) {
                        return;
                    }
                }
            }
        }
    }

    @Override
    public String getDescription() {
        return "Mines deposits of ores";
    }

    private static void breakExtraBlock(Level level, BlockPos pos, BlockState state, Player player, float carbon) {
        if (player.isCreative()) {
            level.removeBlock(pos, false);
            return;
        }
        // Carbon gives diamond and coal ore a bonus drop.
        int bonus = (state.is(BlockTags.DIAMOND_ORES) || state.is(BlockTags.COAL_ORES)) ? (int) (RAND.nextDouble() * Math.log(carbon)) + 1 : 1;
        level.destroyBlock(pos, false, player, Block.UPDATE_ALL);
        for (int i = 0; i < bonus; i++) {
            Block.dropResources(state, level, pos, null, player, player.getMainHandItem());
        }
    }
}
