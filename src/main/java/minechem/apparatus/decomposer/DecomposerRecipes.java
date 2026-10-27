package minechem.apparatus.decomposer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import minechem.chemical.ChemicalBase;

/**
 * The recipe table of the decomposer. Rules:
 * <ul>
 * <li>one recipe per input key; the first recipe added for a key wins and later ones are ignored;</li>
 * <li>an input on the blacklist, or with a decomposer multiplier of 0, is not added;</li>
 * <li>a recipe with a chance returns no output with probability {@code 1 - chance}.</li>
 * </ul>
 * The key is a string (an item id, or "fluid:" + fluid id).
 */
public final class DecomposerRecipes {

    /** One output chemical and its amount. */
    public record Output(ChemicalBase chemical, int amount) {
    }

    /**
     * A recipe: the input key, the chance (1.0 = always) and the outputs. A select recipe
     * has alternatives instead: the chance is rolled first, then one alternative is picked uniformly.
     */
    public record Recipe(String input, float chance, List<Output> outputs, List<Recipe> alternatives,
                         Map<String, Double> components, int fluidAmount) {
        public Recipe {
            outputs = List.copyOf(outputs);
            alternatives = List.copyOf(alternatives);
            components = Map.copyOf(components);
        }

        public Recipe(String input, float chance, List<Output> outputs) {
            this(input, chance, outputs, List.of(), Map.of(), 0);
        }

        /** The same recipe for a fluid: the tank must hold {@code amount} mB, which is used up when a decomposition starts. */
        public Recipe withFluidAmount(int amount) {
            return new Recipe(input, chance, outputs, alternatives, components, amount);
        }

        public static Recipe select(String input, float chance, List<Recipe> alternatives) {
            return new Recipe(input, chance, List.of(), alternatives, Map.of(), 0);
        }

        /**
         * A list recipe: each component key adds a weight, the number of times its own
         * recipe is rolled. A whole weight rolls it that many times; the fraction is a chance of one more roll.
         */
        public static Recipe composite(String input, Map<String, Double> components) {
            return new Recipe(input, 1.0F, List.of(), List.of(), components, 0);
        }
    }

    private final Map<String, Recipe> recipes = new LinkedHashMap<>();

    /** Every recipe in the table, in the order they were added (for the JEI plugin). */
    public java.util.Collection<Recipe> values() {
        return java.util.Collections.unmodifiableCollection(recipes.values());
    }
    private final java.util.Set<String> blacklist = new java.util.HashSet<>();
    private final Random random;

    public DecomposerRecipes(Random random) {
        this.random = random;
    }

    public DecomposerRecipes() {
        this(new Random());
    }

    /** Items that are never decomposed. */
    public void blacklist(String input) {
        blacklist.add(input);
    }

    /** Adds a recipe unless its input is blacklisted or already has one. Returns true when it was added. */
    public boolean add(Recipe recipe) {
        if (blacklist.contains(recipe.input()) || recipes.containsKey(recipe.input())) {
            return false;
        }
        recipes.put(recipe.input(), recipe);
        return true;
    }

    /** Removes {@code recipe} if it is still the recipe of its input (used to rebind the tag recipes on a tag update). */
    public boolean remove(Recipe recipe) {
        return recipes.remove(recipe.input(), recipe);
    }

    private static void addAll(List<Output> into, List<Output> more) {
        if (more != null) {
            into.addAll(more);
        }
    }

    /** Fluid recipes are keyed "fluid:<name>". One is added only when the fluid exists. */
    public static String fluidKey(String fluidName) {
        return "fluid:" + fluidName;
    }

    public boolean addFluidIfRegistered(String fluidName, Recipe recipe, java.util.function.Predicate<String> fluidExists) {
        if (!fluidExists.test(fluidName)) {
            return false;
        }
        return add(recipe);
    }

    public Recipe get(String input) {
        return recipes.get(input);
    }

    /**
     * The keys of the chemicals an input decomposes into: the outputs of its recipe, of its alternatives and of the recipes of its
     * components. Used by the microscope to discover what an inspected item decomposes into. Each key is listed once.
     */
    public List<String> productKeys(String input) {
        java.util.Set<String> keys = new java.util.LinkedHashSet<>();
        java.util.Set<String> visited = new java.util.HashSet<>();
        if (visited.add(input)) {
            Recipe recipe = recipes.get(input);
            if (recipe != null) {
                collectProducts(recipe, keys, visited);
            }
        }
        return new ArrayList<>(keys);
    }

    private void collectProducts(Recipe recipe, java.util.Set<String> keys, java.util.Set<String> visited) {
        for (Output output : recipe.outputs()) {
            if (output.chemical() != null) {
                keys.add(output.chemical().fullName);
            }
        }
        for (Recipe alternative : recipe.alternatives()) {
            collectProducts(alternative, keys, visited);
        }
        for (String component : recipe.components().keySet()) {
            Recipe own = visited.add(component) ? recipes.get(component) : null;
            if (own != null) {
                collectProducts(own, keys, visited);
            }
        }
    }

    public int size() {
        return recipes.size();
    }

    /**
     * The outputs for one input, rolled once: null when there is no recipe or the chance fails.
     */
    public List<Output> roll(String input) {
        Recipe recipe = recipes.get(input);
        if (recipe == null) {
            return null;
        }
        if (!recipe.components().isEmpty()) {
            List<Output> result = new ArrayList<>(recipe.outputs());
            for (Map.Entry<String, Double> component : recipe.components().entrySet()) {
                double weight = component.getValue();
                if (!recipes.containsKey(component.getKey())) {
                    continue;
                }
                for (; weight >= 1; weight--) {
                    addAll(result, roll(component.getKey()));
                }
                if (random.nextDouble() < weight) {
                    addAll(result, roll(component.getKey()));
                }
            }
            return result;
        }
        if (recipe.chance() < 1.0F && random.nextFloat() >= recipe.chance()) {
            return null;
        }
        if (recipe.alternatives().isEmpty()) {
            return new ArrayList<>(recipe.outputs());
        }
        Recipe picked = recipe.alternatives().get(random.nextInt(recipe.alternatives().size()));
        return new ArrayList<>(picked.outputs());
    }
}
