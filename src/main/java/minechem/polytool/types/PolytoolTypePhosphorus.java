package minechem.polytool.types;

import java.lang.reflect.Method;
import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Flammability is FireBlock's canBurn. It is protected and 26.1 has no public call for it, so it is read by reflection from the
 * existing fire block. A new block cannot be built once the registries are frozen, which is when this runs.
 */
public class PolytoolTypePhosphorus extends PolytoolUpgradeType {
    private static final Method CAN_BURN = findCanBurn();

    @Override
    public int atomicNumber() {
        return 15;
    }

    @Override
    public void onBlockDestroyed(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity target) {
        if (!level.isClientSide()) {
            if (canBurn(state)) {
                int x = pos.getX();
                int y = pos.getY();
                int z = pos.getZ();
                for (int i = (int) (x - power); i < x + power; i++) {
                    for (int j = (int) (y - power); j < y + power; j++) {
                        for (int k = (int) (z - power); k < z + power; k++) {
                            BlockPos near = new BlockPos(i, j, k);
                            if (level.getBlockState(near).isAir()) {
                                level.setBlockAndUpdate(near, Blocks.FIRE.defaultBlockState());
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getDescription() {
        return "Sets nearby blocks on fire";
    }

    private static boolean canBurn(BlockState state) {
        try {
            return (boolean) CAN_BURN.invoke(Blocks.FIRE, state);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("FireBlock.canBurn is not accessible", e);
        }
    }

    private static Method findCanBurn() {
        try {
            Method method = FireBlock.class.getDeclaredMethod("canBurn", BlockState.class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("FireBlock.canBurn(BlockState) not found", e);
        }
    }
}
