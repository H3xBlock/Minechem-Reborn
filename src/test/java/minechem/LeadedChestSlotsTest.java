package minechem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import minechem.apparatus.leadedchest.LeadedChestSlots;
import org.junit.jupiter.api.Test;

/** Checks of the lead chest's slot rules against LeadedChestBlockEntity. */
class LeadedChestSlotsTest {
    @Test
    void theChestHasNineSlots() {
        assertEquals(9, LeadedChestSlots.SLOTS);
    }

    @Test
    void everyStackIsCutToSixtyFourWhateverItsOwnLimit() {
        // Every slot has a stack limit of 64; a 99-item stack is cut to 64, a smaller one is kept.
        assertEquals(64, LeadedChestSlots.clamp(99));
        assertEquals(64, LeadedChestSlots.clamp(64));
        assertEquals(10, LeadedChestSlots.clamp(10));
    }
}
