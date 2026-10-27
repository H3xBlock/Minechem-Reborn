package minechem.polytool;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ElementAlloyTest {

    @Test
    void valuesAreDoubled() {
        // Aluminium(Al, axe 4, shovel -2, pickaxe -2, stone 0, sword 0), doubled in the constructor.
        assertEquals(8F, ElementAlloy.Aluminium.axe, 0F);
        assertEquals(-4F, ElementAlloy.Aluminium.shovel, 0F);
        assertEquals(-4F, ElementAlloy.Aluminium.pickaxe, 0F);
        assertEquals(0F, ElementAlloy.Aluminium.stone, 0F);
        assertEquals(0F, ElementAlloy.Aluminium.sword, 0F);
    }

    @Test
    void palladiumIsAFourSwordAlloy() {
        assertEquals(8F, ElementAlloy.Palladium.sword, 0F);
        assertEquals("Pd", ElementAlloy.Palladium.symbol);
    }

    @Test
    void atomicNumbersMatchTheElements() {
        assertEquals(13, ElementAlloy.Aluminium.atomicNumber);
        assertEquals(48, ElementAlloy.Cadmium.atomicNumber);
        assertEquals(46, ElementAlloy.Palladium.atomicNumber);
    }

    @Test
    void thereAreFifteenAlloys() {
        assertEquals(15, ElementAlloy.values().length);
    }
}
