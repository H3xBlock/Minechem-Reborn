package minechem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import minechem.apparatus.decomposer.DecomposerMachine;
import minechem.apparatus.decomposer.DecomposerMachine.State;
import minechem.apparatus.decomposer.DecomposerMachine.Stack;
import org.junit.jupiter.api.Test;

/** Tick-by-tick checks of the decomposer item logic against DecomposerBlockEntity. */
class DecomposerMachineTest {
    private static final Predicate<String> HAS_RECIPE = item -> true;
    private static final Predicate<String> NO_RECIPE = item -> false;

    private static DecomposerMachine<String> machine() {
        return new DecomposerMachine<>(String::equals);
    }

    private static Stack<String> s(String item, int count) {
        return new Stack<>(item, count);
    }

    /** A roll that always gives the same outputs, whatever the input. */
    private static Function<String, List<Stack<String>>> outputs(List<Stack<String>> outputs) {
        return item -> outputs;
    }

    @Test
    void idleWithoutInputDoesNothing() {
        DecomposerMachine<String> machine = machine();
        machine.tick(true, HAS_RECIPE, outputs(List.of(s("X", 1))));
        assertEquals(State.idle, machine.state());
        assertTrue(machine.buffer().isEmpty());
    }

    @Test
    void noRecipeOrNoEnergyLeavesTheInputAlone() {
        DecomposerMachine<String> noRecipe = machine();
        noRecipe.setInput(s("A", 3));
        noRecipe.tick(true, NO_RECIPE, outputs(List.of(s("X", 1))));
        assertEquals(State.idle, noRecipe.state());
        assertEquals(s("A", 3), noRecipe.input());

        DecomposerMachine<String> noEnergy = machine();
        noEnergy.setInput(s("A", 3));
        noEnergy.tick(false, HAS_RECIPE, outputs(List.of(s("X", 1))));
        assertEquals(State.idle, noEnergy.state());
        assertEquals(s("A", 3), noEnergy.input());
    }

    @Test
    void startingTakesOneInputAndBuffersTheOutputsWithoutMovingThem() {
        DecomposerMachine<String> machine = machine();
        machine.setInput(s("A", 3));
        machine.tick(true, HAS_RECIPE, outputs(List.of(s("X", 2))));
        assertEquals(State.active, machine.state());
        assertEquals(s("A", 2), machine.input());
        assertEquals(List.of(s("X", 2)), machine.buffer());
        assertNull(machine.outputSlot(1));
    }

    @Test
    void oneItemLeavesTheBufferPerTick() {
        DecomposerMachine<String> machine = machine();
        machine.setInput(s("A", 3));
        machine.tick(true, HAS_RECIPE, outputs(List.of(s("X", 2))));
        machine.tick(true, NO_RECIPE, outputs(List.of()));
        assertEquals(s("X", 1), machine.outputSlot(1));
        assertEquals(List.of(s("X", 1)), machine.buffer());
        assertEquals(State.active, machine.state());
    }

    @Test
    void bufferEmptiesThenTheMachineGoesIdle() {
        DecomposerMachine<String> machine = machine();
        machine.setInput(s("A", 1));
        machine.tick(true, HAS_RECIPE, outputs(List.of(s("X", 1))));
        machine.tick(true, NO_RECIPE, outputs(List.of()));
        assertEquals(State.active, machine.state(), "the tick that moves the last item is still active");
        assertTrue(machine.buffer().isEmpty());
        assertNull(machine.input());
        machine.tick(true, NO_RECIPE, outputs(List.of()));
        assertEquals(State.idle, machine.state(), "a finished buffer with no new job becomes idle");
    }

    @Test
    void sameKindMergesIntoTheFirstSlotUpToTheLimit() {
        DecomposerMachine<String> machine = machine();
        machine.setOutputSlot(1, s("X", 63));
        machine.setInput(s("A", 1));
        machine.tick(true, HAS_RECIPE, outputs(List.of(s("X", 1), s("X", 1))));
        machine.tick(true, NO_RECIPE, outputs(List.of()));
        assertEquals(s("X", 64), machine.outputSlot(1));
        assertNull(machine.outputSlot(2));
        machine.tick(true, NO_RECIPE, outputs(List.of()));
        assertEquals(s("X", 64), machine.outputSlot(1), "a full slot is not merged into");
        assertEquals(s("X", 1), machine.outputSlot(2), "the next item goes to the first empty slot");
    }

    @Test
    void theMachineJamsWhenEverySlotIsFullAndTheBufferKeepsItsItem() {
        DecomposerMachine<String> machine = machine();
        for (int slot = 1; slot <= 18; slot++) {
            machine.setOutputSlot(slot, s("Y" + slot, 1));
        }
        machine.setInput(s("A", 1));
        machine.tick(true, HAS_RECIPE, outputs(List.of(s("X", 1))));
        machine.tick(true, NO_RECIPE, outputs(List.of()));
        assertEquals(State.jammed, machine.state());
        assertEquals(List.of(s("X", 1)), machine.buffer());
    }

    @Test
    void chanceMissStartsNothingButTheMachineIsActive() {
        DecomposerMachine<String> machine = machine();
        machine.setInput(s("A", 2));
        machine.tick(true, HAS_RECIPE, item -> null);
        assertEquals(State.active, machine.state(), "the machine sets active after decomposeActiveStack");
        assertTrue(machine.buffer().isEmpty());
        assertEquals(s("A", 1), machine.input(), "the input item is taken even on a miss");
    }

    @Test
    void placingBuffersCompactsOutputsTowardTheFirstSlot() {
        DecomposerMachine<String> machine = machine();
        machine.setOutputSlot(3, s("X", 5));
        machine.setInput(s("A", 1));
        machine.tick(true, HAS_RECIPE, outputs(List.of()));
        assertEquals(s("X", 5), machine.outputSlot(1), "pushQueue moves the stack to the first empty slot");
        assertNull(machine.outputSlot(3));
    }

    @Test
    void compactingMergesSameKindOnlyUpToTheLimit() {
        DecomposerMachine<String> machine = machine();
        machine.setOutputSlot(1, s("X", 60));
        machine.setOutputSlot(2, s("X", 10));
        machine.setInput(s("A", 1));
        machine.tick(true, HAS_RECIPE, outputs(List.of()));
        assertEquals(s("X", 64), machine.outputSlot(1));
        assertEquals(s("X", 6), machine.outputSlot(2), "the remainder stays in the later slot");
    }

    @Test
    void outputSlotsAreOneToEighteen() {
        DecomposerMachine<String> machine = machine();
        assertThrows(IllegalArgumentException.class, () -> machine.outputSlot(0));
        assertThrows(IllegalArgumentException.class, () -> machine.outputSlot(19));
    }
}
