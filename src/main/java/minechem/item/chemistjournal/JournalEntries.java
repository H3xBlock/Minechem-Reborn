package minechem.item.chemistjournal;

import minechem.chemical.ChemicalBase;
import minechem.helper.Jenkins;
import minechem.item.chemical.ChemicalItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * The item behind a journal entry. An entry is the key the recipe tables use: a chemical's full name, or an item's registry
 * id (see DecomposerBlockEntity.keyFor). Unknown keys give an empty stack, so a journal written by another version is shown
 * with the entries that still exist.
 */
public final class JournalEntries {
    private JournalEntries() {
    }

    public static ItemStack stackFor(String key) {
        if (key == null || key.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ChemicalBase chemical = Jenkins.get(key);
        if (chemical != null) {
            return ChemicalItem.stackFor(chemical);
        }
        Identifier id = Identifier.tryParse(key);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.getValue(id));
    }
}
