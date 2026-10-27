package minechem.client;

import com.mojang.serialization.MapCodec;
import minechem.chemical.ChemicalBase;
import minechem.chemical.Molecule;
import minechem.item.chemical.ChemicalItem;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * A molecule's second colour tints the molecule layer, after the first one. Elements have a single colour, so their layer
 * stays white. Without a chemical the layer is white, as in {@link ChemicalTint}.
 */
public final class ChemicalSecondaryTint implements ItemTintSource {
    public static final ChemicalSecondaryTint INSTANCE = new ChemicalSecondaryTint();
    public static final MapCodec<ChemicalSecondaryTint> MAP_CODEC = MapCodec.unit(INSTANCE);

    private ChemicalSecondaryTint() {
    }

    @Override
    public int calculate(ItemStack stack, ClientLevel level, LivingEntity entity) {
        ChemicalBase chemical = ChemicalItem.getChemicalBase(stack);
        if (!(chemical instanceof Molecule molecule)) {
            return 0xFFFFFFFF;
        }
        return 0xFF000000 | molecule.getColour2();
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
