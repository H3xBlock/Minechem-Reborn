package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class PolytoolTypeOxygen extends PolytoolUpgradeType {
    @Override
    public int atomicNumber() {
        return 8;
    }

    @Override
    public void onBlockDestroyed(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        entity.setAirSupply((int) (entity.getAirSupply() + power));
    }

    @Override
    public String getDescription() {
        return "Gives extra air when mining underwater";
    }
}
