package minechem.apparatus.synthesis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import minechem.chemical.ChemicalBase;

/**
 * The synthesis recipe table. One recipe per output: the first recipe added for an output wins, and later ones are
 * ignored.
 *
 * <p>The key is the output: a registry id ("minecraft:stone") for an item, or a molecule's name for a molecule item.
 * Ingredients are kept in order; an empty slot of a shaped recipe is {@code null} (kept, not dropped).
 */
public final class SynthesisRecipes {
    /** The output id of the water bottle: a virtual item, since 26.1 has no water bottle item. */
    public static final String WATER_BOTTLE = "minechem:water_bottle";


    /** One ingredient: a chemical and its amount. A {@code null} ingredient is an empty slot of a shaped recipe. */
    public record Ingredient(ChemicalBase chemical, int amount) {
    }

    /**
     * A recipe: the output key, the output count, shaped or shapeless, the energy cost (before the synthesis multiplier),
     * and the ingredients.
     */
    public record Recipe(String output, int count, boolean shaped, int energy, List<Ingredient> ingredients) {
        public Recipe {
            ingredients = Collections.unmodifiableList(new ArrayList<>(ingredients));
        }
    }

    /** The table the game uses, filled at common setup from the bundled data (see SynthesisRecipeLoader). */
    public static final SynthesisRecipes RECIPES = new SynthesisRecipes();

    private final Map<String, Recipe> recipes = new LinkedHashMap<>();

    /** Adds a recipe unless its output already has one. Returns true when it was added. */
    public boolean add(Recipe recipe) {
        if (recipes.containsKey(recipe.output())) {
            return false;
        }
        recipes.put(recipe.output(), recipe);
        return true;
    }

    /** Removes {@code recipe} if it is still the recipe of its output (used to rebind the tag recipes on a tag update). */
    public boolean remove(Recipe recipe) {
        return recipes.remove(recipe.output(), recipe);
    }

    /** The energy of a synthesis is the recipe energy times this. */
    public static final int SYNTHESIS_MULTIPLIER = 10;

    /** The energy one batch of a recipe uses: its energy cost times {@link #SYNTHESIS_MULTIPLIER}. */
    public static int energyCost(Recipe recipe) {
        return recipe.energy() * SYNTHESIS_MULTIPLIER;
    }

    /** At most this many ore dictionary entries get a recipe. */
    public static final int ORE_ENTRY_LIMIT = 8;

    /**
     * The 9-slot grid of an ore dictionary recipe for entry {@code entry}: ingredient i goes to slot
     * {@code (i + entry) % 9}, so each entry's grid is shifted by one more slot.
     */
    public static List<Ingredient> oreGrid(List<Ingredient> chemicals, int entry) {
        Ingredient[] grid = new Ingredient[9];
        for (int i = 0; i < chemicals.size(); i++) {
            grid[(i + entry) % 9] = chemicals.get(i);
        }
        return java.util.Arrays.asList(grid);
    }

    /** The recipes in table order (the first recipe that matches a grid is the one the machine uses). */
    public java.util.Collection<Recipe> values() {
        return java.util.Collections.unmodifiableCollection(recipes.values());
    }

    public Recipe get(String output) {
        return recipes.get(output);
    }

    public int size() {
        return recipes.size();
    }
}
