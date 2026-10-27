package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class PolytoolTypeChromium extends PolytoolUpgradeType {
    @Override
    public int atomicNumber() {
        return 24;
    }

    @Override
    public void onBlockDestroyed(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (!level.isClientSide() && RAND.nextInt(10) < power) {
            if (state.is(BlockTags.WOOL)) {
                level.removeBlock(pos, false);

                // Each wool colour is its own item here, picked by index (0-14).
                DyeColor color = DyeColor.byId(RAND.nextInt(15));
                Item wool = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(color.getName() + "_wool"));
                level.addFreshEntity(new ItemEntity(level, pos.getX() + RAND.nextDouble(), pos.getY() + RAND.nextDouble(), pos.getZ() + RAND.nextDouble(), new ItemStack(wool)));
            }
        }
    }

    @Override
    public String getDescription() {
        return "Chance to change color of wool when mined";
    }
}
