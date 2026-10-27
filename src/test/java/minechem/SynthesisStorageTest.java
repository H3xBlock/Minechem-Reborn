package minechem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import minechem.apparatus.synthesis.SynthesisStorage;
import minechem.apparatus.synthesis.SynthesisStorage.Stack;
import org.junit.jupiter.api.Test;

/** Checks of the synthesis item logic against SynthesisBlockEntity. */
class SynthesisStorageTest {
    private static SynthesisStorage<String> storage() {
        return new SynthesisStorage<>(String::equals);
    }

    private static Stack<String> s(String item, int count) {
        return new Stack<>(item, count);
    }

    @Test
    void takesIngredientsFromEverySameKindSlotInOrder() {
        SynthesisStorage<String> storage = storage();
        storage.setSlot(0, s("X", 2));
        storage.setSlot(2, s("X", 3));
        assertTrue(storage.take(List.of(s("X", 4)), true));
        assertNull(storage.slot(0));
        assertEquals(s("X", 1), storage.slot(2));
    }

    @Test
    void aShortageLeavesTheStorageUnchanged() {
        SynthesisStorage<String> storage = storage();
        storage.setSlot(0, s("X", 2));
        assertFalse(storage.take(List.of(s("X", 3)), true));
        assertEquals(s("X", 2), storage.slot(0));
    }

    @Test
    void ingredientsShareOneWorkingCopy() {
        SynthesisStorage<String> storage = storage();
        storage.setSlot(0, s("X", 3));
        // Each ingredient alone fits (2 of 3), together they need 4: the second must fail.
        assertFalse(storage.take(List.of(s("X", 2), s("X", 2)), false));
        assertEquals(s("X", 3), storage.slot(0), "a check without doTake changes nothing");
    }

    @Test
    void checkWithoutDoTakeReportsButKeepsTheStorage() {
        SynthesisStorage<String> storage = storage();
        storage.setSlot(0, s("X", 5));
        assertTrue(storage.take(List.of(s("X", 5)), false));
        assertEquals(s("X", 5), storage.slot(0));
    }

    @Test
    void batchOutputSplitsAtTheStackLimitAndKeepsTheCounting() {
        // The loop compares the amount (decremented by the template count) with the batch count (incremented by one).
        // With amount 100 and a template of 30 it makes 4 batches, not 3: 30 + 30 + 30 + 30 = 120 items.
        SynthesisStorage<String> storage = storage();
        storage.setSlot(0, s("X", 10));
        List<Stack<String>> out = storage.batchOutput(List.of(s("X", 1)), s("Y", 30), 100, true, () -> true, () -> { });
        assertEquals(List.of(s("Y", 64), s("Y", 56)), out);
        assertEquals(s("X", 6), storage.slot(0), "one ingredient per batch, four batches");
    }

    @Test
    void batchOutputWithNoOutputStillReturnsItsZeroStack() {
        SynthesisStorage<String> storage = storage();
        storage.setSlot(0, s("X", 10));
        List<Stack<String>> out = storage.batchOutput(List.of(s("X", 1)), s("Y", 3), 10, false, () -> true, () -> { });
        assertEquals(List.of(s("Y", 0)), out);
        assertEquals(s("X", 10), storage.slot(0));
    }

    @Test
    void lowEnergyStopsBatchesBeforeAnyIngredientIsTaken() {
        SynthesisStorage<String> storage = storage();
        storage.setSlot(0, s("X", 10));
        AtomicInteger used = new AtomicInteger();
        List<Stack<String>> out = storage.batchOutput(List.of(s("X", 1)), s("Y", 3), 10, true, () -> false, used::incrementAndGet);
        assertEquals(List.of(s("Y", 0)), out);
        assertEquals(0, used.get(), "no energy is used when no batch is made");
        assertEquals(s("X", 10), storage.slot(0));
    }

    @Test
    void energyIsUsedOncePerBatch() {
        SynthesisStorage<String> storage = storage();
        storage.setSlot(0, s("X", 10));
        AtomicInteger used = new AtomicInteger();
        // Amount 4, template 2: batches are made while 4 > batch count, so 2 batches (amount 4 -> 2 -> 0).
        storage.batchOutput(List.of(s("X", 1)), s("Y", 2), 4, true, () -> true, used::incrementAndGet);
        assertEquals(2, used.get());
    }

    @Test
    void shapelessMatchNeedsExactCountsAndNoLeftovers() {
        SynthesisStorage<String> storage = storage();
        List<Stack<String>> recipe = List.of(s("A", 1), s("B", 2));
        assertTrue(storage.matchesShapeless(List.of(s("A", 1), s("B", 2)), recipe));
        assertFalse(storage.matchesShapeless(List.of(s("A", 1), s("B", 1)), recipe), "a wrong count does not match");
        assertFalse(storage.matchesShapeless(List.of(s("A", 1)), recipe), "a missing ingredient does not match");
        assertFalse(storage.matchesShapeless(List.of(s("A", 1), s("B", 2), s("B", 2)), recipe), "a spare stack does not match");
    }

    @Test
    void stateTextFollowsTheStateOrder() {
        assertEquals("Active", SynthesisStorage.state(true, true, true));
        assertEquals("No Recipe", SynthesisStorage.state(false, false, true));
        assertEquals("No Power", SynthesisStorage.state(false, true, false));
        assertEquals("Not Enough Ingredients", SynthesisStorage.state(false, true, true));
    }
}
