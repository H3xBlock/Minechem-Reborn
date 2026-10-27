package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public class PolytoolTypeMagnesium extends PolytoolUpgradeType {
    @Override
    public int atomicNumber() {
        return 12;
    }

    @Override
    public void hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        for (LivingEntity entity : target.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(target.getX() - power, target.getY() - power, target.getZ() - power,
                        target.getX() + power, target.getY() + power, target.getZ() + power))) {
            // Blindness, level II (amplifier 1), for 200 ticks.
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 1));
        }
    }

    @Override
    public String getDescription() {
        return "Blinds nearby entities with flashbangs";
    }
}
