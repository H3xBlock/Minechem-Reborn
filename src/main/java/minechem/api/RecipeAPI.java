package minechem.api;

import java.util.ArrayList;
import java.util.List;
import minechem.apparatus.decomposer.DecomposerBlockEntity;
import minechem.apparatus.decomposer.DecomposerRecipes;
import net.minecraft.world.item.ItemStack;

/**
 * The recipe API, for other mods.
 */
public final class RecipeAPI {
    private RecipeAPI() {
    }

    /**
     * Adds a decomposition recipe: the decomposer turns the stack into the outputs. Each output is
     * written "count name", for example "1 H" or "2 Water", where the name is an element or a molecule as in {@link ChemicalAPI}.
     *
     * @param input the stack to be decomposed
     * @param outputs the molecules or elements produced by the recipe
     * @return whether the recipe was added (false when the stack already has a recipe, is blacklisted, or an output is not known)
     */
    public static boolean addDecompositionRecipe(ItemStack input, String... outputs) {
        try {
            List<DecomposerRecipes.Output> parts = new ArrayList<>();
            for (String output : outputs) {
                String[] words = output.trim().split(" ", 2);
                parts.add(new DecomposerRecipes.Output(ChemicalAPI.chemical(words[1].trim()), Integer.parseInt(words[0])));
            }
            String key = DecomposerBlockEntity.keyFor(input);
            return DecomposerBlockEntity.RECIPES.add(new DecomposerRecipes.Recipe(key, 1.0F, parts));
        } catch (RuntimeException e) {
            return false;
        }
    }
}
