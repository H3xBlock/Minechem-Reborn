package minechem.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import minechem.chemical.ChemicalBase;
import minechem.item.chemical.ChemicalItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Chooses the item model layers for a chemical. The key picks the item model: the form (solid, liquid, gas, plasma), the
 * frame of the animated layer, and whether the chemical is an element or a molecule (molecules add the molecule symbol
 * layer). Keys are "e_" or "m_" plus the form, for example "e_liquid_3" or "m_solid".
 *
 * Animation: liquid and gas cycle through 7 frames over 1500 ms; plasma has one frame.
 */
public final class ChemicalFormProperty implements SelectItemModelProperty<String> {
    public static final ChemicalFormProperty INSTANCE = new ChemicalFormProperty();
    public static final SelectItemModelProperty.Type<ChemicalFormProperty, String> TYPE =
        SelectItemModelProperty.Type.create(MapCodec.unit(INSTANCE), Codec.STRING);

    private static final long PERIOD_MS = 1500L;
    private static final int FRAMES = 7;

    private ChemicalFormProperty() {
    }

    @Override
    public String get(ItemStack stack, ClientLevel level, LivingEntity entity, int seed, ItemDisplayContext context) {
        ChemicalBase chemical = ChemicalItem.getChemicalBase(stack);
        if (chemical == null) {
            return "none";
        }
        int frame = (int) (System.currentTimeMillis() % PERIOD_MS * FRAMES / PERIOD_MS) + 1;
        String kind = chemical.isElement() ? "e_" : "m_";
        return kind + switch (chemical.form) {
            case solid -> "solid";
            case liquid -> "liquid_" + frame;
            case gas -> "gas_" + frame;
            case plasma -> "plasma_1";
        };
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
