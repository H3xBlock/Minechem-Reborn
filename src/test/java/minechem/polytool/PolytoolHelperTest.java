package minechem.polytool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PolytoolHelperTest {

    @Test
    void everyAlloyHasItsOwnUpgrade() {
        for (ElementAlloy alloy : ElementAlloy.values()) {
            PolytoolUpgradeType type = PolytoolHelper.getTypeFromElement(alloy.atomicNumber, 1F);
            assertNotNull(type, alloy.symbol + " should be a polytool upgrade");
            assertEquals(alloy.atomicNumber, type.atomicNumber());
        }
    }

    @Test
    void carbonAndZirconiumUpgradesResolve() {
        assertEquals(6, PolytoolHelper.getTypeFromElement(6, 2F).atomicNumber());
        // The zirconium type reports zinc (30).
        assertEquals(30, PolytoolHelper.getTypeFromElement(30, 1F).atomicNumber());
    }

    @Test
    void elementsWithoutAnUpgradeGiveNothing() {
        assertNull(PolytoolHelper.getTypeFromElement(3, 1F));
    }

    @Test
    void powerIsSetOnTheResult() {
        assertEquals(3.5F, PolytoolHelper.getTypeFromElement(26, 3.5F).power, 0F);
    }
}
