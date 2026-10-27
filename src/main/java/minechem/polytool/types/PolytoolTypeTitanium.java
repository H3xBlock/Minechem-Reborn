package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/**
 * Sets the main-hand drop chance of the mob.
 */
public class PolytoolTypeTitanium extends PolytoolUpgradeType {
    @Override
    public int atomicNumber() {
        return 22;
    }

    @Override
    public void hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target instanceof Mob mob) {
            mob.setDropChance(EquipmentSlot.MAINHAND, 0.8F);
        }
    }

    @Override
    public String getDescription() {
        return "Better chance to collect tools from mob drops";
    }
}
