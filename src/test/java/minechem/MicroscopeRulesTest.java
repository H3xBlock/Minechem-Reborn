package minechem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import minechem.apparatus.microscope.MicroscopeRules;
import org.junit.jupiter.api.Test;

/** Checks of the microscope's rules against MicroscopeBlockEntity and MicroscopeSlotOutput. */
class MicroscopeRulesTest {
    @Test
    void theMicroscopeHasElevenSlotsInFixedOrder() {
        assertEquals(11, MicroscopeRules.SLOTS);
        assertEquals(0, MicroscopeRules.INPUT_SLOT);
        assertEquals(1, MicroscopeRules.JOURNAL_SLOT);
        assertEquals(2, MicroscopeRules.FIRST_OUTPUT_SLOT);
        assertEquals(1, MicroscopeRules.STACK_LIMIT);
    }

    @Test
    void theInputTakesAnythingTheJournalOnlyAJournalAndTheOutputsNothing() {
        assertTrue(MicroscopeRules.canPlace(0, false));
        assertTrue(MicroscopeRules.canPlace(1, true));
        assertFalse(MicroscopeRules.canPlace(1, false), "a non-journal item is refused by the journal slot");
        for (int slot = 2; slot <= 10; slot++) {
            assertFalse(MicroscopeRules.canPlace(slot, true), "output slot " + slot + " refuses every item");
        }
    }

    @Test
    void anInspectedItemGoesIntoTheJournalOnlyWithAJournalAndARecipe() {
        assertTrue(MicroscopeRules.inspectionAddsToJournal(true, true));
        assertFalse(MicroscopeRules.inspectionAddsToJournal(false, true), "no journal, nothing is written");
        assertFalse(MicroscopeRules.inspectionAddsToJournal(true, false), "an item with no recipe is not written");
    }

    @Test
    void decrStackTakesTheWholeStackWhateverAmountIsAsked() {
        // The whole slot content is returned, not the requested amount.
        assertEquals(1, MicroscopeRules.taken(1, 1));
        assertEquals(64, MicroscopeRules.taken(1, 64));
    }
}
