package minechem.apparatus.synthesis;

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
import java.util.List;
import java.util.Map;
import java.util.Set;
import minechem.apparatus.synthesis.SynthesisRecipes.Ingredient;
import minechem.apparatus.synthesis.SynthesisRecipes.Recipe;

/**
 * The OreDictionary synthesis rules. A rule is either a fixed name (an ingot's OreDictionary name), or a text that the name
 * must contain. For each name, the first matching substring rule applies; the fixed names apply on their own.
 *
 * <p>Each rule makes one recipe per entry of the name, at most {@link SynthesisRecipes#ORE_ENTRY_LIMIT} entries, each entry
 * with its own grid position. The output is the entry's item, one of its own.
 */
public final class OreSynthesisRules {
    public static final String RESOURCE = "/assets/minechem/data/synthesisOreRecipes.json";

    /** One rule: a fixed name or a substring, the setting it needs (or null), its shape, energy and chemicals. */
    public record Rule(String name, String contains, String requires, boolean shaped, int energy, List<Ingredient> chemicals) {
        public Rule {
            chemicals = List.copyOf(chemicals);
        }
    }

    private OreSynthesisRules() {
    }

    /** Reads the bundled rules (synthesisOreRecipes.json). */
    public static List<Rule> loadBundled() {
        try (InputStream stream = OreSynthesisRules.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing " + RESOURCE);
            }
            return parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + RESOURCE, e);
        }
    }

    public static List<Rule> parse(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        JsonArray rules = root.getAsJsonArray("rules");
        List<Rule> result = new ArrayList<>();
        for (JsonElement element : rules) {
            JsonObject entry = element.getAsJsonObject();
            String name = entry.has("name") ? entry.get("name").getAsString() : null;
            String contains = entry.has("contains") ? entry.get("contains").getAsString() : null;
            String requires = entry.has("requires") ? entry.get("requires").getAsString() : null;
            String label = name != null ? name : contains;
            List<Ingredient> chemicals = new ArrayList<>();
            for (JsonElement chemical : entry.getAsJsonArray("chemicals")) {
                chemicals.add(SynthesisRecipeLoader.ingredient(label, chemical));
            }
            result.add(new Rule(name, contains, requires, entry.get("shaped").getAsBoolean(), entry.get("energy").getAsInt(), chemicals));
        }
        return result;
    }

    /** Whether a fixed rule is for this OreDictionary name (the names are matched ignoring case). */
    static boolean isFixedFor(Rule rule, String oreName) {
        return rule.name() != null && rule.name().equalsIgnoreCase(oreName);
    }

    /**
     * Adds the recipes of the rules to the table. {@code itemIdsByOreName} gives, for each OreDictionary name in the order the
     * game lists them, the registry ids of its items in order. {@code settings} is the set of settings that are on.
     *
     * @return how many recipes were added
     */
    public static int apply(SynthesisRecipes table, List<Rule> rules, Map<String, List<String>> itemIdsByOreName, Set<String> settings) {
        return apply(table, rules, itemIdsByOreName, settings, new java.util.ArrayList<>());
    }

    /** As {@link #apply(SynthesisRecipes, List, Map, Set)}, and each recipe added is also put in {@code addedRecipes}. */
    public static int apply(SynthesisRecipes table, List<Rule> rules, Map<String, List<String>> itemIdsByOreName, Set<String> settings,
                            List<Recipe> addedRecipes) {
        int added = 0;
        for (Rule rule : rules) {
            if (rule.name() == null) {
                continue;
            }
            for (Map.Entry<String, List<String>> entry : itemIdsByOreName.entrySet()) {
                if (isFixedFor(rule, entry.getKey())) {
                    added += addEntries(table, rule, entry.getValue(), addedRecipes);
                }
            }
        }
        for (Map.Entry<String, List<String>> entry : itemIdsByOreName.entrySet()) {
            Rule matched = null;
            for (Rule rule : rules) {
                if (rule.contains() != null && entry.getKey().contains(rule.contains())) {
                    matched = rule;
                    break;
                }
            }
            if (matched != null && (matched.requires() == null || settings.contains(matched.requires()))) {
                added += addEntries(table, matched, entry.getValue(), addedRecipes);
            }
        }
        return added;
    }

    private static int addEntries(SynthesisRecipes table, Rule rule, List<String> itemIds, List<Recipe> addedRecipes) {
        int added = 0;
        int entries = Math.min(SynthesisRecipes.ORE_ENTRY_LIMIT, itemIds.size());
        for (int entry = 0; entry < entries; entry++) {
            List<Ingredient> grid = SynthesisRecipes.oreGrid(rule.chemicals(), entry);
            Recipe recipe = new Recipe(itemIds.get(entry), 1, rule.shaped(), rule.energy(), grid);
            if (table.add(recipe)) {
                addedRecipes.add(recipe);
                added++;
            }
        }
        return added;
    }
}
