package minechem.client;

import com.mojang.serialization.MapCodec;
import minechem.chemical.ChemicalBase;
import minechem.item.chemical.ChemicalItem;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Port of ChemicalItem.getColorFromItemStack: the second layer of the chemical item model is tinted with the chemical's
 * colour. Without a chemical the layer is left white, so the texture shows unchanged.
 */
public final class ChemicalTint implements ItemTintSource {
    public static final ChemicalTint INSTANCE = new ChemicalTint();
    public static final MapCodec<ChemicalTint> MAP_CODEC = MapCodec.unit(INSTANCE);

    private ChemicalTint() {
    }

    @Override
    public int calculate(ItemStack stack, ClientLevel level, LivingEntity entity) {
        ChemicalBase chemical = ChemicalItem.getChemicalBase(stack);
        if (chemical == null) {
            return 0xFFFFFFFF;
        }
        return 0xFF000000 | chemical.getColour();
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
