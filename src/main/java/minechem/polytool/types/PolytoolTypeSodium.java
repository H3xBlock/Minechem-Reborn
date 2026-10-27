package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The torch goes at the floored position of the miner, not the block mined.
 */
public class PolytoolTypeSodium extends PolytoolUpgradeType {
    @Override
    public int atomicNumber() {
        return 11;
    }

    @Override
    public void onBlockDestroyed(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        BlockPos torchPos = BlockPos.containing(entity.getX(), entity.getY(), entity.getZ());
        if (!level.isClientSide() && level.getRandom().nextInt(35) < power && level.getMaxLocalRawBrightness(torchPos) < 12) {
            BlockState torch = Blocks.TORCH.defaultBlockState();
            if (torch.canSurvive(level, torchPos)) {
                level.setBlock(torchPos, torch, 3);
            }
        }
    }

    @Override
    public String getDescription() {
        return "Lights up area when mining";
    }
}
