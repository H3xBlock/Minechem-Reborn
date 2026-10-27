package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * atomicNumber() returns 30 (zinc), so the upgrade is stored under zinc, although the class is named for zirconium (40).
 */
public class PolytoolTypeZirconium extends PolytoolUpgradeType {
    @Override
    public int atomicNumber() {
        return 30;
    }

    @Override
    public void onBlockDestroyed(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (state.getBlock() == Blocks.DIRT) {
            if (RAND.nextInt(8192) < 1 + power) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + RAND.nextDouble(), pos.getY() + RAND.nextDouble(),
                    pos.getZ() + RAND.nextDouble(), new ItemStack(Items.DIAMOND)));
            }
        }
    }

    @Override
    public String getDescription() {
        return "Makes fake diamonds when mining dirt";
    }
}
