package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PolytoolTypeCaesium extends PolytoolUpgradeType {
    @Override
    public void onBlockDestroyed(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (state.is(Blocks.STONE)) {
            // The explosion destroys blocks: ExplosionInteraction.BLOCK.
            level.explode(entity, entity.getX(), entity.getY(), entity.getZ(), power, Level.ExplosionInteraction.BLOCK);
        }
    }

    @Override
    public int atomicNumber() {
        return 55;
    }

    @Override
    public String getDescription() {
        return "Creates explosion when mining stone";
    }
}
