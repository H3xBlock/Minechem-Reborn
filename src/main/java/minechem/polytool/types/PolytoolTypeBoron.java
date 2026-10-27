package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PolytoolTypeBoron extends PolytoolUpgradeType {
    @Override
    public void hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if ((target instanceof Spider || target instanceof CaveSpider)
                && attacker instanceof Player player
                && target.level() instanceof ServerLevel serverLevel) {
            target.hurtServer(serverLevel, player.damageSources().playerAttack(player), 0.8F * power);
        }
    }

    @Override
    public int atomicNumber() {
        return 5;
    }

    @Override
    public String getDescription() {
        return "Kills spiders with pesticides";
    }
}
