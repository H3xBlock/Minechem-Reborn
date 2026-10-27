package minechem.apparatus.synthesis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import minechem.helper.LogHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Connects the OreDictionary rule names to the game's item tags. The names are read from the common tags (the "c"
 * namespace), so a tag such as c:ingots/iron is read as the name "ingotIron", and c:gems/apatite as "gemApatite".
 *
 * <p>Limits: the names come from the common tags only, so mod-specific names such as the xychorium ones have no tag and no
 * recipe; the items of a name come in tag order; and the recipes are added when the tags are updated (server start, or a
 * reload), because the tags are not bound before that.
 */
public final class OreSynthesisBinder {
    /** The setting the duskberry recipe needs (config recreationalChemicalEffects, on by default). */
    static final String RECREATIONAL_EFFECTS = "recreationalChemicalEffects";

    private static List<OreSynthesisRules.Rule> rules;

    private OreSynthesisBinder() {
    }

    /**
     * The name given to a common tag: "ingots/iron" is "ingotIron", "gems/apatite" is "gemApatite",
     * "storage_blocks/iron" is "blockIron" (the common tag of a block of metal), and an underscore starts a new word, so
     * "ingots/dark_steel" is "ingotDarkSteel".
     */
    public static String oreNameOf(String tagPath) {
        String[] parts = tagPath.split("/");
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0 && part.equals("storage_blocks")) {
                part = "block";
            } else if (i == 0 && part.endsWith("s")) {
                part = part.substring(0, part.length() - 1);
            }
            for (String word : part.split("_")) {
                if (!word.isEmpty()) {
                    name.append(name.length() == 0 ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1));
                }
            }
        }
        return name.toString();
    }

    /** The items of every common tag that has some, by the OreDictionary name the tag gives (tag order, as the game lists them). */
    public static Map<String, List<String>> itemIdsByOreName() {
        Map<String, List<String>> itemIdsByOreName = new LinkedHashMap<>();
        for (TagKey<Item> tag : BuiltInRegistries.ITEM.listTagIds().toList()) {
            if (!tag.location().getNamespace().equals("c")) {
                continue;
            }
            List<String> ids = itemIdsOfTag(tag);
            if (!ids.isEmpty()) {
                itemIdsByOreName.put(oreNameOf(tag.location().getPath()), ids);
            }
        }
        return itemIdsByOreName;
    }

    /** The registry ids of the items of one item tag, given as "namespace:path" (for tags with no common name). */
    public static List<String> itemIdsOfTag(String tagId) {
        String[] parts = tagId.split(":", 2);
        return itemIdsOfTag(TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(parts[0], parts[1])));
    }

    static List<String> itemIdsOfTag(TagKey<Item> tag) {
        List<String> ids = new ArrayList<>();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            ids.add(BuiltInRegistries.ITEM.getKey(holder.value()).toString());
        }
        return ids;
    }

    /** The recipes the last bind added, removed again before the next bind so the table follows the current tags. */
    private static final List<SynthesisRecipes.Recipe> bound = new ArrayList<>();

    /**
     * Adds the OreDictionary recipes from the current item tags. Called when the tags are updated. The recipes of the previous
     * bind are removed first (those still in the table as they were added), so a tag that lost an item, or a new world in the
     * same game, does not keep the old recipes.
     */
    public static synchronized int bind() {
        if (rules == null) {
            rules = OreSynthesisRules.loadBundled();
        }
        for (SynthesisRecipes.Recipe recipe : bound) {
            SynthesisRecipes.RECIPES.remove(recipe);
        }
        bound.clear();
        Map<String, List<String>> itemIdsByOreName = itemIdsByOreName();
        Set<String> settings = minechem.Config.recreationalChemicalEffects() ? Set.of(RECREATIONAL_EFFECTS) : Set.of();
        int added = OreSynthesisRules.apply(SynthesisRecipes.RECIPES, rules, itemIdsByOreName, settings, bound);
        LogHelper.info("OreDictionary synthesis: " + itemIdsByOreName.size() + " names, " + added + " new recipes");
        return added;
    }
}
