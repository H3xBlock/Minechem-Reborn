package minechem;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import minechem.apparatus.decomposer.DecomposerRecipeLoader;
import minechem.apparatus.decomposer.DecomposerRecipes;
import minechem.handler.ElementHandler;
import minechem.handler.MoleculeHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The products of a decomposition, which the microscope discovers with the inspected item. */
class DecomposerProductsTest {
    @BeforeAll
    static void loadChemistry() throws IOException {
        var configDir = Files.createTempDirectory("minechem-config");
        ElementHandler.init(configDir, true);
        MoleculeHandler.init(configDir, true);
    }

    @Test
    void fermentedSpiderEyeGivesPoloniumAndEthylAlcohol() {
        DecomposerRecipes table = new DecomposerRecipes();
        DecomposerRecipeLoader.loadBundled(table);
        List<String> products = table.productKeys("minecraft:fermented_spider_eye");
        assertTrue(products.contains("Polonium"), "the bundled recipe gives polonium (Po): " + products);
        assertTrue(products.contains("Ethyl Alcohol"), "and ethyl alcohol: " + products);
    }

    @Test
    void anInputWithoutARecipeHasNoProducts() {
        DecomposerRecipes table = new DecomposerRecipes();
        DecomposerRecipeLoader.loadBundled(table);
        assertTrue(table.productKeys("minecraft:no_such_item").isEmpty(), "an unknown input gives no products");
    }
}
