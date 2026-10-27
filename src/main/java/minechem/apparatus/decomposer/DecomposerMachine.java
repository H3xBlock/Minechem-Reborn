package minechem.apparatus.decomposer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Item logic of the decomposer: one input slot (slot 0), a buffer of outputs, and output slots 1..18.
 *
 * <p>Ticking: the state comes from determineOperationalState (one item leaves the buffer per tick,
 * into the output slots). If the machine is idle or finished, has energy, and has a recipe for its input, it starts a
 * decomposition: one input item is taken, the recipe is rolled, and the outputs replace the buffer. A chance miss
 * (roll returns null) starts nothing, but the state still becomes active.
 *
 * <p>Behaviour: the buffer is replaced, not appended to; outputs are compacted on every
 * placement ({@link #pushQueue}), merging only up to the stack limit; a merge stops at the first same-kind slot even
 * when a remainder is left.
 *
 * <p>The item type {@code T} is opaque: the caller says when two items are the same kind (Compare.stacksAreSameKind).
 * This class does not handle the fluid tank or the damage/broken multiplier (item NBT). The energy gate is the
 * caller's {@code energyOk} argument.
 */
public final class DecomposerMachine<T> {

    /** The machine states; "finished" only lasts the tick it is found, then becomes idle. */
    public enum State { idle, active, jammed, finished }

    /** An item stack: the item and its count. */
    public record Stack<T>(T item, int count) {
    }

    /** Stack limit used for merging. Every item is assumed to stack to 64. */
    public static final int STACK_LIMIT = 64;
    public static final int FIRST_OUTPUT = 1;
    public static final int LAST_OUTPUT = 18;

    private final BiPredicate<T, T> sameKind;
    private final Stack<T>[] slots;
    private final List<Stack<T>> buffer = new ArrayList<>();
    private Stack<T> input;
    private State state = State.idle;

    public DecomposerMachine(BiPredicate<T, T> sameKind) {
        this.sameKind = sameKind;
        this.slots = newSlots();
    }

    @SuppressWarnings("unchecked")
    private static <T> Stack<T>[] newSlots() {
        return (Stack<T>[]) new Stack<?>[LAST_OUTPUT + 1];
    }

    public void setInput(Stack<T> stack) {
        this.input = stack;
    }

    public Stack<T> input() {
        return input;
    }

    public State state() {
        return state;
    }

    public List<Stack<T>> buffer() {
        return List.copyOf(buffer);
    }

    /** Output slot {@code slot} (1..18), or null when empty. */
    public Stack<T> outputSlot(int slot) {
        checkOutputSlot(slot);
        return slots[slot];
    }

    /** Sets an output slot directly (used by tests and by loading saved data). */
    public void setOutputSlot(int slot, Stack<T> stack) {
        checkOutputSlot(slot);
        slots[slot] = stack;
    }

    /** Sets the state (used by loading saved data and by the client, which gets the state from the server). */
    public void setState(State state) {
        this.state = state;
    }

    /** Replaces the buffer (used by loading saved data). */
    public void setBuffer(List<Stack<T>> stacks) {
        buffer.clear();
        buffer.addAll(stacks);
    }

    /**
     * One server tick (updateEntity).
     *
     * @param energyOk  the energy gate
     * @param hasRecipe whether a recipe exists for the input item (no roll)
     * @param roll      the outputs for the input item, or null on a chance miss
     */
    public void tick(boolean energyOk, Predicate<T> hasRecipe, Function<T, List<Stack<T>>> roll) {
        state = determineOperationalState();
        if ((state == State.idle || state == State.finished)
            && energyOk && input != null && hasRecipe.test(input.item())) {
            decomposeActiveStack(roll);
            state = State.active;
        } else if (state == State.finished) {
            state = State.idle;
        }
    }

    /** decomposeActiveStack: takes one input item, rolls the recipe, and buffers the outputs when there are some. */
    private void decomposeActiveStack(Function<T, List<Stack<T>>> roll) {
        T item = input.item();
        input = input.count() > 1 ? new Stack<>(item, input.count() - 1) : null;
        List<Stack<T>> outputs = roll.apply(item);
        if (outputs != null) {
            placeStacksInBuffer(outputs);
        }
    }

    /** determineOperationalState: moves one item of the first buffer stack into the output slots. */
    private State determineOperationalState() {
        if (buffer.isEmpty()) {
            return State.finished;
        }
        Stack<T> first = buffer.get(0);
        if (!addStackToOutputSlots(first.item())) {
            return State.jammed;
        }
        if (first.count() > 1) {
            buffer.set(0, new Stack<>(first.item(), first.count() - 1));
        } else {
            buffer.remove(0);
        }
        return State.active;
    }

    /** addStackToOutputSlots for one item: merge into the first same-kind slot that has room, else the first empty slot. */
    private boolean addStackToOutputSlots(T item) {
        for (int slot = FIRST_OUTPUT; slot <= LAST_OUTPUT; slot++) {
            Stack<T> inSlot = slots[slot];
            if (inSlot != null && sameKind.test(inSlot.item(), item) && inSlot.count() + 1 <= STACK_LIMIT) {
                slots[slot] = new Stack<>(inSlot.item(), inSlot.count() + 1);
                return true;
            }
        }
        for (int slot = FIRST_OUTPUT; slot <= LAST_OUTPUT; slot++) {
            if (slots[slot] == null) {
                slots[slot] = new Stack<>(item, 1);
                return true;
            }
        }
        return false;
    }

    /** placeStacksInBuffer: the buffer is replaced, then the output slots are compacted (pushQueue). */
    private void placeStacksInBuffer(List<Stack<T>> outputs) {
        buffer.clear();
        buffer.addAll(outputs);
        pushQueue();
    }

    /** pushQueue: moves each output stack toward lower slots, merging same kind up to the stack limit. */
    private void pushQueue() {
        for (int i = LAST_OUTPUT; i > FIRST_OUTPUT; i--) {
            if (slots[i] == null) {
                continue;
            }
            for (int j = FIRST_OUTPUT; j < i; j++) {
                if (slots[j] == null) {
                    slots[j] = slots[i];
                    slots[i] = null;
                    break;
                }
                if (sameKind.test(slots[j].item(), slots[i].item())) {
                    T item = slots[i].item();
                    int spaceLeft = STACK_LIMIT - slots[j].count();
                    if (spaceLeft >= slots[i].count()) {
                        slots[j] = new Stack<>(item, slots[j].count() + slots[i].count());
                        slots[i] = null;
                    } else {
                        slots[j] = new Stack<>(item, slots[j].count() + spaceLeft);
                        slots[i] = new Stack<>(item, slots[i].count() - spaceLeft);
                    }
                    break;
                }
            }
        }
    }

    private static void checkOutputSlot(int slot) {
        if (slot < FIRST_OUTPUT || slot > LAST_OUTPUT) {
            throw new IllegalArgumentException("output slot must be 1..18: " + slot);
        }
    }
}
