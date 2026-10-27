package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PolytoolTypeLead extends PolytoolUpgradeType {
    @Override
    public int atomicNumber() {
        return 82;
    }

    @Override
    public void hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level().isClientSide()) {
            for (LivingEntity entity : target.level().getEntitiesOfClass(LivingEntity.class,
                    new AABB(target.getX() - power * 3, target.getY() - power * 3, target.getZ() - power * 3,
                            target.getX() + power * 3, target.getY() + power * 3, target.getZ() + power * 3))) {
                if (entity != attacker) {
                    Vec3 motion = entity.getDeltaMovement();
                    entity.setDeltaMovement(motion.x, -50, motion.z);
                }
            }
        }
    }

    @Override
    public String getDescription() {
        return "Sends nearby entities flying to the ground";
    }
}
