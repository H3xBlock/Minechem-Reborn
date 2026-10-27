package minechem.apparatus.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import minechem.handler.ElementHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The fission and fusion rules that the reactors and the JEI plugin share. */
class ReactorRecipeRuleTest {
    @BeforeAll
    static void loadElements() throws IOException {
        ElementHandler.init(Files.createTempDirectory("minechem-config"), true);
    }

    @Test
    void aFissionGivesTwoOfHalfTheAtomicNumber() {
        assertEquals(4, ReactorRules.halfOf(8), "oxygen splits into two helium-sized halves of 4");
        assertEquals(46, ReactorRules.halfOf(92), "uranium splits into two of palladium (46)");
    }

    @Test
    void aFissionOfHydrogenOrNothingGivesNothing() {
        assertEquals(0, ReactorRules.halfOf(1), "half of hydrogen is 0, which is not an element");
        assertEquals(0, ReactorRules.halfOf(0), "element 0 does not split");
    }

    @Test
    void twoElementsFuseIntoTheirSum() {
        assertEquals(2, ReactorRules.fusedOf(1, 1), "hydrogen and hydrogen make helium");
        assertEquals(10, ReactorRules.fusedOf(2, 8), "helium and oxygen make neon");
    }

    @Test
    void aFusionAboveTheLastElementOrWithNothingGivesNothing() {
        assertEquals(0, ReactorRules.fusedOf(60, 60), "a sum of 120 is not an element");
        assertEquals(0, ReactorRules.fusedOf(0, 5), "element 0 does not fuse");
    }
}
