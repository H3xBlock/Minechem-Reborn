package minechem.item.chemistjournal;

import minechem.registry.RecipeRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * A journal in the first grid slot and a book in the second makes two journals, copies of the first journal (its discovered
 * list, its active item and its owner come with it).
 *
 * <p>Only the first two grid slots are checked; the rest of the grid is not looked at, so an extra item in the other slots
 * does not stop the recipe.
 */
public class ChemistJournalCloneRecipe extends CustomRecipe {
    public static final ChemistJournalCloneRecipe INSTANCE = new ChemistJournalCloneRecipe();

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() < 2 || input.height() < 1) {
            return false;
        }
        ItemStack journal = input.getItem(0, 0);
        ItemStack book = input.getItem(1, 0);
        return journal.getItem() instanceof ChemistJournalItem && book.is(Items.BOOK);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack clone = input.getItem(0, 0).copyWithCount(2);
        return clone;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return RecipeRegistry.CLONE_JOURNAL.get();
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }
}
