package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PolytoolTypeHelium extends PolytoolUpgradeType {
    private static final String TARGET_KEY = "HeliumHitEntity";

    @Override
    public int atomicNumber() {
        return 2;
    }

    @Override
    public void hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(TARGET_KEY, target.getId()));
    }

    @Override
    public void onTickFull(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        int targetId = data == null ? 0 : data.copyTag().getIntOr(TARGET_KEY, 0);
        if (targetId != 0) {
            Entity target = level.getEntity(targetId);
            if (target != null) {
                Vec3 motion = target.getDeltaMovement();
                target.setDeltaMovement(motion.x, Math.min(motion.y + 0.1, 1), motion.z);
                if (RAND.nextInt((int) (10 * power)) == 0) {
                    CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(TARGET_KEY, 0));
                }
            }
        }
    }

    @Override
    public String getDescription() {
        return "Hit entities float away";
    }
}
