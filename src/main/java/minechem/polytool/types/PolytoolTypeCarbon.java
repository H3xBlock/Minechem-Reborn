package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PolytoolTypeCarbon extends PolytoolUpgradeType {
    @Override
    public void onBlockDestroyed(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            int bonus = (int) (RAND.nextDouble() * Math.log(this.power));
            if (state.is(Blocks.COAL_ORE)) {
                serverLevel.addFreshEntity(new ItemEntity(level,
                        pos.getX() + RAND.nextDouble(), pos.getY() + RAND.nextDouble(), pos.getZ() + RAND.nextDouble(),
                        new ItemStack(Items.COAL, bonus)));
            }
            if (state.is(Blocks.DIAMOND_ORE)) {
                serverLevel.addFreshEntity(new ItemEntity(level,
                        pos.getX() + RAND.nextDouble(), pos.getY() + RAND.nextDouble(), pos.getZ() + RAND.nextDouble(),
                        new ItemStack(Items.DIAMOND, bonus)));
            }
        }
    }

    @Override
    public int atomicNumber() {
        return 6;
    }

    @Override
    public String getDescription() {
        return "More drops from coal and diamond ores";
    }
}
