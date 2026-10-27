package minechem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import minechem.apparatus.synthesis.OreSynthesisRules;
import minechem.apparatus.synthesis.OreSynthesisRules.Rule;
import minechem.apparatus.synthesis.SynthesisRecipes;
import minechem.apparatus.synthesis.SynthesisRecipes.Ingredient;
import org.junit.jupiter.api.Test;

/** The OreDictionary rules: fixed names, first substring match, entry limit, settings. */
public class OreSynthesisRulesTest {

    private static Ingredient amount(int amount) {
        return new Ingredient(null, amount);
    }

    @Test
    public void aFixedNameMatchesIgnoringCase() {
        SynthesisRecipes table = new SynthesisRecipes();
        Rule iron = new Rule("ingotIron", null, null, false, 300, List.of(amount(1)));
        int added = OreSynthesisRules.apply(table, List.of(iron), Map.of("ingotiron", List.of("minecraft:iron_ingot")), Set.of());
        assertEquals(1, added);
        assertNotNull(table.get("minecraft:iron_ingot"));
    }

    @Test
    public void theFirstMatchingSubstringRuleWins() {
        SynthesisRecipes table = new SynthesisRecipes();
        Rule blue = new Rule(null, "Blue", null, false, 300, List.of(amount(1)));
        Rule anyBlue = new Rule(null, "Bl", null, false, 999, List.of(amount(2)));
        OreSynthesisRules.apply(table, List.of(blue, anyBlue), Map.of("xychoriumBlue", List.of("mod:blue_gem")), Set.of());
        assertEquals(300, table.get("mod:blue_gem").energy());
    }

    @Test
    public void aSubstringRuleIsNotUsedForAFixedName() {
        SynthesisRecipes table = new SynthesisRecipes();
        Rule fixed = new Rule("gemApatite", null, null, false, 1000, List.of(amount(1)));
        int added = OreSynthesisRules.apply(table, List.of(fixed), Map.of("ingotIron", List.of("minecraft:iron_ingot")), Set.of());
        assertEquals(0, added);
        assertNull(table.get("minecraft:iron_ingot"));
    }

    @Test
    public void atMostEightEntriesGetARecipe() {
        SynthesisRecipes table = new SynthesisRecipes();
        Rule gem = new Rule(null, "gemApatite", null, false, 1000, List.of(amount(1)));
        List<String> ids = List.of("a:1", "a:2", "a:3", "a:4", "a:5", "a:6", "a:7", "a:8", "a:9", "a:10");
        int added = OreSynthesisRules.apply(table, List.of(gem), Map.of("gemApatite", ids), Set.of());
        assertEquals(SynthesisRecipes.ORE_ENTRY_LIMIT, added);
        assertNull(table.get("a:9"));
    }

    @Test
    public void aRuleThatNeedsASettingIsOffWithoutIt() {
        SynthesisRecipes table = new SynthesisRecipes();
        Rule duskberry = new Rule(null, "cropDuskberry", "recreationalChemicalEffects", false, 1000, List.of(amount(1)));
        Map<String, List<String>> names = Map.of("cropDuskberry", List.of("mod:duskberry"));
        assertEquals(0, OreSynthesisRules.apply(table, List.of(duskberry), names, Set.of()));
        assertEquals(1, OreSynthesisRules.apply(table, List.of(duskberry), names, Set.of("recreationalChemicalEffects")));
    }

    @Test
    public void eachEntryShiftsTheGridByOneSlot() {
        SynthesisRecipes table = new SynthesisRecipes();
        Rule rule = new Rule(null, "gemApatite", null, false, 1000, List.of(amount(3), amount(5)));
        OreSynthesisRules.apply(table, List.of(rule), Map.of("gemApatite", List.of("m:first", "m:second")), Set.of());
        List<Ingredient> first = table.get("m:first").ingredients();
        List<Ingredient> second = table.get("m:second").ingredients();
        assertEquals(3, first.get(0).amount());
        assertEquals(5, first.get(1).amount());
        assertEquals(3, second.get(1).amount());
        assertEquals(5, second.get(2).amount());
    }

    @Test
    public void aRecipeAlreadyInTheTableIsNotAddedAgain() {
        SynthesisRecipes table = new SynthesisRecipes();
        Rule iron = new Rule("ingotIron", null, null, false, 300, List.of(amount(1)));
        Map<String, List<String>> names = Map.of("ingotIron", List.of("minecraft:iron_ingot"));
        assertEquals(1, OreSynthesisRules.apply(table, List.of(iron), names, Set.of()));
        assertEquals(0, OreSynthesisRules.apply(table, List.of(iron), names, Set.of()));
    }

    /**
     * A rebind (OreSynthesisBinder.bind on a tag update) removes the recipes of the previous bind first: an item that left its
     * tag loses its recipe, and a recipe of the same output that came from elsewhere is kept.
     */
    @Test
    public void aRebindDropsTheRecipesOfItemsThatLeftTheTag() {
        SynthesisRecipes table = new SynthesisRecipes();
        SynthesisRecipes.Recipe fromApi = new SynthesisRecipes.Recipe("m:kept", 1, false, 5, List.of(amount(9)));
        table.add(fromApi);
        Rule gem = new Rule(null, "gemApatite", null, false, 1000, List.of(amount(1)));
        List<SynthesisRecipes.Recipe> bound = new java.util.ArrayList<>();
        OreSynthesisRules.apply(table, List.of(gem), Map.of("gemApatite", List.of("m:first", "m:second", "m:kept")), Set.of(), bound);
        assertEquals(2, bound.size());
        for (SynthesisRecipes.Recipe recipe : bound) {
            table.remove(recipe);
        }
        bound.clear();
        OreSynthesisRules.apply(table, List.of(gem), Map.of("gemApatite", List.of("m:second")), Set.of(), bound);
        assertNull(table.get("m:first"), "the item that left the tag kept its recipe");
        assertNotNull(table.get("m:second"));
        assertEquals(fromApi, table.get("m:kept"), "the recipe from elsewhere was removed");
    }
}
