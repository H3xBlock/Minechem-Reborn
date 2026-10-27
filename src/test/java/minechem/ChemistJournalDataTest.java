package minechem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import minechem.item.chemistjournal.ChemistJournalData;
import org.junit.jupiter.api.Test;

/** Checks of the chemist journal data against ChemistJournalItem. */
class ChemistJournalDataTest {
    @Test
    void anItemIsDiscoveredOnce() {
        ChemistJournalData<String> journal = new ChemistJournalData<>(String::equals);
        assertTrue(journal.addDiscovered("iron"));
        assertFalse(journal.addDiscovered("iron"), "the second discovery of the same item is ignored");
        assertTrue(journal.addDiscovered("gold"));
        assertEquals(List.of("iron", "gold"), journal.discovered(), "the first discovery keeps its place");
    }

    @Test
    void theActiveItemIsWhatWasSetAndNothingWhenUnset() {
        ChemistJournalData<String> journal = new ChemistJournalData<>(String::equals);
        assertTrue(journal.active().isEmpty());
        journal.setActive("water");
        assertEquals("water", journal.active().orElseThrow());
    }

    @Test
    void theOwnerIsKeptAsGiven() {
        ChemistJournalData<String> journal = new ChemistJournalData<>(String::equals);
        journal.setOwner("Steve");
        assertEquals("Steve", journal.owner());
    }

    @Test
    void theJournalSlotTakesOnlyOneJournal() {
        // The journal slot: isItemValid is the journal test, and the stack limit is 1.
        assertTrue(ChemistJournalData.acceptsInJournalSlot(true));
        assertFalse(ChemistJournalData.acceptsInJournalSlot(false));
        assertEquals(1, ChemistJournalData.JOURNAL_SLOT_STACK_LIMIT);
    }

    @Test
    void cloningAlwaysGivesTwoJournals() {
        // The result count is 2, whatever the count of the journal it was made from.
        assertEquals(2, ChemistJournalData.cloneResultCount());
        assertEquals(2, ChemistJournalData.CLONE_RESULT_COUNT);
    }
}
