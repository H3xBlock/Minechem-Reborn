package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class PolytoolTypeNickel extends PolytoolUpgradeType {
    @Override
    public int atomicNumber() {
        return 28;
    }

    @Override
    public void onBlockDestroyed(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity player) {
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class,
                new AABB(player.getX() - power, player.getY() - power, player.getZ() - power,
                        player.getX() + power, player.getY() + power, player.getZ() + power))) {
            item.setDeltaMovement(-1 * (item.getX() - player.getX()),
                    -1 * (item.getY() - player.getY()),
                    -1 * (item.getZ() - player.getZ()));
        }
    }

    @Override
    public String getDescription() {
        return "Sucks up nearby items when another block is mined";
    }
}
