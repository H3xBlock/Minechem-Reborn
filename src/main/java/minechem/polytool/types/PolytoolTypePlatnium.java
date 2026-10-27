package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PolytoolTypePlatnium extends PolytoolUpgradeType {
    @Override
    public int atomicNumber() {
        return 78;
    }

    @Override
    public void hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = target.level();
        if (!level.isClientSide()) {
            if (level.getRandom().nextInt(50) < power + 1) {
                BlockPos pos = new BlockPos((int) Math.round(attacker.getX()), (int) Math.round(attacker.getY()), (int) Math.round(attacker.getZ()));
                level.levelEvent(2002, pos, 0);
                int i = (int) (power + level.getRandom().nextInt(5) + level.getRandom().nextInt(5));

                while (i > 0) {
                    int j = ExperienceOrb.getExperienceValue(i);
                    i -= j;
                    level.addFreshEntity(new ExperienceOrb(level, attacker.getX(), attacker.getY(), attacker.getZ(), j));
                }
            }
        }
    }

    @Override
    public String getDescription() {
        return "Bonus XP";
    }
}
