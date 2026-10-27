package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class PolytoolTypeRubidium extends PolytoolUpgradeType {
    @Override
    public int atomicNumber() {
        return 37;
    }

    @Override
    public void hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target.isInWater()) {
            explode(target);
        }
    }

    @Override
    public void onBlockDestroyed(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (entity.isInWater()) {
            explode(entity);
        }
    }

    /** The explosion breaks blocks without fire; TNT interaction is the 26.1 equivalent. */
    private void explode(LivingEntity entity) {
        entity.level().explode(entity, entity.getX(), entity.getY(), entity.getZ(), power, false, Level.ExplosionInteraction.TNT);
    }

    @Override
    public String getDescription() {
        return "Creates explosion if in water";
    }
}
