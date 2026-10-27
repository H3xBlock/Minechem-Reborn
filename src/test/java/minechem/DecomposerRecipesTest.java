package minechem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Random;
import minechem.apparatus.decomposer.DecomposerRecipes;
import minechem.apparatus.decomposer.DecomposerRecipes.Output;
import minechem.apparatus.decomposer.DecomposerRecipes.Recipe;
import minechem.chemical.ChemicalBase;
import minechem.chemical.ChemicalBase;
import minechem.helper.Jenkins;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DecomposerRecipesTest {
    private static ChemicalBase SILICON;
    private static ChemicalBase OXYGEN;

    @BeforeAll
    static void loadElements() throws Exception {
        // Same data as ChemistryDataTest: the bundled elementsData.json, copied to a scratch config directory.
        minechem.handler.ElementHandler.init(java.nio.file.Files.createTempDirectory("minechem-config"), true);
        SILICON = Jenkins.get("Si");
        OXYGEN = Jenkins.get("O");
    }

    @Test
    void firstRecipeForAKeyWins() {
        DecomposerRecipes table = new DecomposerRecipes();
        assertTrue(table.add(new Recipe("minecraft:stone", 1.0F, List.of(new Output(SILICON, 1)))));
        assertFalse(table.add(new Recipe("minecraft:stone", 1.0F, List.of(new Output(OXYGEN, 2)))), "a second recipe for the same key is ignored");
        assertEquals(1, table.size());
        assertEquals(SILICON, table.roll("minecraft:stone").get(0).chemical());
    }

    @Test
    void blacklistedInputIsNeverAdded() {
        DecomposerRecipes table = new DecomposerRecipes();
        table.blacklist("minecraft:bedrock");
        assertFalse(table.add(new Recipe("minecraft:bedrock", 1.0F, List.of(new Output(OXYGEN, 1)))));
        assertNull(table.get("minecraft:bedrock"));
    }

    @Test
    void chanceZeroNeverGivesOutputAndOneAlwaysDoes() {
        DecomposerRecipes never = new DecomposerRecipes(new Random(1));
        never.add(new Recipe("minecraft:gravel", 0.0F, List.of(new Output(OXYGEN, 1))));
        for (int i = 0; i < 50; i++) {
            assertNull(never.roll("minecraft:gravel"), "a zero chance must always miss");
        }
        DecomposerRecipes always = new DecomposerRecipes(new Random(1));
        always.add(new Recipe("minecraft:sand", 1.0F, List.of(new Output(SILICON, 1))));
        for (int i = 0; i < 50; i++) {
            assertEquals(1, always.roll("minecraft:sand").size(), "a full chance must always give the output");
        }
    }

    @Test
    void chanceMissIsRoughlyTheComplement() {
        DecomposerRecipes table = new DecomposerRecipes(new Random(42));
        table.add(new Recipe("minecraft:dirt", 0.4F, List.of(new Output(OXYGEN, 1))));
        int hits = 0;
        for (int i = 0; i < 10_000; i++) {
            if (table.roll("minecraft:dirt") != null) {
                hits++;
            }
        }
        assertTrue(hits > 3800 && hits < 4200, "40% chance should hit about 4000 times in 10000, was " + hits);
    }

    @Test
    void selectRecipePicksEachAlternativeAndRespectsItsChance() {
        Recipe first = new Recipe("minecraft:cobblestone", 1.0F, List.of(new Output(SILICON, 1)));
        Recipe second = new Recipe("minecraft:cobblestone", 1.0F, List.of(new Output(OXYGEN, 1)));
        DecomposerRecipes table = new DecomposerRecipes(new Random(7));
        table.add(Recipe.select("minecraft:cobblestone", 1.0F, List.of(first, second)));
        int siliconHits = 0;
        int oxygenHits = 0;
        for (int i = 0; i < 2_000; i++) {
            ChemicalBase out = table.roll("minecraft:cobblestone").get(0).chemical();
            if (out == SILICON) siliconHits++; else if (out == OXYGEN) oxygenHits++;
        }
        assertTrue(siliconHits > 900 && oxygenHits > 900, "both alternatives must be picked, got " + siliconHits + " / " + oxygenHits);

        DecomposerRecipes never = new DecomposerRecipes(new Random(7));
        never.add(Recipe.select("minecraft:gravel", 0.0F, List.of(first, second)));
        for (int i = 0; i < 50; i++) {
            assertNull(never.roll("minecraft:gravel"), "a select recipe with chance 0 must always miss");
        }
    }

    @Test
    void listRecipeRollsEachComponentByItsWeight() {
        DecomposerRecipes table = new DecomposerRecipes(new Random(3));
        table.add(new Recipe("minecraft:paper", 1.0F, List.of(new Output(SILICON, 1))));
        table.add(new Recipe("minecraft:leather", 1.0F, List.of(new Output(OXYGEN, 1))));
        // Two whole papers, and leather at a 50% chance.
        table.add(Recipe.composite("minecraft:book", java.util.Map.of("minecraft:paper", 2.0, "minecraft:leather", 0.5)));
        int oxygen = 0;
        int silicon = 0;
        for (int i = 0; i < 2_000; i++) {
            for (Output out : table.roll("minecraft:book")) {
                if (out.chemical() == SILICON) silicon++; else oxygen++;
            }
        }
        assertEquals(4_000, silicon, "two whole papers roll the paper recipe twice per book");
        assertTrue(oxygen > 900 && oxygen < 1_100, "leather at 0.5 gives about 1000 of 2000, was " + oxygen);
    }

    @Test
    void listRecipeIgnoresComponentsWithoutARecipe() {
        DecomposerRecipes table = new DecomposerRecipes(new Random(3));
        table.add(Recipe.composite("minecraft:thing", java.util.Map.of("minecraft:unknown", 3.0)));
        assertEquals(0, table.roll("minecraft:thing").size());
    }

    @Test
    void fluidRecipeIsAddedOnlyWhenTheFluidExists() {
        DecomposerRecipes table = new DecomposerRecipes(new Random(5));
        Recipe water = new Recipe(DecomposerRecipes.fluidKey("water"), 1.0F, List.of(new Output(OXYGEN, 1)));
        Recipe missing = new Recipe(DecomposerRecipes.fluidKey("iron.molten"), 1.0F, List.of(new Output(SILICON, 16)));
        java.util.Set<String> registered = java.util.Set.of("water");
        assertTrue(table.addFluidIfRegistered("water", water, registered::contains));
        assertFalse(table.addFluidIfRegistered("iron.molten", missing, registered::contains), "a mod fluid that is not present is not added");
        assertEquals(1, table.size());
        assertEquals(OXYGEN, table.roll(DecomposerRecipes.fluidKey("water")).get(0).chemical());
    }

    @Test
    void unknownInputGivesNoRecipe() {
        assertNull(new DecomposerRecipes().roll("minecraft:nothing"));
    }

    @Test
    void fluidRecipeKeepsTheAmountItUsesUpAndItemRecipesUseNone() {
        DecomposerRecipes table = new DecomposerRecipes();
        Recipe water = new Recipe(DecomposerRecipes.fluidKey("water"), 1.0F, List.of(new Output(OXYGEN, 1))).withFluidAmount(1000);
        assertTrue(table.add(water));
        assertEquals(1000, table.get(DecomposerRecipes.fluidKey("water")).fluidAmount(), "the tank must hold 1000 mB");
        Recipe item = new Recipe("Si", 1.0F, List.of(new Output(SILICON, 1)));
        assertEquals(0, item.fluidAmount(), "an item recipe uses no fluid");
    }
}
