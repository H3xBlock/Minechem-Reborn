package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

public class PolytoolTypeArgon extends PolytoolUpgradeType {
    @Override
    public void hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = target.level();
        BlockPos pos = target.blockPosition();
        // Below the WORLD_SURFACE heightmap the block cannot see the sky, so the target takes wall damage.
        if (level instanceof ServerLevel serverLevel
                && pos.getY() < level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ())) {
            target.hurtServer(serverLevel, target.damageSources().inWall(), power);
        }
    }

    @Override
    public int atomicNumber() {
        return 18;
    }

    @Override
    public String getDescription() {
        return "Does extra suffocation damage when in a cave";
    }
}
