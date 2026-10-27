package minechem.apparatus.synthesis;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;

/**
 * The pure item logic of the synthesis machine: taking a recipe's ingredients out of the storage, batching the output,
 * matching a shapeless recipe, and the state text. Generic over the item type T, like the decomposer machine; the caller
 * says when two items are the same kind (Compare.stacksAreSameKind).
 *
 * <p>Taking ingredients walks the storage in slot order and takes from every same-kind slot until the count is met; a batch
 * always returns its first (zero-count) stack, even when no batch was made; the output stack is split at the stack limit,
 * and a remainder becomes a new stack.
 *
 * <p>Not here: the recipe table, the energy cost (the caller's {@code BooleanSupplier} and {@code Runnable}), and the
 * storage slots' numbering in the container.
 */
public final class SynthesisStorage<T> {

    /** An item stack: the item and its count. */
    public record Stack<T>(T item, int count) {
    }

    /** Storage slots: the machine's slots 10 to 28. */
    public static final int STORAGE_SLOTS = 19;

    /** Output stack limit used for splitting: 64 for every item. */
    public static final int STACK_LIMIT = 64;

    private final BiPredicate<T, T> sameKind;
    private final List<Stack<T>> storage = new ArrayList<>();

    public SynthesisStorage(BiPredicate<T, T> sameKind) {
        this.sameKind = sameKind;
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            storage.add(null);
        }
    }

    public Stack<T> slot(int index) {
        return storage.get(index);
    }

    public void setSlot(int index, Stack<T> stack) {
        storage.set(index, stack);
    }

    /**
     * Can every ingredient be taken from the storage? With {@code doTake}, the storage is changed.
     * Ingredients are taken in order, sharing one working copy, so a shortfall on one leaves the storage unchanged.
     */
    public boolean take(List<Stack<T>> ingredients, boolean doTake) {
        List<Stack<T>> working = new ArrayList<>(storage);
        for (Stack<T> ingredient : ingredients) {
            if (ingredient != null && !takeOne(working, ingredient)) {
                return false;
            }
        }
        if (doTake) {
            storage.clear();
            storage.addAll(working);
        }
        return true;
    }

    /** Walks the slots in order, taking from every same-kind stack until the count is met. */
    private boolean takeOne(List<Stack<T>> working, Stack<T> ingredient) {
        int left = ingredient.count();
        for (int slot = 0; slot < working.size(); slot++) {
            Stack<T> stored = working.get(slot);
            if (stored != null && sameKind.test(stored.item(), ingredient.item())) {
                int amount = Math.min(stored.count(), left);
                left -= amount;
                int remaining = stored.count() - amount;
                working.set(slot, remaining > 0 ? new Stack<>(stored.item(), remaining) : null);
                if (left <= 0) {
                    break;
                }
            }
        }
        return left == 0;
    }

    /**
     * Makes output stacks of the recipe's output while the output slot is filled, the energy allows it, and the ingredients
     * can be taken; each batch takes the ingredients and adds one template count. The result always starts with a
     * zero-count stack.
     *
     * @param outputPresent whether the output slot holds the recipe's output
     * @param energyOk      checked before each batch (hasEnoughPowerForCurrentRecipe)
     * @param useEnergy     called once per batch that takes the ingredients (useEnergy(energyCost))
     */
    public List<Stack<T>> batchOutput(List<Stack<T>> ingredients, Stack<T> template, int amount, boolean outputPresent,
                                      BooleanSupplier energyOk, Runnable useEnergy) {
        List<Stack<T>> outputs = new ArrayList<>();
        outputs.add(new Stack<>(template.item(), 0));
        int took = 0;
        while (outputPresent && energyOk.getAsBoolean() && take(ingredients, false) && amount > took && takeInputs(ingredients, useEnergy)) {
            took++;
            Stack<T> last = outputs.get(outputs.size() - 1);
            if (last.count() + template.count() > STACK_LIMIT) {
                int leftOver = template.count() - (STACK_LIMIT - last.count());
                outputs.set(outputs.size() - 1, new Stack<>(last.item(), STACK_LIMIT));
                if (leftOver > 0) {
                    outputs.add(new Stack<>(template.item(), leftOver));
                }
            } else {
                outputs.set(outputs.size() - 1, new Stack<>(last.item(), last.count() + template.count()));
            }
            amount -= template.count();
        }
        return outputs;
    }

    /** Takes the ingredients if they can be taken, and uses the energy for it. */
    private boolean takeInputs(List<Stack<T>> ingredients, Runnable useEnergy) {
        if (take(ingredients, false)) {
            take(ingredients, true);
            useEnergy.run();
            return true;
        }
        return false;
    }

    /**
     * Every given stack must match a distinct ingredient of the same kind and the same count, and no ingredient may be left
     * over.
     */
    public boolean matchesShapeless(List<Stack<T>> given, List<Stack<T>> recipe) {
        List<Stack<T>> left = new ArrayList<>(recipe);
        for (Stack<T> stack : given) {
            if (stack == null) {
                continue;
            }
            int index = -1;
            for (int i = 0; i < left.size(); i++) {
                Stack<T> ingredient = left.get(i);
                if (ingredient != null && sameKind.test(stack.item(), ingredient.item()) && stack.count() == ingredient.count()) {
                    index = i;
                    break;
                }
            }
            if (index == -1) {
                return false;
            }
            left.remove(index);
        }
        return left.isEmpty();
    }

    /**
     * The 9 grid slots must match the 9 recipe slots position by position. An empty slot matches an empty ingredient;
     * otherwise the kind and the count must be the same.
     */
    public boolean matchesShaped(List<Stack<T>> grid, List<Stack<T>> recipe) {
        for (int i = 0; i < grid.size(); i++) {
            Stack<T> stack = grid.get(i);
            Stack<T> ingredient = i < recipe.size() ? recipe.get(i) : null;
            if (stack == null && ingredient == null) {
                continue;
            }
            if (stack == null || ingredient == null) {
                return false;
            }
            if (!sameKind.test(stack.item(), ingredient.item()) || stack.count() != ingredient.count()) {
                return false;
            }
        }
        return true;
    }

    /**
     * The state text: "Active" when the output can be taken, "No Recipe" with no output, "No Power" when the energy is short,
     * otherwise "Not Enough Ingredients".
     */
    public static String state(boolean canTakeOutput, boolean outputPresent, boolean powerOk) {
        if (canTakeOutput) {
            return "Active";
        }
        if (!outputPresent) {
            return "No Recipe";
        }
        return powerOk ? "Not Enough Ingredients" : "No Power";
    }
}
