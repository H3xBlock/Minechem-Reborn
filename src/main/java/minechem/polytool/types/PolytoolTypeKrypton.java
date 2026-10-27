package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PolytoolTypeKrypton extends PolytoolUpgradeType {
    @Override
    public int atomicNumber() {
        return 36;
    }

    @Override
    public void hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target instanceof Player player && target.level() instanceof ServerLevel serverLevel && player.getArmorValue() > 23) {
            player.hurtServer(serverLevel, player.damageSources().fellOutOfWorld(), power);
        }
    }

    @Override
    public String getDescription() {
        return "Does extra damage to heavily armored players";
    }
}
