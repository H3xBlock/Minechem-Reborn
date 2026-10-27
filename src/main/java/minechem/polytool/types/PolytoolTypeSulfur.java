package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class PolytoolTypeSulfur extends PolytoolUpgradeType {
    @Override
    public int atomicNumber() {
        return 16;
    }

    @Override
    public void hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.igniteForSeconds((int) (power + 1));
    }

    @Override
    public String getDescription() {
        return "Sets entities on fire";
    }
}
