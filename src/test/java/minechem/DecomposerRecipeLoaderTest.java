package minechem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.List;
import minechem.apparatus.decomposer.DecomposerRecipeLoader;
import minechem.apparatus.decomposer.DecomposerRecipes;
import minechem.apparatus.decomposer.DecomposerRecipes.Output;
import minechem.chemical.ChemicalBase;
import minechem.handler.ElementHandler;
import minechem.handler.MoleculeHandler;
import minechem.helper.Jenkins;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Checks of the bundled decomposer recipes (converted to 26.1 ids). */
class DecomposerRecipeLoaderTest {
    @BeforeAll
    static void loadChemistry() throws IOException {
        var configDir = Files.createTempDirectory("minechem-config");
        ElementHandler.init(configDir, true);
        MoleculeHandler.init(configDir, true);
    }

    @Test
    void theBundledFileGivesOneRecipePerInput() {
        // The file has 379 entries without a "requires" key (the water bottle included). Two have the input pumpkin: the
        // pumpkin-seed recipe also uses the pumpkin block as its input, so the first one wins and the second is not added: 378.
        DecomposerRecipes table = new DecomposerRecipes();
        DecomposerRecipeLoader.loadBundled(table);
        assertEquals(378, table.size());
    }

    @Test
    void variantsAreKeyedByTheirVanillaMetadata() {
        // Metadata values: planks 3 = jungle, dye 3 = cocoa beans, dye 0 = black.
        DecomposerRecipes table = new DecomposerRecipes();
        DecomposerRecipeLoader.loadBundled(table);
        assertNotNull(table.get("minecraft:jungle_planks"));
        assertNotNull(table.get("minecraft:cocoa_beans"));
        assertNotNull(table.get("minecraft:black_dye"));
        assertNotNull(table.get("minecraft:oak_planks"), "a recipe exists for metadata 0 planks");
    }

    @Test
    void selectRecipesPickOneAlternativeAndMolecuesAreKeyedByName() {
        DecomposerRecipes table = new DecomposerRecipes();
        DecomposerRecipeLoader.loadBundled(table);
        assertNotNull(table.get("minecraft:stone"), "select recipe");
        assertNotNull(table.get("minecraft:podzol"), "dirt metadata 2 is podzol");
        assertNotNull(table.get("Water"), "a molecule item is keyed by its molecule name");
        assertNotNull(table.get("minecraft:map"), "list recipe");
    }

    @Test
    void fluidRecipesKeepTheAmountTheyUse() {
        DecomposerRecipes table = new DecomposerRecipes();
        DecomposerRecipeLoader.loadBundled(table);
        assertEquals(1000, table.get("fluid:water").fluidAmount(), "water uses 1000 mB");
        assertEquals(250, table.get("fluid:lava").fluidAmount(), "lava uses 250 mB");
        assertEquals(0.2F, table.get("fluid:lava").chance(), 0.0001F);
    }

    @Test
    void thePumpkinKeepsOnlyItsFirstRecipe() {
        DecomposerRecipes table = new DecomposerRecipes();
        DecomposerRecipeLoader.loadBundled(table);
        ChemicalBase cucurbitacin = Jenkins.get("Cucurbitacin");
        assertEquals(List.of(new Output(cucurbitacin, 1)), table.roll("minecraft:pumpkin"));
    }

    @Test
    void aMoleculeOutputIsResolvedByName() {
        DecomposerRecipes table = new DecomposerRecipes();
        DecomposerRecipeLoader.loadBundled(table);
        ChemicalBase silicaDioxide = Jenkins.get("Silicon Dioxide");
        assertNotNull(silicaDioxide, "the molecule siliconDioxide must exist");
        assertEquals(List.of(new Output(silicaDioxide, 16)), table.roll("minecraft:sand"));
    }

    @Test
    void anElementOutputIsResolvedBySymbolInFileOrder() {
        DecomposerRecipes table = new DecomposerRecipes();
        DecomposerRecipeLoader.loadBundled(table);
        ChemicalBase potassium = Jenkins.get("K");
        ChemicalBase sodium = Jenkins.get("Na");
        ChemicalBase carbon = Jenkins.get("C");
        List<Output> outputs = table.roll("minecraft:cooked_chicken");
        assertEquals(List.of(new Output(potassium, 1), new Output(sodium, 1), new Output(carbon, 2)), outputs);
    }

    @Test
    void anUnknownOutputFailsLoudly() {
        String json = "{\"recipes\":[{\"input\":\"minecraft:stone\",\"chance\":1.0,\"outputs\":[{\"symbol\":\"Zz\",\"amount\":1}]}]}";
        DecomposerRecipes table = new DecomposerRecipes();
        assertThrows(IllegalStateException.class, () -> DecomposerRecipeLoader.load(table, new StringReader(json)));
    }

    /** The game's load (skipUnknown): a recipe with an unknown output is skipped, the next one is still loaded. */
    @Test
    void theGameSkipsARecipeWithAnUnknownOutput() {
        String json = "{\"recipes\":[{\"input\":\"minecraft:stone\",\"chance\":1.0,\"outputs\":[{\"symbol\":\"Zz\",\"amount\":1}]},"
            + "{\"input\":\"minecraft:dirt\",\"chance\":1.0,\"outputs\":[{\"symbol\":\"H\",\"amount\":1}]}]}";
        DecomposerRecipes table = new DecomposerRecipes();
        assertEquals(1, DecomposerRecipeLoader.load(table, new StringReader(json), true));
        assertEquals(null, table.get("minecraft:stone"));
        assertEquals("minecraft:dirt", table.get("minecraft:dirt").input());
    }
}
