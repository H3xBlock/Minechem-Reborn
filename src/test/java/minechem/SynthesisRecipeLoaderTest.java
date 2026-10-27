package minechem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import minechem.apparatus.synthesis.SynthesisRecipeLoader;
import minechem.apparatus.synthesis.SynthesisRecipes;
import minechem.apparatus.synthesis.SynthesisRecipes.Recipe;
import minechem.handler.ElementHandler;
import minechem.handler.MoleculeHandler;
import minechem.helper.Jenkins;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Checks of the bundled synthesis recipes (converted to 26.1 ids). */
class SynthesisRecipeLoaderTest {
    @BeforeAll
    static void loadChemistry() throws IOException {
        var configDir = Files.createTempDirectory("minechem-config");
        ElementHandler.init(configDir, true);
        MoleculeHandler.init(configDir, true);
    }

    @Test
    void theBundledFileGivesOneRecipePerOutput() {
        // 404 entries, two outputs come twice (pumpkin and diamond): the first of each wins.
        SynthesisRecipes table = new SynthesisRecipes();
        SynthesisRecipeLoader.loadBundled(table);
        // 404 entries; the polytool and the water bottle add one output each: 402 outputs.
        assertEquals(402, table.size());
    }

    @Test
    void aShapedStoneRecipeKeepsItsEmptySlotsAndItsCount() {
        SynthesisRecipes table = new SynthesisRecipes();
        SynthesisRecipeLoader.loadBundled(table);
        Recipe stone = table.get("minecraft:stone");
        assertNotNull(stone);
        assertEquals(16, stone.count(), "the stone recipe yields 16 items");
        assertTrue(stone.shaped());
        assertEquals(30, stone.energy(), "COST_SMOOTH");
        assertEquals(6, stone.ingredients().size());
        assertNull(stone.ingredients().get(2), "an empty grid slot is kept as null");
        assertEquals(Jenkins.get("Si"), stone.ingredients().get(0).chemical());
        assertEquals(3, stone.ingredients().get(4).amount());
    }

    @Test
    void aMoleculeItemIsKeyedByItsNameWithTheMoleculeEnergy() {
        SynthesisRecipes table = new SynthesisRecipes();
        SynthesisRecipeLoader.loadBundled(table);
        Recipe water = table.get("Water");
        assertNotNull(water, "the molecule water has a synthesis recipe");
        assertEquals(10, water.energy(), "COST_ITEM");
        assertFalse(water.shaped());
    }

    @Test
    void theEnergyOfABatchIsTheRecipeEnergyTimesTheMultiplier() {
        SynthesisRecipes table = new SynthesisRecipes();
        SynthesisRecipeLoader.loadBundled(table);
        // stone: energy 30 -> 300 per batch (Settings.synthesisMultiplier = 10).
        assertEquals(300, SynthesisRecipes.energyCost(table.get("minecraft:stone")));
        assertEquals(100, SynthesisRecipes.energyCost(table.get("Water")));
    }

    @Test
    void anOreRecipeGridIsShiftedByItsEntryIndex() {
        var a = new SynthesisRecipes.Ingredient(Jenkins.get("Fe"), 16);
        var b = new SynthesisRecipes.Ingredient(Jenkins.get("O"), 1);
        var grid0 = SynthesisRecipes.oreGrid(java.util.List.of(a, b), 0);
        assertEquals(a, grid0.get(0));
        assertEquals(b, grid0.get(1));
        var grid3 = SynthesisRecipes.oreGrid(java.util.List.of(a, b), 3);
        assertNull(grid3.get(0));
        assertEquals(a, grid3.get(3));
        assertEquals(b, grid3.get(4));
        // Entry 8 wraps: the first ingredient lands in slot 8, the second in slot 0.
        var grid8 = SynthesisRecipes.oreGrid(java.util.List.of(a, b), 8);
        assertEquals(b, grid8.get(0));
        assertEquals(a, grid8.get(8));
    }

    @Test
    void anUnknownIngredientFailsLoudly() {
        String json = "{\"recipes\":[{\"output\":\"minecraft:stone\",\"count\":1,\"shaped\":false,\"energy\":1,"
            + "\"chemicals\":[{\"symbol\":\"Zz\",\"amount\":1}]}]}";
        SynthesisRecipes table = new SynthesisRecipes();
        assertThrows(IllegalStateException.class, () -> SynthesisRecipeLoader.load(table, new StringReader(json)));
    }

    /** The game's load (skipUnknown): a recipe with an unknown ingredient is skipped, the next one is still loaded. */
    @Test
    void theGameSkipsARecipeWithAnUnknownIngredient() {
        String json = "{\"recipes\":["
            + "{\"output\":\"minecraft:stone\",\"count\":1,\"shaped\":false,\"energy\":1,\"chemicals\":[{\"symbol\":\"Zz\",\"amount\":1}]},"
            + "{\"output\":\"minecraft:dirt\",\"count\":1,\"shaped\":false,\"energy\":1,\"chemicals\":[{\"symbol\":\"H\",\"amount\":1}]}]}";
        SynthesisRecipes table = new SynthesisRecipes();
        assertEquals(1, SynthesisRecipeLoader.load(table, new StringReader(json), true));
        assertEquals(null, table.get("minecraft:stone"));
        assertNotNull(table.get("minecraft:dirt"));
    }
}
