package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public class PolytoolTypeChlorine extends PolytoolUpgradeType {
    @Override
    public void hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        AABB area = new AABB(target.getX() - power, target.getY() - power, target.getZ() - power,
                target.getX() + power, target.getY() + power, target.getZ() + power);
        for (LivingEntity entity : target.level().getEntitiesOfClass(LivingEntity.class, area)) {
            // Poison, level II (amplifier 1), for 200 ticks.
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
        }
    }

    @Override
    public int atomicNumber() {
        return 17;
    }

    @Override
    public String getDescription() {
        return "Area of Effect poison";
    }
}
