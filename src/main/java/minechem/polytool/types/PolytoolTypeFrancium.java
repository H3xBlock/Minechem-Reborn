package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class PolytoolTypeFrancium extends PolytoolUpgradeType {
    @Override
    public int atomicNumber() {
        return 87;
    }

    @Override
    public void hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        explode(target.level(), target.getX(), target.getY(), target.getZ());
    }

    @Override
    public void onBlockDestroyed(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        // The z position is always 0.5, whatever the block's z.
        explode(level, pos.getX() + 0.5D, pos.getY() + 0.5D, 0.5D);
    }

    private void explode(Level level, double x, double y, double z) {
        if (level.isRaining()) {
            level.explode(null, x, y, z, power, true, Level.ExplosionInteraction.TNT);
        }
    }

    @Override
    public String getDescription() {
        return "Creates explosions when raining";
    }
}
