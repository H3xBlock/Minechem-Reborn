package minechem.apparatus.synthesis;

import minechem.helper.ModRequirement;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import minechem.apparatus.synthesis.SynthesisRecipes.Ingredient;
import minechem.apparatus.synthesis.SynthesisRecipes.Recipe;
import minechem.chemical.ChemicalBase;
import minechem.helper.Jenkins;

/**
 * Loads the synthesis recipes from the bundled data file, in file order (the first recipe for an output wins). The file
 * holds the static recipes, the molecule recipes and the derived recipes, with items mapped to 26.1 names. Recipes whose
 * output is an OreDictionary name are not in the file; OreSynthesisBinder adds them.
 *
 * <p>An ingredient symbol or name that does not resolve is an error: a recipe with a missing ingredient would be wrong.
 */
public final class SynthesisRecipeLoader {
    /** The bundled data file, on the classpath. */
    public static final String RESOURCE = "/assets/minechem/data/synthesisRecipes.json";

    private SynthesisRecipeLoader() {
    }

    /** Adds the recipes of the bundled file to the table. */
    public static void loadBundled(SynthesisRecipes table) {
        try (InputStream stream = SynthesisRecipeLoader.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled data file " + RESOURCE);
            }
            load(table, new InputStreamReader(stream, StandardCharsets.UTF_8), true);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + RESOURCE, e);
        }
    }

    /** Adds the recipes of a file to the table and returns how many were added (a duplicate output is not added). */
    public static int load(SynthesisRecipes table, Reader reader) {
        return load(table, reader, false);
    }

    /**
     * As {@link #load(SynthesisRecipes, Reader)}. With {@code skipUnknown}, a recipe whose ingredient does not resolve is
     * logged and skipped instead of failing: the game loads the bundled recipes this way, since a user's element or molecule
     * file (useDefaultElements or useDefaultMolecules set to false) may lack a chemical the bundled recipes use.
     */
    public static int load(SynthesisRecipes table, Reader reader, boolean skipUnknown) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        int added = 0;
        for (JsonElement element : root.getAsJsonArray("recipes")) {
            JsonObject entry = element.getAsJsonObject();
            if (!ModRequirement.satisfied(entry)) {
                continue;
            }
            Recipe recipe;
            try {
                recipe = recipeOf(entry);
            } catch (IllegalStateException e) {
                if (!skipUnknown) {
                    throw e;
                }
                minechem.helper.LogHelper.warn("Synthesis recipe skipped: " + e.getMessage());
                continue;
            }
            if (table.add(recipe)) {
                added++;
            }
        }
        return added;
    }

    private static Recipe recipeOf(JsonObject entry) {
        String output = entry.get("output").getAsString();
        List<Ingredient> ingredients = new ArrayList<>();
        for (JsonElement chemical : entry.getAsJsonArray("chemicals")) {
            ingredients.add(ingredient(output, chemical));
        }
        return new Recipe(
            output,
            entry.get("count").getAsInt(),
            entry.get("shaped").getAsBoolean(),
            entry.get("energy").getAsInt(),
            ingredients);
    }

    static Ingredient ingredient(String output, JsonElement element) {
        if (element.isJsonNull()) {
            return null;
        }
        JsonObject chemical = element.getAsJsonObject();
        String id = chemical.has("symbol") ? chemical.get("symbol").getAsString() : chemical.get("name").getAsString();
        ChemicalBase resolved = Jenkins.get(id);
        if (resolved == null) {
            throw new IllegalStateException("Unknown synthesis ingredient " + id + " for output " + output);
        }
        return new Ingredient(resolved, chemical.get("amount").getAsInt());
    }
}
