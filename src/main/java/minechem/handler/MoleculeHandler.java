package minechem.handler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import minechem.Compendium;
import minechem.Config;
import minechem.helper.DataFileHelper;
import net.neoforged.fml.loading.FMLPaths;
import minechem.helper.ColourHelper;
import minechem.helper.LogHelper;
import minechem.registry.MoleculeRegistry;

/**
 * Molecules can reference other molecules by name, so unparsed entries are retried up to 5 passes.
 */
public class MoleculeHandler {
    public static void init() {
        init(FMLPaths.CONFIGDIR.get(), Config.useDefaultMolecules());
    }

    /** Loads the molecules from configDir/minechem/data; the game calls {@link #init()}. */
    public static void init(Path configDir, boolean useDefault) {
        String name = Compendium.Data.moleculesJson;
        try (InputStream stream = DataFileHelper.open(configDir, MoleculeHandler.class, name, useDefault)) {
            readFrom(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            // A user's file (useDefaultMolecules = false) that is not valid JSON: logged, the game starts without its molecules.
            LogHelper.exception("Cannot read " + name, e);
        }
    }

    static void readFrom(Reader reader) {
        JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
        readFromObject(object.entrySet(), 0);
        LogHelper.info("Total of " + MoleculeRegistry.getInstance().getMolecules().size() + " molecules registered");
    }

    private static void readFromObject(Set<Map.Entry<String, JsonElement>> elementSet, int run) {
        if (elementSet.isEmpty()) {
            return;
        } else if (run > 4) {
            for (Map.Entry<String, JsonElement> moleculeEntry : elementSet) {
                LogHelper.warn("Molecule Parsing Error: " + moleculeEntry.getKey() + " cannot be parsed.");
            }
        } else {
            Map<String, JsonElement> unparsed = new HashMap<String, JsonElement>();
            for (Map.Entry<String, JsonElement> moleculeEntry : elementSet) {
                if (!moleculeEntry.getValue().isJsonObject()) {
                    continue;
                }
                try {
                    if (readEntry(moleculeEntry)) {
                        unparsed.put(moleculeEntry.getKey(), moleculeEntry.getValue());
                    }
                } catch (RuntimeException e) {
                    // A malformed entry of a user's file (a missing Formula, a value of the wrong type): skipped and logged.
                    LogHelper.warn("Molecule Parsing Error: " + moleculeEntry.getKey() + " is malformed and skipped (" + e + ")");
                }
            }
            readFromObject(unparsed.entrySet(), run + 1);
        }
    }

    /** Registers one molecule entry; true when it must be retried (it names a molecule not registered yet). */
    private static boolean readEntry(Map.Entry<String, JsonElement> moleculeEntry) {
        JsonObject elementObject = moleculeEntry.getValue().getAsJsonObject();
        String form = "liquid";
        if (elementObject.has("Form")) {
            // The state is given directly.
            form = elementObject.get("Form").getAsString();
        } else if (elementObject.has("MeltingPt") && elementObject.get("MeltingPt").getAsDouble() > 25) {
            form = "solid";
        } else if (elementObject.has("BoilingPt") && elementObject.get("BoilingPt").getAsDouble() < 25) {
            form = "gas";
        }
        int colour = 0;
        if (elementObject.has("Colour") && elementObject.get("Colour").isJsonPrimitive()) {
            JsonPrimitive cInput = elementObject.getAsJsonPrimitive("Colour");
            if (cInput.isString()) {
                colour = ColourHelper.RGB(cInput.getAsString());
            } else if (cInput.isNumber()) {
                colour = cInput.getAsInt();
            }
        }
        int colour2 = colour;
        if (elementObject.has("Colour2") && elementObject.get("Colour2").isJsonPrimitive()) {
            colour2 = elementObject.get("Colour2").getAsInt();
        }
        return MoleculeRegistry.getInstance().registerMolecule(
            moleculeEntry.getKey(),
            form,
            colour,
            colour2,
            elementObject.get("Formula").getAsString());
    }
}
