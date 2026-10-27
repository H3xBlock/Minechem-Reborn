package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PolytoolTypeGold extends PolytoolUpgradeType {
    @Override
    public int atomicNumber() {
        return 79;
    }

    // Redstone Flux items are not charged here: no energy API is hooked to them yet, so only the lightning strike happens.
    @Override
    public void onTickFull(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level instanceof ServerLevel serverLevel && RAND.nextInt(35000) < power) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.TRIGGERED);
            if (bolt != null) {
                bolt.setPos(entity.position());
                serverLevel.addFreshEntity(bolt);
            }
        }
    }

    @Override
    public String getDescription() {
        return "Occasionally creates lightning strikes which charge inventory";
    }
}
