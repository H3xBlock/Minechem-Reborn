package minechem.polytool.types;

import minechem.polytool.PolytoolUpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class PolytoolTypeFluorine extends PolytoolUpgradeType {
    @Override
    public int atomicNumber() {
        return 9;
    }

    @Override
    public void onBlockDestroyed(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (entity instanceof Player player && RAND.nextInt(10) < power) {
            FoodProperties carrot = new ItemStack(Items.CARROT).get(DataComponents.FOOD);
            if (carrot != null) {
                player.getFoodData().eat(carrot);
            }
        }
    }

    @Override
    public String getDescription() {
        return "Preserves teeth to give you bonus food when mining";
    }
}
