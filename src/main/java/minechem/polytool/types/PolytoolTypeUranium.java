package minechem.polytool.types;

import java.util.List;
import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Wither for 200 ticks at amplifier 1.
 */
public class PolytoolTypeUranium extends PolytoolUpgradeType {
    @Override
    public int atomicNumber() {
        return 92;
    }

    @Override
    public void hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        AABB area = new AABB(target.getX() - power, target.getY() - power, target.getZ() - power,
            target.getX() + power, target.getY() + power, target.getZ() + power);
        List<LivingEntity> targets = target.level().getEntitiesOfClass(LivingEntity.class, area);
        for (LivingEntity entity : targets) {
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
        }
    }

    @Override
    public String getDescription() {
        return "Area of Effect wither";
    }
}
