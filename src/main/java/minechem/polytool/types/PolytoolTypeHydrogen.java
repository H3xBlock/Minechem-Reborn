package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PolytoolTypeHydrogen extends PolytoolUpgradeType {
    @Override
    public int atomicNumber() {
        return 1;
    }

    @Override
    public void hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = target.level();
        if (!level.isClientSide() && target instanceof Mob && target.isOnFire()) {
            level.explode(target, target.getX(), target.getY(), target.getZ(), power, true, Level.ExplosionInteraction.TNT);
        }
    }

    @Override
    public String getDescription() {
        return "Creates explosion if hit entity is on fire";
    }
}
