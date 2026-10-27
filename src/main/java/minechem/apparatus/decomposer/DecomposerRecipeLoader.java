package minechem.apparatus.decomposer;

import minechem.helper.ModRequirement;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import minechem.apparatus.decomposer.DecomposerRecipes.Output;
import minechem.apparatus.decomposer.DecomposerRecipes.Recipe;
import minechem.chemical.ChemicalBase;
import minechem.helper.Jenkins;

/**
 * Loads the decomposer recipes from the bundled data file, in file order (the first recipe for a key wins). Plain, select and composite (list) recipes are read, including the fluid recipes ("fluid:" keys) and the mod items
 * of the optional mods ("requires": the mod id, see ModRequirement).
 *
 * <p>An output symbol or name that does not resolve is an error: a recipe with a missing output would silently change the
 * result of a decomposition.
 */
public final class DecomposerRecipeLoader {
    /** The bundled data file, on the classpath. */
    public static final String RESOURCE = "/assets/minechem/data/decomposerRecipes.json";

    private DecomposerRecipeLoader() {
    }

    /** Adds the recipes of the bundled file to the table. */
    public static void loadBundled(DecomposerRecipes table) {
        try (InputStream stream = DecomposerRecipeLoader.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled data file " + RESOURCE);
            }
            load(table, new InputStreamReader(stream, StandardCharsets.UTF_8), true);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + RESOURCE, e);
        }
    }

    /** Adds the recipes of a file to the table and returns how many were added (a duplicate input is not added). */
    public static int load(DecomposerRecipes table, Reader reader) {
        return load(table, reader, false);
    }

    /**
     * As {@link #load(DecomposerRecipes, Reader)}. With {@code skipUnknown}, a recipe whose output does not resolve is logged
     * and skipped instead of failing: the game loads the bundled recipes this way, since a player's element or molecule file
     * (useDefaultElements or useDefaultMolecules set to false) may lack a chemical the bundled recipes use.
     */
    public static int load(DecomposerRecipes table, Reader reader, boolean skipUnknown) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        int added = 0;
        for (JsonElement element : root.getAsJsonArray("recipes")) {
            JsonObject entry = element.getAsJsonObject();
            if (!ModRequirement.satisfied(entry)) {
                continue;
            }
            // The brown mushroom and nether wart recipes are under the recreationalChemicalEffects setting.
            if (entry.has("setting") && entry.get("setting").getAsString().equals("recreationalChemicalEffects")
                && !minechem.Config.recreationalChemicalEffects()) {
                continue;
            }
            Recipe recipe;
            try {
                recipe = recipeOf(entry);
            } catch (IllegalStateException e) {
                if (!skipUnknown) {
                    throw e;
                }
                minechem.helper.LogHelper.warn("Decomposer recipe skipped: " + e.getMessage());
                continue;
            }
            if (table.add(recipe)) {
                added++;
            }
        }
        return added;
    }

    private static Recipe recipeOf(JsonObject entry) {
        String kind = entry.has("kind") ? entry.get("kind").getAsString() : "plain";
        String input = entry.get("input").getAsString();
        Recipe recipe = switch (kind) {
            case "plain" -> new Recipe(input, entry.get("chance").getAsFloat(), outputs(input, entry.getAsJsonArray("outputs")));
            case "select" -> {
                List<Recipe> alternatives = new ArrayList<>();
                for (JsonElement alternative : entry.getAsJsonArray("alternatives")) {
                    alternatives.add(new Recipe(input, 1.0F, outputs(input, alternative.getAsJsonArray())));
                }
                yield Recipe.select(input, entry.get("chance").getAsFloat(), alternatives);
            }
            case "composite" -> {
                Map<String, Double> components = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> component : entry.getAsJsonObject("components").entrySet()) {
                    components.put(component.getKey(), component.getValue().getAsDouble());
                }
                yield Recipe.composite(input, components);
            }
            default -> throw new IllegalStateException("Unknown decomposer recipe kind " + kind + " for input " + input);
        };
        int fluidAmount = entry.has("fluidAmount") ? entry.get("fluidAmount").getAsInt() : 0;
        return recipe.withFluidAmount(fluidAmount);
    }

    private static List<Output> outputs(String input, JsonArray array) {
        List<Output> outputs = new ArrayList<>();
        for (JsonElement out : array) {
            outputs.add(output(input, out.getAsJsonObject()));
        }
        return outputs;
    }

    private static Output output(String input, JsonObject out) {
        String id = out.has("symbol") ? out.get("symbol").getAsString() : out.get("name").getAsString();
        ChemicalBase chemical = Jenkins.get(id);
        if (chemical == null) {
            throw new IllegalStateException("Unknown decomposer output " + id + " for input " + input);
        }
        return new Output(chemical, out.get("amount").getAsInt());
    }
}
