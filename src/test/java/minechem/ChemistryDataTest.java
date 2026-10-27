package minechem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import minechem.chemical.Element;
import minechem.chemical.Molecule;
import minechem.handler.ElementHandler;
import minechem.handler.MoleculeHandler;
import minechem.helper.ColourHelper;
import minechem.helper.Jenkins;
import minechem.registry.ElementRegistry;
import minechem.registry.MoleculeRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Checks the chemistry data against the bundled JSON data.
 * Expected counts come from the JSON itself, so a lost or mis-parsed entry makes these fail.
 */
class ChemistryDataTest {
    private static JsonObject readBundled(String name) throws Exception {
        try (InputStream stream = ChemistryDataTest.class.getResourceAsStream(Compendium.Data.dataPrefix + name)) {
            assertNotNull(stream, "bundled data missing: " + name);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @BeforeAll
    static void loadData() throws Exception {
        // The game loads from config/minechem/data; the test copies the bundled files into a scratch config directory.
        Path configDir = Files.createTempDirectory("minechem-config");
        ElementHandler.init(configDir, true);
        MoleculeHandler.init(configDir, true);
    }

    @Test
    void everyBundledElementIsRegistered() throws Exception {
        JsonObject json = readBundled(Compendium.Data.elementsJson);
        long expected = json.entrySet().stream().filter(e -> e.getValue().isJsonObject()).count();
        assertEquals(expected, ElementRegistry.getInstance().getElements().size());
        assertEquals(118, expected, "the periodic table has 118 elements");
    }

    /**
     * A user's data file (useDefaultMolecules or useDefaultElements set to false) that is not valid JSON, or has an entry
     * without its fields, is logged and skipped: it does not stop the game's start.
     */
    @Test
    void aMalformedUserDataFileDoesNotThrow() throws Exception {
        Path configDir = Files.createTempDirectory("minechem-bad-config");
        Path data = configDir.resolve("minechem").resolve("data");
        Files.createDirectories(data);
        Files.writeString(data.resolve(Compendium.Data.moleculesJson), "{ not json");
        Files.writeString(data.resolve(Compendium.Data.elementsJson), "{\"200\": {\"longName\": \"Broken\"}}");
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> MoleculeHandler.init(configDir, false));
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> ElementHandler.init(configDir, false));
        Files.writeString(data.resolve(Compendium.Data.moleculesJson), "{\"Broken Molecule\": {\"Form\": \"liquid\"}}");
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> MoleculeHandler.init(configDir, false));
        assertEquals(null, Jenkins.get("Broken Molecule"));
    }

    @Test
    void hydrogenIsFoundByNumberAndSymbol() {
        Element byNumber = Jenkins.get(1);
        assertNotNull(byNumber);
        assertEquals("H", byNumber.shortName);
        Element bySymbol = Jenkins.get("H");
        assertEquals(1, bySymbol.atomicNumber);
    }

    @Test
    void everyBundledMoleculeIsRegistered() throws Exception {
        JsonObject json = readBundled(Compendium.Data.moleculesJson);
        long expected = json.entrySet().stream().filter(e -> e.getValue().isJsonObject()).count();
        // Some molecules reference others by name and are only parsed on a later pass; all must end up registered.
        java.util.List<String> missing = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, com.google.gson.JsonElement> e : json.entrySet()) {
            if (e.getValue().isJsonObject() && MoleculeRegistry.getInstance().getMoleculeByName(e.getKey()) == null) {
                missing.add(e.getKey() + " = " + e.getValue().getAsJsonObject().get("Formula").getAsString());
            }
        }
        // Every entry has elements that exist, so none may be missing.
        assertEquals(java.util.List.of(), missing, "every molecule in the data must be registered");
        assertEquals(expected, MoleculeRegistry.getInstance().getMolecules().size());
    }

    @Test
    void moleculeKeepsItsSecondColour() {
        // Carbon Dioxide has colour triples (0.5, 0.5, 0.5) and (0.25, 0.25, 0.25) in the bundled data.
        minechem.chemical.Molecule co2 = (minechem.chemical.Molecule) Jenkins.get("carbon dioxide");
        assertNotNull(co2, "carbon dioxide must be registered from moleculesData.json");
        assertEquals(0x808080, co2.getColour());
        assertEquals(0x404040, co2.getColour2(), "the second colour must come from Colour2, not the first");
    }

    @Test
    void waterFormulaIsParsedFromItsName() {
        Molecule water = Jenkins.get("water");
        assertNotNull(water, "water must be registered from moleculesData.json");
        assertEquals("H2O", water.getFormula());
        assertTrue(!water.isElement());
    }

    @Test
    void chemicalItemListCoversElementsAndMolecules() {
        int chemicals = Jenkins.getAll().size();
        assertEquals(ElementRegistry.getInstance().getElements().size() + MoleculeRegistry.getInstance().getMolecules().size(), chemicals);
    }

    /**
     * ColourHelper.RGB(float...): the cast binds before the multiply, so mid-range floats collapse
     * to 0 or 255. This test pins that behaviour so a "fix" is a deliberate choice, not an accident.
     */
    @Test
    void floatRgbIsKept() {
        assertEquals(ColourHelper.RGBA(0, 0, 0, 255), ColourHelper.RGB(0.5f, 0.5f, 0.5f));
        assertEquals(ColourHelper.RGBA(255, 255, 255, 255), ColourHelper.RGB(1.0f, 1.0f, 1.0f));
    }
}
