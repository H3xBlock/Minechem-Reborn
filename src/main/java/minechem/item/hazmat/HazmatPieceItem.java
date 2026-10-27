package minechem.item.hazmat;

import java.util.function.Consumer;
import minechem.radiation.IRadiationShield;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * One piece of the radiation suit. Worn, it adds its factor to the reduction of the radiation damage (see RadiationTicker).
 * The tooltip shows the percentage.
 */
public class HazmatPieceItem extends Item implements IRadiationShield {
    private final float factor;

    public HazmatPieceItem(Item.Properties properties, float factor) {
        super(properties);
        this.factor = factor;
    }

    @Override
    public float radiationReductionFactor() {
        return factor;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        builder.accept(Component.translatable("tooltip.minechem.radiation_shielding", Math.round(factor * 100)));
    }
}
