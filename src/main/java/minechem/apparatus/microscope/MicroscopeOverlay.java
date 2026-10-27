package minechem.apparatus.microscope;

import java.util.List;
import minechem.apparatus.decomposer.DecomposerBlockEntity;
import minechem.apparatus.decomposer.DecomposerRecipes;
import minechem.apparatus.synthesis.SynthesisRecipes;
import minechem.item.chemical.ChemicalItem;
import net.minecraft.world.item.ItemStack;

/**
 * What the microscope shows for the item in its input slot: the recipe's ingredients, or its outputs, in the nine
 * output slots, and the energy or the chance under them. The stacks are drawn over the empty slots, so the slots
 * themselves stay empty.
 */
public final class MicroscopeOverlay {
    /** The slideshow of a select recipe: 8 seconds per alternative, at 20 ticks a second. */
    public static final int SLIDE_TICKS = 8 * 20;
    public static final int SLOTS = MicroscopeRules.OUTPUT_SLOTS;

    /** The stacks for the nine slots (EMPTY where none), and the text under them (null when none). */
    public record Overlay(List<ItemStack> slots, String text) {
    }

    private static final Overlay NOTHING = new Overlay(List.of(), null);

    private MicroscopeOverlay() {
    }

    /** The synthesis recipe of the input: its ingredients, and the energy it costs. */
    public static Overlay synthesis(ItemStack input) {
        if (input.isEmpty()) {
            return NOTHING;
        }
        SynthesisRecipes.Recipe recipe = SynthesisRecipes.RECIPES.get(DecomposerBlockEntity.keyFor(input));
        if (recipe == null) {
            return NOTHING;
        }
        ItemStack[] slots = emptySlots();
        for (int i = 0; i < Math.min(SLOTS, recipe.ingredients().size()); i++) {
            SynthesisRecipes.Ingredient ingredient = recipe.ingredients().get(i);
            if (ingredient != null && ingredient.chemical() != null) {
                slots[i] = ChemicalItem.stackFor(ingredient.chemical()).copyWithCount(ingredient.amount());
            }
        }
        return new Overlay(List.of(slots), SynthesisRecipes.energyCost(recipe) + " Energy");
    }

    /**
     * The decomposer recipe of the input: its outputs, or for a select recipe the outputs of the alternative that is on
     * screen at {@code tick}. The chance is shown for a recipe with a chance below 1 (see the note in the status document).
     */
    public static Overlay decomposer(ItemStack input, long tick) {
        if (input.isEmpty()) {
            return NOTHING;
        }
        DecomposerRecipes.Recipe recipe = DecomposerBlockEntity.RECIPES.get(DecomposerBlockEntity.keyFor(input));
        if (recipe == null) {
            return NOTHING;
        }
        boolean select = !recipe.alternatives().isEmpty();
        List<DecomposerRecipes.Output> outputs = select
            ? recipe.alternatives().get((int) ((tick / SLIDE_TICKS) % recipe.alternatives().size())).outputs()
            : recipe.outputs();
        ItemStack[] slots = emptySlots();
        for (int i = 0; i < Math.min(SLOTS, outputs.size()); i++) {
            DecomposerRecipes.Output output = outputs.get(i);
            if (output.chemical() != null) {
                slots[i] = ChemicalItem.stackFor(output.chemical()).copyWithCount(output.amount());
            }
        }
        String text = !select && recipe.chance() < 1.0F ? (int) (recipe.chance() * 100) + "%" : null;
        return new Overlay(List.of(slots), text);
    }

    private static ItemStack[] emptySlots() {
        ItemStack[] slots = new ItemStack[SLOTS];
        java.util.Arrays.fill(slots, ItemStack.EMPTY);
        return slots;
    }
}
