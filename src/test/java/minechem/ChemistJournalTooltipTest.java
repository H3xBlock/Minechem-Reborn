package minechem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import minechem.item.chemistjournal.ChemistJournalTooltip;
import org.junit.jupiter.api.Test;

/** Checks of the journal's tooltip lines against ChemistJournalItem.addInformation. */
class ChemistJournalTooltipTest {
    @Test
    void activeThenOwnerWhenBothAreSet() {
        assertEquals(List.of("Active: Water", "Owner: Steve"), ChemistJournalTooltip.lines("Water", "Steve"));
    }

    @Test
    void aNewJournalShowsNothing() {
        assertTrue(ChemistJournalTooltip.lines(null, null).isEmpty());
    }

    @Test
    void noActiveItemMeansNoActiveLine() {
        assertEquals(List.of("Owner: Steve"), ChemistJournalTooltip.lines("", "Steve"));
    }
}
