package minechem.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import minechem.chemical.ChemicalBase;
import minechem.chemical.Element;
import minechem.item.chemical.ChemicalItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Gives the element's symbol to the item model (for example "H" or "Uuo"), and the item model selects the matching symbol
 * texture. Molecules and other chemicals return "none", which selects an empty model.
 */
public final class ChemicalSymbolProperty implements SelectItemModelProperty<String> {
    public static final ChemicalSymbolProperty INSTANCE = new ChemicalSymbolProperty();
    public static final SelectItemModelProperty.Type<ChemicalSymbolProperty, String> TYPE =
        SelectItemModelProperty.Type.create(MapCodec.unit(INSTANCE), Codec.STRING);

    private ChemicalSymbolProperty() {
    }

    @Override
    public String get(ItemStack stack, ClientLevel level, LivingEntity entity, int seed, ItemDisplayContext context) {
        ChemicalBase chemical = ChemicalItem.getChemicalBase(stack);
        if (chemical instanceof Element element) {
            return element.shortName;
        }
        return "none";
    }

    @Override
    public Codec<String> valueCodec() {
        return Codec.STRING;
    }

    @Override
    public SelectItemModelProperty.Type<? extends SelectItemModelProperty<String>, String> type() {
        return TYPE;
    }
}
