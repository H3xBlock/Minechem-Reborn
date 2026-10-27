package minechem.polytool;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class PolytoolUpgradesTest {

    @Test
    void addingPowerToAnElementAddsToItsEntry() {
        PolytoolUpgrades tool = PolytoolUpgrades.EMPTY.withPower(13, 1.5F).withPower(6, 2F).withPower(13, 0.5F);
        assertEquals(2F, tool.powerOf(13), 0.0001F);
        assertEquals(2F, tool.powerOf(6), 0.0001F);
        assertEquals(0F, tool.powerOf(1), 0.0001F);
        assertEquals(2, tool.upgrades().size(), "an element should keep one entry");
    }

    @Test
    void newElementsAreAppendedInOrder() {
        PolytoolUpgrades tool = PolytoolUpgrades.EMPTY.withPower(26, 1F).withPower(29, 1F);
        assertEquals(26, tool.upgrades().get(0).atomicNumber());
        assertEquals(29, tool.upgrades().get(1).atomicNumber());
    }

    @Test
    void codecRoundTrip() {
        PolytoolUpgrades tool = PolytoolUpgrades.EMPTY.withPower(8, 3.25F).withPower(12, 1F);
        PolytoolUpgrades back = PolytoolUpgrades.CODEC.parse(JsonOps.INSTANCE, PolytoolUpgrades.CODEC.encodeStart(JsonOps.INSTANCE, tool).getOrThrow()).getOrThrow();
        assertEquals(tool, back);
    }
}
