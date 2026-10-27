package minechem.handler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import minechem.Compendium;
import minechem.Config;
import minechem.helper.DataFileHelper;
import net.neoforged.fml.loading.FMLPaths;
import minechem.helper.LogHelper;
import minechem.registry.ElementRegistry;

/**
 * Reads config/minechem/data/elementsData.json, copied from the bundled file (see DataFileHelper and
 * Config.useDefaultElements).
 */
public class ElementHandler {
    public static void init() {
        init(FMLPaths.CONFIGDIR.get(), Config.useDefaultElements());
    }

    /** Loads the elements from configDir/minechem/data; the game calls {@link #init()}. */
    public static void init(Path configDir, boolean useDefault) {
        String name = Compendium.Data.elementsJson;
        try (InputStream stream = DataFileHelper.open(configDir, ElementHandler.class, name, useDefault)) {
            readFrom(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            // A user's file (useDefaultElements = false) that is not valid JSON: logged, the game starts without its elements.
            LogHelper.exception("Cannot read " + name, e);
        }
    }

    static void readFrom(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> elementsSet = root.entrySet();
        int count = 0;
        for (Map.Entry<String, JsonElement> elementEntry : elementsSet) {
            if (!elementEntry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject elementObject = elementEntry.getValue().getAsJsonObject();
            try {
                ElementRegistry.getInstance().registerElement(
                    Integer.parseInt(elementEntry.getKey()),
                    elementObject.get("longName").getAsString(),
                    elementObject.get("shortName").getAsString(),
                    elementObject.get("form").getAsString(),
                    elementObject.get("type").getAsString());
                count++;
            } catch (RuntimeException e) {
                // A malformed entry of a user's file (a missing field, a number that is not one): skipped and logged.
                LogHelper.warn("Element Parsing Error: " + elementEntry.getKey() + " is malformed and skipped (" + e + ")");
            }
        }
        LogHelper.info("Total of " + count + " elements registered");
    }
}
