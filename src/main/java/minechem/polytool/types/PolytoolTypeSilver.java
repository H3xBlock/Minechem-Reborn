package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PolytoolTypeSilver extends PolytoolUpgradeType {
    @Override
    public int atomicNumber() {
        return 47;
    }

    @Override
    public void hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target instanceof EnderMan && attacker instanceof Player player) {
            target.hurt(target.level().damageSources().playerAttack(player), 0.8F * power);
        }
    }

    @Override
    public String getDescription() {
        return "Extra damage vs Endermen";
    }
}
