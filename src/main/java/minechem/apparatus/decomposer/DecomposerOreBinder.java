package minechem.apparatus.decomposer;

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
import minechem.apparatus.decomposer.DecomposerRecipes.Output;
import minechem.apparatus.decomposer.DecomposerRecipes.Recipe;
import minechem.apparatus.synthesis.OreSynthesisBinder;
import minechem.chemical.ChemicalBase;
import minechem.helper.Jenkins;
import minechem.helper.LogHelper;

/**
 * Connects the ore-name decomposer rules to the item tags. Each rule is keyed by an ore name; each item of the name's
 * tag gets its own recipe, keyed by its id. A rule with explicit tags (logWood, plankWood) reads those tags instead.
 *
 * <p>The names come from the NeoForge common tags, so a name with no tag (the
 * mod-specific names of the xychorium and of the other mods) has no recipe; recipes are added on each tag update, not at mod setup;
 * a recipe is kept for the first input that has one, so a static recipe of the table wins over an ore-name rule for the same
 * item.
 */
public final class DecomposerOreBinder {
    /** The bundled rules, on the classpath. */
    public static final String RESOURCE = "/assets/minechem/data/decomposerOreRecipes.json";
    /** The setting the duskberry rule needs (config recreationalChemicalEffects, on by default). */
    static final String RECREATIONAL_EFFECTS = "recreationalChemicalEffects";

    /** One rule: a fixed name or a substring, the setting it needs (or null), its explicit tags, and its outputs. */
    public record Rule(String name, String contains, String requires, List<String> tags, List<Output> outputs) {
        public Rule {
            tags = List.copyOf(tags);
            outputs = List.copyOf(outputs);
        }
    }

    private static List<Rule> rules;

    private DecomposerOreBinder() {
    }

    /** The recipes the last bind added, removed again before the next bind so the table follows the current tags. */
    private static final List<Recipe> bound = new ArrayList<>();

    /**
     * Adds the OreDictionary decomposer recipes from the current item tags. Called when the tags are updated. The recipes of the
     * previous bind are removed first (those still in the table as they were added).
     */
    public static synchronized int bind() {
        if (rules == null) {
            rules = loadBundled();
        }
        for (Recipe recipe : bound) {
            DecomposerBlockEntity.RECIPES.remove(recipe);
        }
        bound.clear();
        Map<String, List<String>> itemIdsByOreName = OreSynthesisBinder.itemIdsByOreName();
        Set<String> settings = minechem.Config.recreationalChemicalEffects() ? Set.of(RECREATIONAL_EFFECTS) : Set.of();
        int added = 0;
        List<String> unresolved = new ArrayList<>();
        for (Rule rule : rules) {
            if (rule.name() == null || (rule.requires() != null && !settings.contains(rule.requires()))) {
                continue;
            }
            List<String> items = rule.tags().isEmpty()
                ? itemIdsByOreName.getOrDefault(rule.name(), List.of())
                : itemIdsOfTags(rule.tags());
            if (items.isEmpty()) {
                unresolved.add(rule.name());
            }
            added += addRecipes(rule, items);
        }
        for (Map.Entry<String, List<String>> entry : itemIdsByOreName.entrySet()) {
            Rule matched = firstContaining(entry.getKey());
            if (matched != null && (matched.requires() == null || settings.contains(matched.requires()))) {
                added += addRecipes(matched, entry.getValue());
            }
        }
        LogHelper.info("OreDictionary decomposer: " + rules.size() + " rules, " + added + " new recipes; " + unresolved.size()
            + " rules without a tag in this game");
        return added;
    }

    /** The first substring rule that the name contains (if/else order: the first match decides). */
    private static Rule firstContaining(String oreName) {
        for (Rule rule : rules) {
            if (rule.contains() != null && oreName.contains(rule.contains())) {
                return rule;
            }
        }
        return null;
    }

    private static int addRecipes(Rule rule, List<String> itemIds) {
        int added = 0;
        for (String itemId : itemIds) {
            Recipe recipe = new Recipe(itemId, 1.0F, rule.outputs());
            if (DecomposerBlockEntity.RECIPES.add(recipe)) {
                bound.add(recipe);
                added++;
            }
        }
        return added;
    }

    private static List<String> itemIdsOfTags(List<String> tags) {
        List<String> ids = new ArrayList<>();
        for (String tag : tags) {
            ids.addAll(OreSynthesisBinder.itemIdsOfTag(tag));
        }
        return ids;
    }

    /** Reads the bundled rules (decomposerOreRecipes.json). */
    public static List<Rule> loadBundled() {
        try (InputStream stream = DecomposerOreBinder.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled data file " + RESOURCE);
            }
            return parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + RESOURCE, e);
        }
    }

    static List<Rule> parse(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        List<Rule> result = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("rules")) {
            JsonObject entry = element.getAsJsonObject();
            String name = entry.has("name") ? entry.get("name").getAsString() : null;
            String contains = entry.has("contains") ? entry.get("contains").getAsString() : null;
            String requires = entry.has("requires") ? entry.get("requires").getAsString() : null;
            List<String> tags = new ArrayList<>();
            if (entry.has("tags")) {
                for (JsonElement tag : entry.getAsJsonArray("tags")) {
                    tags.add(tag.getAsString());
                }
            }
            String label = name != null ? name : contains;
            List<Output> outputs = new ArrayList<>();
            for (JsonElement out : entry.getAsJsonArray("outputs")) {
                outputs.add(output(label, out.getAsJsonObject()));
            }
            result.add(new Rule(name, contains, requires, tags, outputs));
        }
        return result;
    }

    private static Output output(String label, JsonObject out) {
        String id = out.has("symbol") ? out.get("symbol").getAsString() : out.get("name").getAsString();
        ChemicalBase chemical = Jenkins.get(id);
        if (chemical == null) {
            throw new IllegalStateException("Unknown decomposer output " + id + " for OreDictionary rule " + label);
        }
        return new Output(chemical, out.get("amount").getAsInt());
    }
}
