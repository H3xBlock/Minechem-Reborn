package minechem.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.stream.Stream;
import minechem.chemical.Element;
import minechem.helper.Jenkins;
import minechem.item.chemical.ChemicalItem;
import minechem.registry.ItemRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

/**
 * An ingredient that is one element of a given atomic number, whatever its count.
 *
 * <p>In a recipe: {@code {"type": "minechem:element", "atomic_number": 82}}.
 */
public record ElementIngredient(int atomicNumber) implements ICustomIngredient {
    public static final MapCodec<ElementIngredient> CODEC = Codec.INT.fieldOf("atomic_number")
        .xmap(ElementIngredient::new, ElementIngredient::atomicNumber);
    public static final IngredientType<ElementIngredient> TYPE = new IngredientType<>(CODEC);

    @Override
    public boolean test(ItemStack stack) {
        return stack.is(ItemRegistry.CHEMICAL.get())
            && ChemicalItem.getChemicalBase(stack) instanceof Element element
            && element.atomicNumber == atomicNumber;
    }

    @Override
    public Stream<Holder<Item>> items() {
        return Stream.of(BuiltInRegistries.ITEM.wrapAsHolder(ItemRegistry.CHEMICAL.get()));
    }

    /**
     * What JEI and the recipe book show: the element itself (for example lead for 82), not the generic chemical item that
     * {@link #items()} gives. Falls back to the generic item when the element does not exist.
     */
    @Override
    public SlotDisplay display() {
        Element element = Jenkins.get(atomicNumber);
        if (element == null) {
            return ICustomIngredient.super.display();
        }
        return new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(ChemicalItem.stackFor(element)));
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE;
    }
}
