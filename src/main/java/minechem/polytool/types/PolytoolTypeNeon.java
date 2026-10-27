package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class PolytoolTypeNeon extends PolytoolUpgradeType {
    @Override
    public int atomicNumber() {
        return 10;
    }

    @Override
    public void hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Night vision, for power * 10 ticks, on the attacker.
        attacker.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, (int) (power * 10)));
    }

    @Override
    public String getDescription() {
        return "Gives players night vision during combat";
    }
}
