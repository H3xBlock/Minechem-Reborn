package minechem.gametest;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import minechem.Compendium;
import minechem.apparatus.decomposer.DecomposerBlockEntity;
import minechem.apparatus.leadedchest.LeadedChestBlockEntity;
import minechem.apparatus.decomposer.DecomposerMenu;
import minechem.apparatus.leadedchest.LeadedChestBlockEntity;
import minechem.apparatus.leadedchest.LeadedChestMenu;
import minechem.apparatus.synthesis.SynthesisMenu;
import minechem.apparatus.microscope.MicroscopeBlockEntity;
import minechem.apparatus.microscope.MicroscopeMenu;
import minechem.apparatus.microscope.MicroscopeRules;
import minechem.apparatus.synthesis.SynthesisBlockEntity;
import minechem.apparatus.synthesis.SynthesisRecipes;
import minechem.helper.Jenkins;
import minechem.recipes.ElementIngredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import minechem.fluid.ElementFluids;
import minechem.polytool.PolytoolItem;
import minechem.recipes.BucketEmptyRecipe;
import minechem.recipes.BucketFillRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import java.util.ArrayList;
import java.util.List;
import minechem.fluid.MoleculeFluids;
import minechem.item.chemical.ChemicalItem;
import minechem.registry.BlockRegistry;
import minechem.registry.ItemRegistry;
import minechem.registry.MobEffectRegistry;
import minechem.registry.SoundRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import minechem.registry.CreativeTabRegistry;
import minechem.registry.MenuRegistry;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.Blocks;
import minechem.apparatus.multiblock.BlueprintProjectorBlock;
import minechem.apparatus.multiblock.BlueprintProjectorBlockEntity;
import minechem.apparatus.multiblock.FissionReactorBlockEntity;
import minechem.apparatus.multiblock.GhostBlock;
import minechem.apparatus.multiblock.FusionReactorBlockEntity;
import minechem.apparatus.multiblock.ProjectionFrame;
import minechem.apparatus.multiblock.ReactorBlueprint;
import minechem.apparatus.multiblock.ReactorWallBlockEntity;
import minechem.apparatus.prefab.block.BasicBlockContainer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.minecraft.gametest.framework.GameTestInstance;
import minechem.chemical.ChemicalBase;
import minechem.chemical.Element;
import minechem.chemical.Molecule;
import minechem.radiation.RadiationEnum;
import minechem.fluid.ElementFluids;
import minechem.recipes.BucketEmptyRecipe;
import minechem.recipes.BucketFillRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import java.util.ArrayList;
import java.util.List;
import minechem.fluid.MoleculeFluids;
import net.minecraft.world.item.BucketItem;
import minechem.helper.Jenkins;
import minechem.item.chemical.ChemicalItem;
import minechem.radiation.RadiationRules;
import minechem.radiation.RadiationTicker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * GameTests for the mod (run by the gameTestServer run configuration). Each test is code-only: its body is a Java consumer,
 * so the test instances are registered in memory and never read from a data file.
 *
 * <p>The tests cover that the machine blocks do not occlude their neighbours (the floor faces next to them are kept), that
 * the machines have their block entities, and that the OreDictionary recipes are bound from the item tags.
 */
public final class MinechemGameTests {
    private static final String STRUCTURE = "minecraft:empty";

    /** The data of a decoded code test (a client decodes the registry it is sent; the tests only run on the gameTestServer). */
    private static final TestData<Holder<TestEnvironmentDefinition<?>>> PLACEHOLDER =
        new TestData<>(Holder.direct(new TestEnvironmentDefinition.AllOf()), Identifier.parse(STRUCTURE), 1, 0, false);

    /**
     * The code-only test instance type. The registry sync serializes each test instance by its type key, so the type must be
     * registered (an unregistered codec fails the client join).
     */
    public static final DeferredRegister<MapCodec<? extends GameTestInstance>> TYPES = DeferredRegister.create(Registries.TEST_INSTANCE_TYPE, Compendium.Naming.id);
    public static final DeferredHolder<MapCodec<? extends GameTestInstance>, MapCodec<Check>> CODE_TYPE = TYPES.register("code", () -> Check.CODEC);

    private MinechemGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(Identifier.fromNamespaceAndPath(Compendium.Naming.id, "default"));
        registerTest(event, name("synthesis_block_entity"), new Check(data(environment), MinechemGameTests::synthesisHasBlockEntity));
        registerTest(event, name("machines_do_not_occlude"), new Check(data(environment), MinechemGameTests::machinesDoNotOcclude));
        registerTest(event, name("ore_recipes_bound_from_tags"), new Check(data(environment), MinechemGameTests::oreRecipesBound));
        registerTest(event, name("machines_open_menus"), new Check(data(environment), MinechemGameTests::machinesOpenMenus));
        registerTest(event, name("creative_tab_has_machines"), new Check(data(environment), MinechemGameTests::creativeTabHasMachines));
        registerTest(event, name("decomposer_item_sides"), new Check(data(environment), MinechemGameTests::decomposerItemSides));
        registerTest(event, name("molecule_drink_applies_effects"), new Check(data(environment), MinechemGameTests::moleculeDrinkAppliesEffects));
        registerTest(event, name("hydrogen_is_furnace_fuel"), new Check(data(environment), MinechemGameTests::hydrogenIsFurnaceFuel));
        registerTest(event, name("vial_pours_liquid"), new Check(data(environment), MinechemGameTests::vialPoursLiquid));
        registerTest(event, name("fission_reactor_links_walls"), new Check(data(alone(event, "reactor_links")), MinechemGameTests::fissionReactorLinksWalls));
        registerTest(event, name("fission_splits_element"), new Check(data(alone(event, "reactor_split")), MinechemGameTests::fissionSplitsElement));
        registerTest(event, name("fusion_fuses_elements"), new Check(data(alone(event, "reactor_fusion")), MinechemGameTests::fusionFusesElements));
        registerTest(event, name("projector_builds_reactor"), new Check(data(alone(event, "reactor_projector")), MinechemGameTests::projectorBuildsReactor));
        registerTest(event, name("dungeon_chests_hold_blueprint"), new Check(data(alone(event, "dungeon_loot")), MinechemGameTests::dungeonChestsHoldBlueprint));
        registerTest(event, name("lens_and_projector_recipes"), new Check(data(alone(event, "recipes")), MinechemGameTests::lensAndProjectorRecipes));
        registerTest(event, name("machine_recipes_craft"), new Check(data(alone(event, "machine_recipes")), MinechemGameTests::machineRecipesCraft));
        registerTest(event, name("microscope_and_chest_recipes"), new Check(data(alone(event, "microscope_recipes")), MinechemGameTests::microscopeAndChestRecipes));
        registerTest(event, name("element_recipes_craft_walls"), new Check(data(alone(event, "element_recipes")), MinechemGameTests::elementRecipesCraftWalls));
        registerTest(event, name("projector_sound_registered"), new Check(data(alone(event, "projector_sound")), MinechemGameTests::projectorSoundIsRegistered));
        registerTest(event, name("radiation_decays_element"), new Check(data(alone(event, "radiation")), MinechemGameTests::radiationDecaysElement));
        registerTest(event, name("radiation_leaded_chest_keeps_stacks"), new Check(data(alone(event, "radiation_chest")), MinechemGameTests::radiationLeadedChestKeepsStacks));
        registerTest(event, name("radiation_decomposer_shields_player"), new Check(data(alone(event, "radiation_shield")), MinechemGameTests::radiationDecomposerShieldsPlayer));
        registerTest(event, name("radiation_molecule_decays_into_atoms"), new Check(data(alone(event, "radiation_molecule")), MinechemGameTests::radiationMoleculeDecaysIntoAtoms));
        registerTest(event, name("molecule_bucket_is_bucket"), new Check(data(alone(event, "molecule_bucket")), MinechemGameTests::moleculeBucketIsBucket));
        registerTest(event, name("element_bucket_is_bucket"), new Check(data(alone(event, "element_bucket")), MinechemGameTests::elementBucketIsBucket));
        registerTest(event, name("radiation_element_bucket_decays"), new Check(data(alone(event, "radiation_bucket")), MinechemGameTests::radiationElementBucketDecays));
        registerTest(event, name("bucket_recipes_fill_and_empty"), new Check(data(alone(event, "bucket_recipes")), MinechemGameTests::bucketRecipesFillAndEmpty));
        registerTest(event, name("radioactive_fluid_keeps_state_on_pickup"), new Check(data(alone(event, "fluid_pickup")), MinechemGameTests::radioactiveFluidKeepsStateOnPickup));
        registerTest(event, name("polytool_alloy_strength"), new Check(data(alone(event, "polytool_strength")), MinechemGameTests::polytoolAlloyStrength));
        registerTest(event, name("polytool_alloy_infusion_validity"), new Check(data(alone(event, "polytool_infusion")), MinechemGameTests::polytoolAlloyInfusionValidity));
        registerTest(event, name("polytool_hit_damage"), new Check(data(alone(event, "polytool_hit")), MinechemGameTests::polytoolHitDamage));
        registerTest(event, name("radioactive_element_tooltip"), new Check(data(alone(event, "radioactive_tooltip")), MinechemGameTests::radioactiveElementTooltip));
        registerTest(event, name("radiation_tooltip_time_left"), new Check(data(alone(event, "radiation_time_left")), MinechemGameTests::radiationTooltipTimeLeft));
        registerTest(event, name("polytool_synthesis_recipe_listed"), new Check(data(alone(event, "polytool_recipe")), MinechemGameTests::polytoolSynthesisRecipeListed));
        registerTest(event, name("fluid_block_is_walkable"), new Check(data(alone(event, "fluid_walk")), MinechemGameTests::fluidBlockIsWalkable));
        registerTest(event, name("fluids_swim_like_water"), new Check(data(alone(event, "fluid_swim")), MinechemGameTests::fluidsSwimLikeWater));
        registerTest(event, name("player_in_chemical_liquid_is_in_water"), new Check(data(alone(event, "fluid_in_water")), MinechemGameTests::playerInChemicalLiquidIsInWater));
        registerTest(event, name("player_in_water_control"), new Check(data(alone(event, "fluid_water_control")), MinechemGameTests::playerInWaterControl));
        registerTest(event, name("chemical_liquid_affects_living_entity"), new Check(data(alone(event, "fluid_effects")), MinechemGameTests::chemicalLiquidAffectsLivingEntity));
        registerTest(event, name("solid_liquid_does_not_flow"), new Check(data(alone(event, "fluid_solid")), MinechemGameTests::solidLiquidDoesNotFlow));
        registerTest(event, name("reaction_fluid_meets_fluid"), new Check(data(alone(event, "reaction_fluid")), MinechemGameTests::reactionFluidMeetsFluid));
        registerTest(event, name("reaction_item_meets_fluid"), new Check(data(alone(event, "reaction_item")), MinechemGameTests::reactionItemMeetsFluid));
        registerTest(event, name("reaction_item_water_bucket_control"), new Check(data(alone(event, "reaction_item_control")), MinechemGameTests::reactionItemWaterBucketControl));
        registerTest(event, name("reaction_tnt_explodes_by_fire"), new Check(data(alone(event, "reaction_tnt")), MinechemGameTests::reactionTntExplodesByFire));
        registerTest(event, name("reaction_tnt_without_fire_stays"), new Check(data(alone(event, "reaction_tnt_control")), MinechemGameTests::reactionTntWithoutFireStays));
        registerTest(event, name("gas_rises"), new Check(data(alone(event, "gas_rises")), MinechemGameTests::gasRises));
        registerTest(event, name("gas_does_not_flow_down"), new Check(data(alone(event, "gas_down")), MinechemGameTests::gasDoesNotFlowDown));
        registerTest(event, name("gas_stops_at_build_limit"),new Check(data(alone(event, "gas_top")), MinechemGameTests::gasStopsAtBuildLimit));
        registerTest(event, name("gas_stack_stays_bounded"), new Check(data(alone(event, "gas_stack")), MinechemGameTests::gasStackStaysBounded));
        registerTest(event, name("api_adds_reaction_and_recipe"), new Check(data(environment), MinechemGameTests::apiAddsReactionAndRecipe));
        registerTest(event, name("addon_recipes_follow_mod_presence"), new Check(data(environment), MinechemGameTests::addonRecipesFollowModPresence));
        registerTest(event, name("computercraft_turtle_upgrade"), new Check(data(environment), MinechemGameTests::computercraftTurtleUpgrade));
        registerTest(event, name("hazmat_recipes_exist"), new Check(data(alone(event, "hazmat_recipes")), MinechemGameTests::hazmatRecipesExist));
        registerTest(event, name("hazmat_suit_shields_player"), new Check(data(alone(event, "hazmat_suit")), MinechemGameTests::hazmatSuitShieldsPlayer));
        registerTest(event, name("decomposer_ore_rule_from_tag"), new Check(data(alone(event, "decomposer_ore")), MinechemGameTests::decomposerOreRuleFromTag));
        registerTest(event, name("new_resources_decompose"), new Check(data(alone(event, "new_resources")), MinechemGameTests::newResourcesDecompose));
        registerTest(event, name("ore_dictionary_defaults"), new Check(data(environment), MinechemGameTests::oreDictionaryDefaults));
        registerTest(event, name("uranium_ore_generates_in_overworld"), new Check(data(alone(event, "uranium_ore")), MinechemGameTests::uraniumOreGeneratesInOverworld));
        registerTest(event, name("uranium_ore_feature_places_veins"), new Check(data(alone(event, "uranium_ore_feature")), MinechemGameTests::uraniumOreFeaturePlacesVeins));
        registerTest(event, name("projector_lit_with_blueprint"),new Check(data(alone(event, "projector_lit")), MinechemGameTests::projectorLitWithBlueprint));
        registerTest(event, name("synthesis_grid_holds_ghosts"), new Check(data(alone(event, "synthesis_ghosts")), MinechemGameTests::synthesisGridHoldsGhosts));
        registerTest(event, name("power_use_config_gates_synthesis"), new Check(data(alone(event, "power_use")), MinechemGameTests::powerUseConfigGatesSynthesis));
        registerTest(event, name("polytool_keeps_durability"), new Check(data(alone(event, "polytool_durability")), MinechemGameTests::polytoolKeepsDurability));
        registerTest(event, name("synthesis_automation_skips_grid"), new Check(data(alone(event, "synthesis_sides")), MinechemGameTests::synthesisAutomationSkipsGrid));
        registerTest(event, name("synthesis_hopper_takes_batch_output"), new Check(data(alone(event, "synthesis_hopper")), MinechemGameTests::synthesisHopperTakesBatchOutput));
        registerTest(event, name("synthesis_hoppers_follow_sides"), new Check(data(alone(event, "synthesis_hopper_sides")), MinechemGameTests::synthesisHoppersFollowSides));
        registerTest(event, name("synthesis_recipe_survives_reload"), new Check(data(alone(event, "synthesis_reload")), MinechemGameTests::synthesisRecipeSurvivesReload));
        registerTest(event, name("radioactive_tooltip_uses_context_level"), new Check(data(environment), MinechemGameTests::radioactiveTooltipUsesContextLevel));
        registerTest(event, name("radiation_update_does_not_reequip"), new Check(data(environment), MinechemGameTests::radiationUpdateDoesNotReequip));
        registerTest(event, name("reactor_wall_follows_allow_automation"), new Check(data(alone(event, "reactor_automation")), MinechemGameTests::reactorWallFollowsAllowAutomation));
        registerTest(event, name("config_defaults_and_recreational_recipes"), new Check(data(environment), MinechemGameTests::configDefaultsAndRecreationalRecipes));
        registerTest(event, name("radioactive_fluid_entry_goes_with_block"), new Check(data(alone(event, "fluid_entry")), MinechemGameTests::radioactiveFluidEntryGoesWithBlock));
        registerTest(event, name("journal_menu_uses_its_hand"), new Check(data(environment), MinechemGameTests::journalMenuUsesItsHand));
        registerTest(event, name("microscope_discovers_products"), new Check(data(environment), MinechemGameTests::microscopeDiscoversProducts));
        registerTest(event, name("menu_factory_without_block_entity"), new Check(data(alone(event, "menu_factory")), MinechemGameTests::menuFactoryWithoutBlockEntity));
    }

    /** The polytool is made in the synthesis machine: its recipe is in the table JEI reads. */
    private static void polytoolSynthesisRecipeListed(GameTestHelper helper) {
        minechem.apparatus.synthesis.SynthesisRecipes.Recipe recipe = minechem.apparatus.synthesis.SynthesisRecipes.RECIPES.get("minechem:polytool");
        helper.assertTrue(recipe != null, "the polytool recipe should be in the synthesis table");
        helper.assertTrue(recipe.ingredients().stream().filter(java.util.Objects::nonNull).count() == 4, "the polytool recipe has four ingredients");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath("minechem", "polytool")), "minechem:polytool should be a registered item");
        helper.succeed();
    }

    /**
     * The newer resources have a decomposition: raw materials and blocks, copper blocks and nuggets, the deepslate ores,
     * netherite (iridium, the rare element of the deep and the hot) and the other new items (real-world analogues). The netherite scrap gives four of iridium (atomic number 77).
     */
    private static void newResourcesDecompose(GameTestHelper helper) {
        for (String id : java.util.List.of("minecraft:raw_iron", "minecraft:raw_iron_block", "minecraft:raw_copper", "minecraft:raw_gold_block",
            "minecraft:copper_block", "minecraft:oxidized_copper", "minecraft:copper_nugget", "minecraft:deepslate_iron_ore",
            "minecraft:deepslate_copper_ore", "minecraft:netherite_ingot", "minecraft:netherite_block", "minecraft:ancient_debris",
            "minecraft:netherite_scrap", "minecraft:shulker_shell", "minecraft:nautilus_shell", "minecraft:calcite", "minecraft:amethyst_shard",
            "minecraft:chorus_fruit", "minecraft:glow_ink_sac", "minecraft:tuff", "minecraft:phantom_membrane")) {
            helper.assertTrue(DecomposerBlockEntity.RECIPES.get(id) != null, id + " should have a decomposer recipe");
        }
        minechem.apparatus.decomposer.DecomposerRecipes.Recipe scrap = DecomposerBlockEntity.RECIPES.get("minecraft:netherite_scrap");
        helper.assertTrue(scrap.outputs().size() == 1 && scrap.outputs().get(0).chemical() instanceof Element element
            && element.atomicNumber == 77 && scrap.outputs().get(0).amount() == 4, "netherite scrap: 4 of Ir");
        minechem.apparatus.decomposer.DecomposerRecipes.Recipe raw = DecomposerBlockEntity.RECIPES.get("minecraft:raw_iron_block");
        helper.assertTrue(raw.outputs().get(0).amount() == 144, "a raw iron block: 144 of Fe");
        // Uranium ore decomposes into 48 of uranium (U); the block is the uranium ore item.
        minechem.apparatus.decomposer.DecomposerRecipes.Recipe uranium = DecomposerBlockEntity.RECIPES.get("minechem:uranium_ore");
        helper.assertTrue(uranium != null && uranium.outputs().size() == 1 && uranium.outputs().get(0).chemical() instanceof Element uranic
            && uranic.shortName.equals("U") && uranium.outputs().get(0).amount() == 48, "uranium ore: 48 of U");
        helper.succeed();
    }

    /**
     * Uranium ore is generated in the surface worlds (the overworld). The biome modifier
     * (minechem:uranium_ore, on the overworld biomes) must give the code-built uranium ore feature to the test's biome, in the
     * underground ores step. The block is registered and mined with a pickaxe.
     */
    private static void uraniumOreGeneratesInOverworld(GameTestHelper helper) {
        net.minecraft.world.level.biome.Biome biome = helper.getLevel().getBiome(helper.absolutePos(new BlockPos(0, 1, 0))).value();
        java.util.List<net.minecraft.core.HolderSet<net.minecraft.world.level.levelgen.placement.PlacedFeature>> steps = biome.getGenerationSettings().features();
        int ores = net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_ORES.ordinal();
        helper.assertTrue(steps.size() > ores && steps.get(ores).stream()
                .anyMatch(placed -> placed.value().feature().value().feature() == minechem.registry.WorldgenRegistry.URANIUM_ORE.get()),
            "the uranium ore feature should be in the underground ores step of the overworld biome");
        helper.assertTrue(BlockRegistry.URANIUM_ORE.get().defaultBlockState().requiresCorrectToolForDrops(),
            "uranium ore should need a pickaxe to drop");
        helper.succeed();
    }

    /**
     * The uranium ore feature on the real server level, with the default config (generateOre true, uraniumoredensity 5,
     * uraniumOreClusterSize 3): far from the other tests, two by two chunks of stone get veins of uranium ore from the feature run
     * at the first chunk's corner, as the chunk decoration does; with generateOre off, the same stone gets none.
     */
    private static void uraniumOreFeaturePlacesVeins(GameTestHelper helper) {
        helper.assertTrue(minechem.Config.generateOre() && minechem.Config.uraniumOreDensity() == 5 && minechem.Config.uraniumOreClusterSize() == 3,
            "the worldgen config should have the expected defaults");
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        helper.assertTrue(level.dimension() == net.minecraft.world.level.Level.OVERWORLD, "the tests run in the overworld");
        int minX = 4096 * 16;
        int minZ = 4096 * 16;
        int minY = Math.max(level.getMinY(), -4);
        BlockPos origin = new BlockPos(minX, level.getMinY(), minZ);
        boolean before = minechem.Config.GENERATE_ORE.get();
        try {
            int ores = uraniumOreAfterFeature(helper, level, origin, minY, 12345L);
            helper.assertTrue(ores > 0, "the feature should place uranium ore in stone with the default config");
            minechem.Config.GENERATE_ORE.set(false);
            helper.assertTrue(uraniumOreAfterFeature(helper, level, origin, minY, 12345L) == 0, "with generateOre off, no uranium ore");
        } finally {
            minechem.Config.GENERATE_ORE.set(before);
        }
        helper.succeed();
    }

    /** Fills the two by two chunks from origin with stone (y minY to 52), runs the feature at origin, counts the uranium ore. */
    private static int uraniumOreAfterFeature(GameTestHelper helper, net.minecraft.server.level.ServerLevel level, BlockPos origin, int minY, long seed) {
        net.minecraft.world.level.block.state.BlockState stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 32; dx++) {
            for (int dz = 0; dz < 32; dz++) {
                for (int y = minY; y <= 52; y++) {
                    level.setBlock(pos.set(origin.getX() + dx, y, origin.getZ() + dz), stone, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                }
            }
        }
        minechem.registry.WorldgenRegistry.URANIUM_ORE.get().place(new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(
            java.util.Optional.empty(), level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(seed), origin,
            net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE));
        int ores = 0;
        for (int dx = 0; dx < 32; dx++) {
            for (int dz = 0; dz < 32; dz++) {
                for (int y = minY; y <= 52; y++) {
                    if (level.getBlockState(pos.set(origin.getX() + dx, y, origin.getZ() + dz)).is(BlockRegistry.URANIUM_ORE.get())) {
                        // x is 16 * chunkX + nextInt(16), shifted by 8 in WorldGenMinable, plus the vein's reach.
                        helper.assertTrue(dx >= 4 && dx <= 26 && dz >= 4 && dz <= 26 && y <= 51, "uranium ore out of the vein range at " + pos);
                        ores++;
                    }
                }
            }
        }
        return ores;
    }

    /**
     * The OreDictionary name ingotCopper decomposes into 16 copper. Copper has no static recipe, so the
     * recipe comes from the common tag of the copper ingot, through the OreDictionary rule.
     */
    private static void decomposerOreRuleFromTag(GameTestHelper helper) {
        minechem.apparatus.decomposer.DecomposerRecipes.Recipe recipe = DecomposerBlockEntity.RECIPES.get("minecraft:copper_ingot");
        helper.assertTrue(recipe != null, "the ingotCopper rule should give minecraft:copper_ingot a recipe");
        helper.assertTrue(recipe.outputs().size() == 1 && recipe.outputs().get(0).chemical() instanceof Element element
            && element.atomicNumber == 29 && recipe.outputs().get(0).amount() == 16, "copper ingot: 16 of Cu");
        helper.succeed();
    }

    /** A radioactive element in the inventory does its damage and decays into the element below. */
    private static void radiationDecaysElement(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        // The mock player is in creative mode, which ignores damage: the test plays in survival.
        player.setGameMode(GameType.SURVIVAL);
        ItemStack stack = ChemicalItem.stackFor(Jenkins.get(84));
        stack.setCount(3);
        player.getInventory().setItem(0, stack);
        long life = RadiationRules.ofElement("Po").getLife();
        RadiationTicker.update(player, null, 0L, "minecraft:overworld");
        helper.assertTrue(player.getInventory().getItem(0).getCount() == 3, "the decay has not started, so nothing changed");
        int damage = RadiationTicker.update(player, null, life + 1, "minecraft:overworld");
        helper.assertTrue(damage == 6 * 3, "radioactive polonium does 6 per item, so 18 for three: got " + damage);
        ItemStack after = player.getInventory().getItem(0);
        helper.assertTrue(ChemicalItem.getChemicalBase(after) instanceof Element element && element.atomicNumber == 83, "polonium should decay into bismuth");
        helper.assertTrue(after.getCount() == 3, "the stack size is kept");
        // The mock server player is invulnerable until its client has loaded (ServerPlayer.isInvulnerableTo checks
        // hasClientLoaded), so its health stays at 20 here. The damage value above is what this test checks; a real player takes it.
        helper.succeed();
    }

    /** The INoDecay interface: a leaded chest keeps its radioactive stacks, which do not decay while it is open. */
    private static void radiationLeadedChestKeepsStacks(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, BlockRegistry.LEADED_CHEST.get());
        LeadedChestBlockEntity chest = helper.getBlockEntity(pos, LeadedChestBlockEntity.class);
        chest.setItem(0, ChemicalItem.stackFor(Jenkins.get(84)));
        long life = RadiationRules.ofElement("Po").getLife();
        RadiationTicker.update(player, chest, 0L, "minecraft:overworld");
        int damage = RadiationTicker.update(player, chest, life + 1, "minecraft:overworld");
        helper.assertTrue(damage == 0, "nothing decays in a leaded chest, so no damage: got " + damage);
        helper.assertTrue(ChemicalItem.getChemicalBase(chest.getItem(0)) instanceof Element element && element.atomicNumber == 84,
            "the polonium stays polonium in a leaded chest");
        helper.succeed();
    }

    /**
     * While the decomposer (factor 0.4) is open, the damage to the player is reduced: 18 becomes 11
     * (18 times 0.6, rounded).
     */
    private static void radiationDecomposerShieldsPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, BlockRegistry.DECOMPOSER.get());
        DecomposerBlockEntity decomposer = helper.getBlockEntity(pos, DecomposerBlockEntity.class);
        ItemStack stack = ChemicalItem.stackFor(Jenkins.get(84));
        stack.setCount(3);
        player.getInventory().setItem(0, stack);
        long life = RadiationRules.ofElement("Po").getLife();
        RadiationTicker.update(player, decomposer, 0L, "minecraft:overworld");
        int damage = RadiationTicker.update(player, decomposer, life + 1, "minecraft:overworld");
        helper.assertTrue(damage == 11, "the decomposer shield (0.4) leaves 11 of 18: got " + damage);
        helper.succeed();
    }

    /** A placed chemical liquid block lets an entity walk into it, as water does: no collision, and the entity's box fits. */
    private static void fluidBlockIsWalkable(GameTestHelper helper) {
        net.minecraft.world.level.block.Block chemical = minechem.fluid.ElementFluids.entries().get(0).block().get();
        BlockPos pos = new BlockPos(1, 2, 1);
        BlockPos abs = helper.absolutePos(pos);
        net.minecraft.world.level.block.state.BlockState water = net.minecraft.world.level.block.Blocks.WATER.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState state = chemical.defaultBlockState();
        helper.setBlock(pos, state);
        net.minecraft.world.level.block.state.BlockState placed = helper.getLevel().getBlockState(abs);
        helper.assertTrue(placed.getCollisionShape(helper.getLevel(), abs).isEmpty(),
            "the chemical liquid should have no collision shape, but has " + placed.getCollisionShape(helper.getLevel(), abs));
        helper.assertTrue(water.getCollisionShape(helper.getLevel(), abs).isEmpty(), "water is walkable");
        helper.assertTrue(!placed.getFluidState().isEmpty(), "the block should hold a fluid");
        helper.assertTrue(placed.getFluidState().is(net.minecraft.tags.FluidTags.WATER),
            "the chemical liquid should answer to the water tag, which the engine reads to decide that an entity is in water");
        // The tag lists every chemical fluid (data/minecraft/tags/fluid/water.json, generated from the molecule and element data).
        int chemicalFluids = 0;
        for (net.minecraft.world.level.material.Fluid fluid : BuiltInRegistries.FLUID) {
            if (fluid instanceof minechem.fluid.MoleculeFluid) {
                chemicalFluids++;
                helper.assertTrue(fluid.defaultFluidState().is(net.minecraft.tags.FluidTags.WATER),
                    "the chemical fluid " + BuiltInRegistries.FLUID.getKey(fluid) + " should be in the water tag");
            }
        }
        helper.assertTrue(chemicalFluids == 580, "the water tag should list the 580 chemical fluids, but there are " + chemicalFluids);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        net.minecraft.world.phys.AABB box = player.getBoundingBox().move(abs.getX() + 0.5 - player.getX(), 0, abs.getZ() + 0.5 - player.getZ());
        helper.assertTrue(helper.getLevel().noCollision(player, box), "the player's box should fit in the chemical liquid");
        helper.succeed();
    }

    /** A placed molecule or element liquid moves a swimmer like water: it can be swum in, drowns, and slows the same way. */
    private static void fluidsSwimLikeWater(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        net.neoforged.neoforge.fluids.FluidType water = net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value();
        net.neoforged.neoforge.fluids.FluidType molecule = minechem.fluid.MoleculeFluids.MOLECULE_TYPE.get();
        net.neoforged.neoforge.fluids.FluidType element = minechem.fluid.ElementFluids.ELEMENT_TYPE.get();
        for (net.neoforged.neoforge.fluids.FluidType type : java.util.List.of(molecule, element)) {
            helper.assertTrue(type.canSwim(player), "a chemical liquid should let the player swim");
            helper.assertTrue(type.canDrownIn(player), "a chemical liquid should drown the player like water");
            helper.assertTrue(type.motionScale(player) == water.motionScale(player),
                "a chemical liquid should slow the player like water: " + type.motionScale(player) + " against " + water.motionScale(player));
        }
        helper.succeed();
    }

    /**
     * In 26.1 the engine's "in water" check (Entity.isInWater) asks the fluid type whether it is water-like (FluidType.isWaterLike),
     * not the swim settings. A player standing in a chemical pool must therefore be in water, as in a water pool, or the player
     * neither swims nor falls the way it does in water.
     */
    private static void playerInChemicalLiquidIsInWater(GameTestHelper helper) {
        net.minecraft.world.level.block.Block chemical = minechem.fluid.ElementFluids.entries().get(0).block().get();
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), chemical);
            }
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(abs.getX() + 0.5, abs.getY() + 0.2, abs.getZ() + 0.5);
        helper.runAfterDelay(2, () -> {
            player.baseTick(); // a mock player does not tick by itself; baseTick updates its fluid state
            helper.assertTrue(player.isInWater(), "a player standing in a chemical liquid should be in water, as in a water pool");
            helper.succeed();
        });
    }

    /**
     * A living entity in a radioactive element's liquid is poisoned, and one in a molecule's liquid gets the molecule's
     * pharmacology (sucrose: speed).
     */
    private static void chemicalLiquidAffectsLivingEntity(GameTestHelper helper) {
        net.minecraft.world.level.block.Block polonium = null;
        for (minechem.fluid.ElementFluids.Entry entry : minechem.fluid.ElementFluids.entries()) {
            if (entry.symbol().equals("Po")) {
                polonium = entry.block().get();
            }
        }
        net.minecraft.world.level.block.Block sucrose = minechem.fluid.MoleculeFluids.blockFor("Sucrose");
        net.minecraft.world.level.block.Block ethanol = minechem.fluid.MoleculeFluids.blockFor("Ethyl Alcohol");
        helper.assertTrue(polonium != null && sucrose != null && ethanol != null, "the polonium, sucrose and ethyl alcohol liquids should exist");
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), polonium);
                helper.setBlock(new BlockPos(x + 4, 1, z), sucrose);
                helper.setBlock(new BlockPos(x + 8, 1, z), ethanol);
            }
        }
        ServerPlayer inPolonium = helper.makeMockServerPlayerInLevel();
        ServerPlayer inSucrose = helper.makeMockServerPlayerInLevel();
        ServerPlayer inEthanol = helper.makeMockServerPlayerInLevel();
        BlockPos poloniumAbs = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos sucroseAbs = helper.absolutePos(new BlockPos(5, 1, 1));
        BlockPos ethanolAbs = helper.absolutePos(new BlockPos(9, 1, 1));
        inPolonium.setPos(poloniumAbs.getX() + 0.5, poloniumAbs.getY() + 0.2, poloniumAbs.getZ() + 0.5);
        inSucrose.setPos(sucroseAbs.getX() + 0.5, sucroseAbs.getY() + 0.2, sucroseAbs.getZ() + 0.5);
        inEthanol.setPos(ethanolAbs.getX() + 0.5, ethanolAbs.getY() + 0.2, ethanolAbs.getZ() + 0.5);
        helper.runAfterDelay(2, () -> {
            // The liquid's effects are applied while the player moves through it (Entity.applyEffectsFromBlocks); a mock player
            // does not move by itself, so a short movement inside the liquid is given.
            net.minecraft.world.phys.Vec3 a = inPolonium.position();
            inPolonium.applyEffectsFromBlocks(a, a.add(0.01, 0, 0));
            net.minecraft.world.phys.Vec3 b = inSucrose.position();
            inSucrose.applyEffectsFromBlocks(b, b.add(0.01, 0, 0));
            net.minecraft.world.phys.Vec3 c = inEthanol.position();
            inEthanol.applyEffectsFromBlocks(c, c.add(0.01, 0, 0));
            helper.assertTrue(inPolonium.hasEffect(net.minecraft.world.effect.MobEffects.POISON),
                "a player in a radioactive element's liquid should be poisoned");
            helper.assertTrue(inSucrose.hasEffect(net.minecraft.world.effect.MobEffects.SPEED),
                "a player in sucrose's liquid should get sucrose's speed effect");
            helper.assertTrue(inPolonium.getEffect(net.minecraft.world.effect.MobEffects.SLOWNESS).getAmplifier() == 3,
                "a solid chemical's liquid should slow by 60 %: slowness level 3");
            helper.assertTrue(inEthanol.getEffect(net.minecraft.world.effect.MobEffects.SLOWNESS).getAmplifier() == 2,
                "a liquid should slow by 45 %: slowness level 2");
            helper.assertTrue(!helper.getLevel().getBlockState(sucroseAbs).isAir(), "the sucrose liquid should be placed");
            helper.succeed();
        });
    }

    /**
     * A solid chemical's liquid does not flow, so the polonium liquid placed on a floor stays
     * put; the water control on the same floor spreads to the air beside it.
     */
    private static void solidLiquidDoesNotFlow(GameTestHelper helper) {
        net.minecraft.world.level.block.Block polonium = minechem.fluid.ElementFluids.blockFor("Po");
        helper.assertTrue(polonium != null, "the polonium liquid should exist");
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 6; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.STONE);
            }
        }
        helper.setBlock(new BlockPos(1, 2, 1), polonium);
        helper.setBlock(new BlockPos(1, 2, 4), net.minecraft.world.level.block.Blocks.WATER);
        // Both liquids are scheduled to tick; the server runs their fluid ticks (where a flow would spread) a few ticks later.
        tickFluidAt(helper, new BlockPos(1, 2, 1));
        tickFluidAt(helper, new BlockPos(1, 2, 4));
        helper.runAfterDelay(5, () -> {
            net.minecraft.world.level.Level level = helper.getLevel();
            helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(2, 2, 1))).isAir(),
                "a solid chemical's liquid should not flow to the air beside it");
            helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(2, 2, 4))).getFluidState().is(net.minecraft.tags.FluidTags.WATER),
                "the water control should flow to the air beside it");
            helper.succeed();
        });
    }

    /**
     * Lithium in a liquid beside water reacts (H2O+Li==LiOH+H). Both cells are cleared, and the products take
     * free cells of the three layers around the water: the hydrogen (a gas) from the top, the lithium hydroxide from the bottom.
     */
    private static void reactionFluidMeetsFluid(GameTestHelper helper) {
        helper.assertTrue(minechem.fluid.ElementFluids.blockFor("Li") != null, "the lithium liquid should exist");
        floorOf(helper);
        helper.setBlock(new BlockPos(1, 2, 1), net.minecraft.world.level.block.Blocks.WATER);
        helper.setBlock(new BlockPos(0, 2, 1), minechem.fluid.ElementFluids.blockFor("Li"));
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        minechem.fluid.reaction.ChemicalFluidReactionHandler.checkStatus(level, helper.absolutePos(new BlockPos(0, 2, 1)));
        helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(0, 2, 1))).isAir(), "the lithium liquid should be used up");
        helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(1, 2, 1))).isAir(), "the water should be used up");
        assertLithiumProducts(helper);
        helper.succeed();
    }

    /** A lithium bucket that falls into water reacts with it, and the bucket is used up. */
    private static void reactionItemMeetsFluid(GameTestHelper helper) {
        floorOf(helper);
        helper.setBlock(new BlockPos(1, 2, 1), net.minecraft.world.level.block.Blocks.WATER);
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(new BlockPos(1, 2, 1));
        net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(level,
            abs.getX() + 0.5, abs.getY() + 0.1, abs.getZ() + 0.5, minechem.fluid.ChemicalBuckets.bucketOf(minechem.helper.Jenkins.get(3)));
        level.addFreshEntity(item);
        minechem.fluid.reaction.ChemicalFluidReactionHandler.checkEntityItem(level, item);
        helper.assertTrue(!item.isAlive(), "the lithium bucket should be used up");
        assertLithiumProducts(helper);
        helper.succeed();
    }

    /** Control for reactionItemMeetsFluid: the vanilla water bucket in the same water does not react (it is not a bucket of the mod). */
    private static void reactionItemWaterBucketControl(GameTestHelper helper) {
        floorOf(helper);
        helper.setBlock(new BlockPos(1, 2, 1), net.minecraft.world.level.block.Blocks.WATER);
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(new BlockPos(1, 2, 1));
        net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(level,
            abs.getX() + 0.5, abs.getY() + 0.1, abs.getZ() + 0.5, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
        level.addFreshEntity(item);
        minechem.fluid.reaction.ChemicalFluidReactionHandler.checkEntityItem(level, item);
        helper.assertTrue(item.isAlive(), "the vanilla water bucket should not react");
        helper.succeed();
    }

    /** A TNT liquid with fire beside it is removed and explodes. */
    private static void reactionTntExplodesByFire(GameTestHelper helper) {
        helper.assertTrue(minechem.fluid.MoleculeFluids.blockFor("Trinitrotoluene") != null, "the TNT liquid should exist");
        floorOf(helper);
        helper.setBlock(new BlockPos(1, 2, 1), minechem.fluid.MoleculeFluids.blockFor("Trinitrotoluene"));
        helper.setBlock(new BlockPos(2, 2, 1), net.minecraft.world.level.block.Blocks.FIRE);
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        minechem.fluid.reaction.ChemicalFluidReactionHandler.checkStatus(level, helper.absolutePos(new BlockPos(1, 2, 1)));
        helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(1, 2, 1))).isAir(), "the TNT liquid beside fire should explode");
        helper.succeed();
    }

    /** Control for reactionTntExplodesByFire: the same TNT liquid with no fire beside it stays. */
    private static void reactionTntWithoutFireStays(GameTestHelper helper) {
        floorOf(helper);
        helper.setBlock(new BlockPos(1, 2, 1), minechem.fluid.MoleculeFluids.blockFor("Trinitrotoluene"));
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        minechem.fluid.reaction.ChemicalFluidReactionHandler.checkStatus(level, helper.absolutePos(new BlockPos(1, 2, 1)));
        helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(1, 2, 1))).is(minechem.fluid.MoleculeFluids.blockFor("Trinitrotoluene")),
            "the TNT liquid with no fire beside it should stay");
        helper.succeed();
    }

    /**
     * A gas with density -10 rises. The hydrogen placed on the floor moves into the air above it, while the
     * sulfuric acid liquid beside it (control) stays where it was placed.
     */
    private static void gasRises(GameTestHelper helper) {
        net.minecraft.world.level.block.Block hydrogen = minechem.fluid.ElementFluids.blockFor("H");
        net.minecraft.world.level.block.Block acid = minechem.fluid.MoleculeFluids.blockFor("Sulfuric Acid");
        helper.assertTrue(hydrogen != null && acid != null, "the hydrogen and sulfuric acid liquids should exist");
        floorOf(helper);
        helper.setBlock(new BlockPos(1, 2, 1), hydrogen);
        helper.setBlock(new BlockPos(0, 2, 1), acid);
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        BlockPos gas = helper.absolutePos(new BlockPos(1, 2, 1));
        // One fluid tick, called directly (the scheduler is not used here): the gas must leave its cell.
        net.minecraft.world.level.material.FluidState gasFluid = level.getFluidState(gas);
        ((minechem.fluid.MoleculeFluid) gasFluid.getType()).tick(level, gas, level.getBlockState(gas), gasFluid);
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(!level.getBlockState(helper.absolutePos(new BlockPos(1, 2, 1))).is(hydrogen),
                "the gas should leave the cell it was placed in");
            helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(1, 3, 1))).is(hydrogen)
                || level.getBlockState(helper.absolutePos(new BlockPos(1, 4, 1))).is(hydrogen), "the gas should be above the floor");
            helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(0, 2, 1))).is(acid),
                "the liquid control should stay where it was placed");
            helper.succeed();
        });
    }

    /**
     * A gas under a ceiling does not flow down: the air below it stays empty after one fluid tick, while the gas may spread sideways.
     */
    private static void gasDoesNotFlowDown(GameTestHelper helper) {
        net.minecraft.world.level.block.Block hydrogen = minechem.fluid.ElementFluids.blockFor("H");
        helper.assertTrue(hydrogen != null, "the hydrogen liquid should exist");
        floorOf(helper);
        helper.setBlock(new BlockPos(1, 4, 1), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(new BlockPos(1, 3, 1), hydrogen);
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        BlockPos gas = helper.absolutePos(new BlockPos(1, 3, 1));
        net.minecraft.world.level.material.FluidState gasFluid = level.getFluidState(gas);
        ((minechem.fluid.MoleculeFluid) gasFluid.getType()).tick(level, gas, level.getBlockState(gas), gasFluid);
        helper.assertTrue(!level.getBlockState(helper.absolutePos(new BlockPos(1, 2, 1))).is(hydrogen),
            "the gas should not flow down into the air below it");
        helper.succeed();
    }

    /**
     * A gas at the top of the world does not rise out of it: it stays (a source does not vanish), as it does under a ceiling.
     */
    private static void gasStopsAtBuildLimit(GameTestHelper helper) {
        net.minecraft.world.level.block.Block hydrogen = minechem.fluid.ElementFluids.blockFor("H");
        helper.assertTrue(hydrogen != null, "the hydrogen liquid should exist");
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos top = new BlockPos(abs.getX(), level.getMaxY(), abs.getZ());
        level.setBlock(top, hydrogen.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        net.minecraft.world.level.material.FluidState gasFluid = level.getFluidState(top);
        ((minechem.fluid.MoleculeFluid) gasFluid.getType()).tick(level, top, level.getBlockState(top), gasFluid);
        helper.assertTrue(level.getBlockState(top).is(hydrogen), "the gas at the top of the world should not be lost");
        helper.succeed();
    }

    /**
     * Two gas sources, one on the other, under a stone ceiling: the gas spreads sideways and stays bounded. A flowing cell can only
     * reach 6 from a source beside it, so no flowing gas may hold 8. The engine's rule for a flowing fluid, which takes the level
     * of the fluid above it, would give 8 to a gas and let the cloud grow without end.
     */
    private static void gasStackStaysBounded(GameTestHelper helper) {
        net.minecraft.world.level.block.Block hydrogen = minechem.fluid.ElementFluids.blockFor("H");
        helper.assertTrue(hydrogen != null, "the hydrogen liquid should exist");
        floorOf(helper);
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(new BlockPos(x, 4, z), net.minecraft.world.level.block.Blocks.STONE);
            }
        }
        helper.setBlock(new BlockPos(1, 2, 1), hydrogen);
        helper.setBlock(new BlockPos(1, 3, 1), hydrogen);
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        tickFluidAt(helper, new BlockPos(1, 2, 1));
        tickFluidAt(helper, new BlockPos(1, 3, 1));
        helper.runAfterDelay(80, () -> {
            int sources = 0;
            for (int x = 0; x < 3; x++) {
                for (int y = 2; y <= 3; y++) {
                    for (int z = 0; z < 3; z++) {
                        BlockPos abs = helper.absolutePos(new BlockPos(x, y, z));
                        net.minecraft.world.level.material.FluidState fluid = level.getFluidState(abs);
                        if (!level.getBlockState(abs).is(hydrogen)) {
                            continue;
                        }
                        if (fluid.isSource()) {
                            sources++;
                        } else {
                            helper.assertTrue(fluid.getAmount() < 8, "a flowing gas at " + abs + " holds " + fluid.getAmount()
                                + " levels, so the gas feeds itself from the gas above");
                        }
                    }
                }
            }
            helper.assertTrue(sources <= 2, "no source should be made from the gas, but there are " + sources);
            helper.succeed();
        });
    }

    /**
     * The API of other mods: a reaction and a decomposition recipe are added by name, and a
     * name that is not known, or a second recipe for the same stack, is refused.
     */
    private static void apiAddsReactionAndRecipe(GameTestHelper helper) {
        helper.assertTrue(minechem.api.ChemicalAPI.registerFluidChemicalReaction("Rb", "Cs", 0.5F, "Water"),
            "a reaction between known chemicals should be added");
        helper.assertTrue(!minechem.api.ChemicalAPI.registerFluidChemicalReaction("Rb", "NoSuchChemical", 0.5F, "Water"),
            "an unknown chemical should refuse the reaction");
        // A honeycomb has no recipe in the data and no OreDictionary rule.
        ItemStack honeycomb = new ItemStack(Items.HONEYCOMB);
        helper.assertTrue(minechem.api.RecipeAPI.addDecompositionRecipe(honeycomb, "1 H", "2 Water"),
            "a decomposition recipe with known outputs should be added");
        helper.assertTrue(minechem.apparatus.decomposer.DecomposerBlockEntity.RECIPES.get(
            minechem.apparatus.decomposer.DecomposerBlockEntity.keyFor(honeycomb)) != null, "the decomposer should have the recipe");
        helper.assertTrue(!minechem.api.RecipeAPI.addDecompositionRecipe(honeycomb, "1 H"), "a second recipe for the stack should be refused");
        helper.assertTrue(!minechem.api.RecipeAPI.addDecompositionRecipe(new ItemStack(Items.IRON_NUGGET), "1 NoSuchChemical"),
            "an unknown output should refuse the recipe");
        helper.succeed();
    }

    /**
     * The recipes of Applied Energistics 2 and Ender IO (data with "requires"): they are in the tables when the mod is loaded, and not
     * when it is not (the loader skips them). The item ids are the 26.1.2 ones.
     */
    private static void addonRecipesFollowModPresence(GameTestHelper helper) {
        boolean ae2 = net.neoforged.fml.ModList.get().isLoaded("ae2");
        boolean enderio = net.neoforged.fml.ModList.get().isLoaded("enderio");
        helper.assertTrue((minechem.apparatus.decomposer.DecomposerBlockEntity.RECIPES.get("ae2:singularity") != null) == ae2,
            "the singularity recipe should be present exactly when Applied Energistics 2 is loaded");
        helper.assertTrue((minechem.apparatus.synthesis.SynthesisRecipes.RECIPES.get("ae2:quartz_glass") != null) == ae2,
            "the quartz glass synthesis should be present exactly when Applied Energistics 2 is loaded");
        helper.assertTrue((minechem.apparatus.decomposer.DecomposerBlockEntity.RECIPES.get("enderio:dark_steel_ingot") != null) == enderio,
            "the dark steel recipe should be present exactly when Ender IO is loaded");
        helper.assertTrue((minechem.apparatus.decomposer.DecomposerBlockEntity.RECIPES.get("enderio:end_steel_ingot") != null) == enderio,
            "the end steel recipe should be present exactly when Ender IO is loaded");
        helper.assertTrue((minechem.apparatus.decomposer.DecomposerBlockEntity.RECIPES.get("enderio:powdered_iron") != null) == enderio,
            "the powdered iron recipe should be present exactly when Ender IO is loaded");
        helper.assertTrue((minechem.apparatus.decomposer.DecomposerBlockEntity.RECIPES.get("enderio:ender_crystal_powder") != null) == enderio,
            "the grains of the End recipe should be present exactly when Ender IO is loaded");
        boolean railcraft = net.neoforged.fml.ModList.get().isLoaded("railcraft");
        helper.assertTrue((minechem.apparatus.decomposer.DecomposerBlockEntity.RECIPES.get("railcraft:black_post") != null) == railcraft,
            "the Railcraft post recipe should be present exactly when Railcraft Reborn is loaded");
        helper.succeed();
    }

    /**
     * The chemical turtle is a ComputerCraft upgrade only when ComputerCraft is loaded: then its type is registered and its datapack
     * entry is in the turtle upgrade registry. Without ComputerCraft nothing is checked (the test server has no mods unless its
     * folder holds the jar).
     */
    private static void computercraftTurtleUpgrade(GameTestHelper helper) {
        if (net.neoforged.fml.ModList.get().isLoaded("computercraft")) {
            helper.assertTrue(minechem.compat.computercraft.ComputerCraftCompat.CHEMICAL.isBound(), "the chemical turtle upgrade type should be registered");
            helper.assertTrue(helper.getLevel().registryAccess().lookupOrThrow(dan200.computercraft.api.turtle.ITurtleUpgrade.REGISTRY)
                .get(Identifier.fromNamespaceAndPath("minechem", "chemical")).isPresent(),
                "minechem:chemical should be in the turtle upgrade registry");
        }
        helper.succeed();
    }

    /**
     * The OreDictionary defaults are rules of the bundled file: an ingot, a nugget and a plate of
     * nickel, the zinc ingot, and the brass and block additions. A tag decides which items a rule reaches, and the test game has no
     * Railcraft, so only the rules are checked here.
     */
    private static void oreDictionaryDefaults(GameTestHelper helper) {
        java.util.Map<String, minechem.apparatus.decomposer.DecomposerOreBinder.Rule> byName = new java.util.HashMap<>();
        for (minechem.apparatus.decomposer.DecomposerOreBinder.Rule rule : minechem.apparatus.decomposer.DecomposerOreBinder.loadBundled()) {
            if (rule.name() != null) {
                byName.put(rule.name(), rule);
            }
        }
        for (String name : java.util.List.of("ingotNickel", "nuggetNickel", "plateNickel", "ingotZinc", "ingotBrass", "blockNickel")) {
            helper.assertTrue(byName.containsKey(name), name + " should have an OreDictionary decomposition rule");
        }
        minechem.apparatus.decomposer.DecomposerRecipes.Output nickel = byName.get("ingotNickel").outputs().get(0);
        helper.assertTrue(nickel.chemical() instanceof Element element && element.shortName.equals("Ni") && nickel.amount() == 16,
            "a nickel ingot gives 16 of Ni (the expected composition)");
        helper.assertTrue(byName.get("nuggetNickel").outputs().get(0).amount() == 1, "a nickel nugget gives 1 of Ni (16 / 9, floored)");
        helper.assertTrue(byName.get("blockNickel").outputs().get(0).amount() == 144, "a nickel block gives 144 of Ni (nine ingots)");
        for (String name : java.util.List.of("nuggetBronze", "plateBronze", "blockBronze", "nuggetInvar", "plateInvar", "blockInvar",
            "nuggetSteel", "plateSteel", "blockSteel")) {
            helper.assertTrue(byName.containsKey(name), name + " should have an OreDictionary decomposition rule");
        }
        // The Ender IO alloys, the Railcraft gears (the composition of the craft) and the steel tools (three steel ingots for a pickaxe).
        for (String name : java.util.List.of("ingotDarkSteel", "nuggetDarkSteel", "blockDarkSteel", "ingotConductiveAlloy",
            "blockEndSteel", "gearBronze", "gearIron", "toolPickaxesSteel", "toolShovelsSteel")) {
            helper.assertTrue(byName.containsKey(name), name + " should have an OreDictionary decomposition rule");
        }
        helper.assertTrue(byName.get("gearBronze").outputs().stream().anyMatch(o -> o.amount() == 96),
            "a bronze gear is four bronze ingots (Cu 48) and a bushing gear, itself four bronze ingots (Cu 48): Cu 96");
        helper.assertTrue(byName.get("toolPickaxesSteel").outputs().stream().anyMatch(o -> o.amount() == 45),
            "a steel pickaxe is three steel ingots (Fe 15 each): Fe 45");
        // The common tags name a block of metal "storage_blocks/<metal>" and an underscore starts a word (ingots/dark_steel).
        helper.assertTrue(minechem.apparatus.synthesis.OreSynthesisBinder.oreNameOf("storage_blocks/iron").equals("blockIron"),
            "the common tag storage_blocks/iron should be the name blockIron");
        helper.assertTrue(minechem.apparatus.synthesis.OreSynthesisBinder.oreNameOf("ingots/dark_steel").equals("ingotDarkSteel"),
            "the common tag ingots/dark_steel should be the name ingotDarkSteel");
        helper.assertTrue(minechem.apparatus.decomposer.DecomposerBlockEntity.RECIPES.get(
            minechem.apparatus.decomposer.DecomposerBlockEntity.keyFor(new ItemStack(Items.IRON_BLOCK))) != null,
            "a block of iron should be decomposed through the blockIron rule");
        helper.succeed();
    }

    /** A stone floor under a 3 by 3 area: the cells above it are where a reaction's products go. */
    private static void floorOf(GameTestHelper helper) {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.STONE);
            }
        }
    }

    /** The hydrogen and the lithium hydroxide of the lithium reaction: one of each, in the three layers above the floor. */
    private static void assertLithiumProducts(GameTestHelper helper) {
        net.minecraft.world.level.block.Block hydrogen = minechem.fluid.ElementFluids.blockFor("H");
        net.minecraft.world.level.block.Block lithiumHydroxide = minechem.fluid.MoleculeFluids.blockFor("Lithium Hydroxide");
        helper.assertTrue(hydrogen != null && lithiumHydroxide != null, "the hydrogen and lithium hydroxide liquids should exist");
        helper.assertTrue(countBlocks(helper, hydrogen) == 1, "one hydrogen liquid should be made");
        helper.assertTrue(countBlocks(helper, lithiumHydroxide) == 1, "one lithium hydroxide liquid should be made");
    }

    private static int countBlocks(GameTestHelper helper, net.minecraft.world.level.block.Block block) {
        int count = 0;
        for (int x = 0; x < 3; x++) {
            for (int y = 1; y <= 3; y++) {
                for (int z = 0; z < 3; z++) {
                    if (helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(x, y, z))).is(block)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static void tickFluidAt(GameTestHelper helper, BlockPos pos) {
        BlockPos abs = helper.absolutePos(pos);
        net.minecraft.world.level.material.FluidState fluid = helper.getLevel().getFluidState(abs);
        helper.getLevel().scheduleTick(abs, fluid.getType(), 1);
    }

    /** Control for playerInChemicalLiquidIsInWater: the same pool of vanilla water must put the player in water. */
    private static void playerInWaterControl(GameTestHelper helper) {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.WATER);
            }
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(abs.getX() + 0.5, abs.getY() + 0.2, abs.getZ() + 0.5);
        helper.runAfterDelay(2, () -> {
            player.baseTick(); // a mock player does not tick by itself; baseTick updates its fluid state
            helper.assertTrue(player.isInWater(), "control: a player standing in water should be in water");
            helper.succeed();
        });
    }

    /** The radiation suit has a recipe for each of its four pieces. */
    private static void hazmatRecipesExist(GameTestHelper helper) {
        for (String piece : java.util.List.of("helmet", "chestplate", "leggings", "boots")) {
            helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                    Identifier.fromNamespaceAndPath("minechem", "hazmat_" + piece))).isPresent(), "no recipe for the hazmat " + piece);
        }
        helper.succeed();
    }

    /** Worn in full, the suit removes 0.80 of the radiation damage: 18 becomes 4 (18 times 0.2 is 3.6, rounded). */
    private static void hazmatSuitShieldsPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(ItemRegistry.HAZMAT_HELMET.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(ItemRegistry.HAZMAT_CHESTPLATE.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(ItemRegistry.HAZMAT_LEGGINGS.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(ItemRegistry.HAZMAT_BOOTS.get()));
        ItemStack stack = ChemicalItem.stackFor(Jenkins.get(84));
        stack.setCount(3);
        player.getInventory().setItem(0, stack);
        long life = RadiationRules.ofElement("Po").getLife();
        RadiationTicker.update(player, null, 0L, "minecraft:overworld");
        int damage = RadiationTicker.update(player, null, life + 1, "minecraft:overworld");
        helper.assertTrue(damage == 4, "the full suit leaves 4 of 18: got " + damage);
        helper.succeed();
    }

    /**
     * A radioactive molecule, when its life has passed, becomes its atoms (the radioactive ones one
     * step down); no molecule is left in the inventory.
     */
    private static void radiationMoleculeDecaysIntoAtoms(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ChemicalBase molecule = null;
        for (ChemicalBase chemical : Jenkins.getAll()) {
            if (chemical instanceof Molecule && RadiationRules.ofFormula(chemical.getFormula()) != RadiationEnum.stable) {
                molecule = chemical;
                break;
            }
        }
        helper.assertTrue(molecule != null, "the data has a radioactive molecule");
        ItemStack stack = ChemicalItem.stackFor(molecule);
        stack.setCount(2);
        player.getInventory().setItem(0, stack);
        long life = RadiationRules.ofFormula(molecule.getFormula()).getLife();
        RadiationTicker.update(player, null, 0L, "minecraft:overworld");
        int damage = RadiationTicker.update(player, null, life + 1, "minecraft:overworld");
        helper.assertTrue(damage > 0, "a decaying molecule does damage: got " + damage);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            helper.assertTrue(item.isEmpty() || ChemicalItem.getChemicalBase(item) instanceof Element,
                "the molecule should be gone, but slot " + slot + " holds " + item);
        }
        helper.succeed();
    }

    /** Each molecule fluid has a bucket item that is a bucket. */
    /** PolytoolItem.getSwordStr and getPickaxeStr: 8 plus each alloy's strength times its power (palladium: sword 8, pickaxe -2 per power). */
    /** A radioactive fluid keeps its state in the level, and the bucket that picks it up gets it back. */
    private static void radioactiveFluidKeepsStateOnPickup(GameTestHelper helper) {
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        net.minecraft.core.BlockPos pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
        minechem.fluid.MoleculeFluidBlock block = null;
        for (ElementFluids.Entry entry : ElementFluids.entries()) {
            if (entry.symbol().equals("Po")) {
                block = entry.block().get();
            }
        }
        helper.assertTrue(block != null, "no fluid block for polonium");
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        minechem.radiation.RadiationState state = new minechem.radiation.RadiationState(0L, 0L, "minecraft:overworld");
        minechem.radiation.FluidRadiationData.get(level).put(pos, state);
        ItemStack bucket = block.pickupBlock(null, level, pos, level.getBlockState(pos));
        helper.assertTrue(!bucket.isEmpty(), "the polonium fluid should give a bucket");
        minechem.radiation.RadiationState back = bucket.get(ItemRegistry.RADIATION.get());
        helper.assertTrue(back != null && "minecraft:overworld".equals(back.dimension()), "the bucket should carry the fluid's radiation");
        helper.assertTrue(minechem.radiation.FluidRadiationData.get(level).take(pos) == null, "the fluid's entry should be taken");
        helper.succeed();
    }

    /**
     * A radioactive fluid replaced by another block loses its entry,
     * so the level's data does not keep the entries of fluids that are gone.
     */
    private static void radioactiveFluidEntryGoesWithBlock(GameTestHelper helper) {
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        net.minecraft.core.BlockPos pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
        minechem.fluid.MoleculeFluidBlock block = null;
        for (ElementFluids.Entry entry : ElementFluids.entries()) {
            if (entry.symbol().equals("Po")) {
                block = entry.block().get();
            }
        }
        helper.assertTrue(block != null, "no fluid block for polonium");
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        minechem.radiation.FluidRadiationData.get(level).put(pos, new minechem.radiation.RadiationState(0L, 0L, "minecraft:overworld"));
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        helper.assertTrue(minechem.radiation.FluidRadiationData.get(level).take(pos) == null, "the replaced fluid kept its radiation entry");
        helper.succeed();
    }

    private static void polytoolAlloyStrength(GameTestHelper helper) {
        ItemStack tool = new ItemStack(ItemRegistry.POLYTOOL.get());
        PolytoolItem.addPower(tool, 46, 1F);
        helper.assertTrue(PolytoolItem.getSwordStr(tool) == 16F, "sword of a palladium tool should be 16, got " + PolytoolItem.getSwordStr(tool));
        helper.assertTrue(PolytoolItem.getPickaxeStr(tool) == 6F, "pickaxe of a palladium tool should be 6, got " + PolytoolItem.getPickaxeStr(tool));
        helper.succeed();
    }

    /** The time left is the level's life less the time since the decay started. */
    private static void radiationTooltipTimeLeft(GameTestHelper helper) {
        minechem.radiation.RadiationState state = new minechem.radiation.RadiationState(1000L, 1000L, "minecraft:overworld");
        long left = ChemicalItem.remainingTicks(state, minechem.radiation.RadiationEnum.radioactive, 3000L);
        helper.assertTrue(left == 432000L - 2000L, "the time left should be the life less 2000 ticks, got " + left);
        helper.assertTrue(ChemicalItem.formatTicks(72000L * 2 + 1200L * 3) .equals("2h 3min"), "2 h 3 min was " + ChemicalItem.formatTicks(72000L * 2 + 1200L * 3));
        helper.assertTrue(ChemicalItem.formatTicks(1200L * 3 + 20L * 5).equals("3min 5s"), "3 min 5 s was " + ChemicalItem.formatTicks(1200L * 3 + 20L * 5));
        helper.assertTrue(ChemicalItem.formatTicks(20L * 5).equals("5s"), "5 s was " + ChemicalItem.formatTicks(20L * 5));
        helper.succeed();
    }

    /** A radioactive element's tooltip names its level; a stable one adds no such line. */
    private static void radioactiveElementTooltip(GameTestHelper helper) {
        ItemStack polonium = ChemicalItem.stackFor(Jenkins.get(84));
        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        polonium.getItem().appendHoverText(polonium, net.minecraft.world.item.Item.TooltipContext.EMPTY, net.minecraft.world.item.component.TooltipDisplay.DEFAULT, lines::add, net.minecraft.world.item.TooltipFlag.NORMAL);
        helper.assertTrue(lines.stream().anyMatch(line -> line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                && t.getKey().equals("element.property.radioactive")), "polonium's tooltip does not name its radioactive level");
        ItemStack hydrogen = ChemicalItem.stackFor(Jenkins.get(1));
        java.util.List<net.minecraft.network.chat.Component> stable = new java.util.ArrayList<>();
        hydrogen.getItem().appendHoverText(hydrogen, net.minecraft.world.item.Item.TooltipContext.EMPTY, net.minecraft.world.item.component.TooltipDisplay.DEFAULT, stable::add, net.minecraft.world.item.TooltipFlag.NORMAL);
        helper.assertTrue(stable.stream().noneMatch(line -> line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                && t.getKey().startsWith("element.property.")), "a stable element should have no radioactive level line");
        helper.succeed();
    }

    /**
     * The radiation clock rewrites a radioactive stack every tick: that alone must not replay the re-equip animation, while a
     * new chemical (a decay) or another slot does. Only the hook's answer is checked; the animation itself needs a client.
     */
    private static void radiationUpdateDoesNotReequip(GameTestHelper helper) {
        ItemStack before = ChemicalItem.stackFor(Jenkins.get(84));
        before.set(ItemRegistry.RADIATION.get(), new minechem.radiation.RadiationState(10L, 10L, "minecraft:overworld"));
        ItemStack ticked = before.copy();
        ticked.set(ItemRegistry.RADIATION.get(), new minechem.radiation.RadiationState(10L, 11L, "minecraft:overworld"));
        helper.assertFalse(before.getItem().shouldCauseReequipAnimation(before, ticked, false), "a radiation tick replays the re-equip animation");
        helper.assertTrue(before.getItem().shouldCauseReequipAnimation(before, ChemicalItem.stackFor(Jenkins.get(83)), false), "a decay into bismuth should re-equip");
        helper.assertTrue(before.getItem().shouldCauseReequipAnimation(before, ticked, true), "a new slot should re-equip");
        ItemStack bucket = new ItemStack(bucketOfElement("Po"));
        ItemStack tickedBucket = bucket.copy();
        tickedBucket.set(ItemRegistry.RADIATION.get(), new minechem.radiation.RadiationState(10L, 11L, "minecraft:overworld"));
        helper.assertFalse(bucket.getItem().shouldCauseReequipAnimation(bucket, tickedBucket, false), "a radiation tick on a bucket replays the re-equip animation");
        helper.succeed();
    }

    /**
     * The time left in a radioactive element's tooltip comes from the tooltip context's level, because the client's Minecraft
     * class does not exist on a dedicated server.
     */
    private static void radioactiveTooltipUsesContextLevel(GameTestHelper helper) {
        long now = helper.getLevel().getGameTime();
        ItemStack polonium = ChemicalItem.stackFor(Jenkins.get(84));
        polonium.set(ItemRegistry.RADIATION.get(), new minechem.radiation.RadiationState(now, now, "minecraft:overworld"));
        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        polonium.getItem().appendHoverText(polonium, net.minecraft.world.item.Item.TooltipContext.of(helper.getLevel()),
            net.minecraft.world.item.component.TooltipDisplay.DEFAULT, lines::add, net.minecraft.world.item.TooltipFlag.NORMAL);
        helper.assertTrue(lines.stream().anyMatch(line -> line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                && t.getKey().equals("element.property.radioactive") && line.getString().contains("(")),
            "polonium's radioactive line should show the time left");
        helper.succeed();
    }

    /** PolytoolItem.hitEntity: a hit deals the sword strength (palladium gives 16) and the zombie loses that much health. */
    private static void polytoolHitDamage(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack tool = new ItemStack(ItemRegistry.POLYTOOL.get());
        PolytoolItem.addPower(tool, 46, 1F);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        float before = zombie.getHealth();
        tool.getItem().hurtEnemy(tool, zombie, player);
        float lost = before - zombie.getHealth();
        // Vanilla damage reduction (the zombie's armour) takes a little off the 16: 15.74 was measured.
        helper.assertTrue(lost > 15F && lost <= 16F, "a palladium polytool should deal about 16 damage, the zombie lost " + lost);
        helper.succeed();
    }

    /**
     * Neither a hit nor a broken block damages the polytool (a 26.1 pickaxe loses 2 per hit through its WEAPON component and
     * 1 per block through its TOOL component).
     */
    private static void polytoolKeepsDurability(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack tool = new ItemStack(ItemRegistry.POLYTOOL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        if (tool.hurtEnemy(zombie, player)) {
            tool.postHurtEnemy(zombie, player);
        }
        BlockPos stone = new BlockPos(0, 1, 0);
        helper.setBlock(stone, Blocks.STONE);
        BlockPos absolute = helper.absolutePos(stone);
        tool.mineBlock(helper.getLevel(), helper.getLevel().getBlockState(absolute), absolute, player);
        helper.assertTrue(tool.getDamageValue() == 0, "the polytool lost durability: damage " + tool.getDamageValue());
        helper.succeed();
    }

    /** PolytoolItem.validAlloyInfusion: an aluminium alloy may be added once (pickaxe 4 left), not twice (pickaxe 0). */
    private static void polytoolAlloyInfusionValidity(GameTestHelper helper) {
        ItemStack tool = new ItemStack(ItemRegistry.POLYTOOL.get());
        helper.assertTrue(PolytoolItem.validAlloyInfusion(tool, 13), "one aluminium should be allowed on a new polytool");
        PolytoolItem.addPower(tool, 13, 1F);
        helper.assertTrue(!PolytoolItem.validAlloyInfusion(tool, 13), "a second aluminium should be refused");
        helper.assertTrue(PolytoolItem.validAlloyInfusion(tool, 8), "a non-alloy element should always be allowed");
        helper.succeed();
    }

    private static void moleculeBucketIsBucket(GameTestHelper helper) {
        MoleculeFluids.Entry entry = MoleculeFluids.entries().get(0);
        ItemStack stack = new ItemStack(entry.bucket().get());
        helper.assertTrue(stack.getItem() instanceof BucketItem, "the bucket of " + entry.molecule() + " is not a BucketItem");
        helper.assertTrue(stack.getItem() == entry.bucket().get(), "the bucket item is not the registered one");
        helper.succeed();
    }

    /** Each element has a bucket item too. */
    private static void elementBucketIsBucket(GameTestHelper helper) {
        ElementFluids.Entry entry = ElementFluids.entries().get(0);
        ItemStack stack = new ItemStack(entry.bucket().get());
        helper.assertTrue(stack.getItem() instanceof BucketItem, "the bucket of element " + entry.symbol() + " is not a BucketItem");
        helper.assertTrue(ElementFluids.colourOf("nonMetal") == new java.awt.Color(1F, 0.5F, 0F).getRGB(), "nonmetals are red and half green");
        helper.succeed();
    }

    /** A radioactive element bucket decays into the bucket of the element below. */
    private static void radiationElementBucketDecays(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack bucket = new ItemStack(bucketOfElement("Po"), 3);
        player.getInventory().setItem(0, bucket);
        long life = RadiationRules.ofElement("Po").getLife();
        RadiationTicker.update(player, null, 0L, "minecraft:overworld");
        RadiationTicker.update(player, null, life + 1, "minecraft:overworld");
        ItemStack after = player.getInventory().getItem(0);
        helper.assertTrue(after.getItem() == bucketOfElement("Bi"), "polonium bucket should become bismuth bucket");
        helper.assertTrue(after.getCount() == 3, "three polonium buckets should stay three bismuth buckets, got " + after.getCount());
        helper.succeed();
    }

    private static net.minecraft.world.item.Item bucketOfElement(String symbol) {
        for (ElementFluids.Entry entry : ElementFluids.entries()) {
            if (entry.symbol().equals(symbol)) {
                return entry.bucket().get();
            }
        }
        throw new IllegalStateException("no bucket for " + symbol);
    }

    /** Eight vials and a bucket make the bucket; the bucket makes eight vials. */
    private static void bucketRecipesFillAndEmpty(GameTestHelper helper) {
        net.minecraft.world.level.Level level = helper.getLevel();
        ItemStack vial = ChemicalItem.stackFor(Jenkins.get(11));
        List<ItemStack> eight = new ArrayList<>();
        eight.add(new ItemStack(net.minecraft.world.item.Items.BUCKET));
        for (int i = 0; i < 8; i++) {
            eight.add(vial.copy());
        }
        while (eight.size() < 9) {
            eight.add(ItemStack.EMPTY);
        }
        CraftingInput fill = CraftingInput.of(3, 3, eight);
        helper.assertTrue(BucketFillRecipe.INSTANCE.matches(fill, level), "eight sodium vials and a bucket should fill a sodium bucket");
        helper.assertTrue(BucketFillRecipe.INSTANCE.assemble(fill).getItem() == bucketOfElement("Na"), "the result should be the sodium bucket");
        List<ItemStack> seven = new ArrayList<>(eight);
        seven.remove(8);
        seven.add(ItemStack.EMPTY);
        helper.assertTrue(!BucketFillRecipe.INSTANCE.matches(CraftingInput.of(3, 3, seven), level), "seven vials must not fill a bucket");
        List<ItemStack> bucket = new ArrayList<>();
        bucket.add(new ItemStack(bucketOfElement("Na")));
        while (bucket.size() < 9) {
            bucket.add(ItemStack.EMPTY);
        }
        CraftingInput empty = CraftingInput.of(3, 3, bucket);
        helper.assertTrue(BucketEmptyRecipe.INSTANCE.matches(empty, level), "a sodium bucket should empty");
        ItemStack vials = BucketEmptyRecipe.INSTANCE.assemble(empty);
        helper.assertTrue(vials.getCount() == 8 && ChemicalItem.getChemicalBase(vials) != null && ChemicalItem.getChemicalBase(vials).fullName.equals(ChemicalItem.getChemicalBase(vial).fullName), "eight sodium vials should come back");
        helper.succeed();
    }

    /** The decomposer's sides: the top reaches the input, the others the outputs. */
    private static void decomposerItemSides(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 1, 0), BlockRegistry.DECOMPOSER.get());
        DecomposerBlockEntity decomposer = helper.getBlockEntity(new BlockPos(0, 1, 0), DecomposerBlockEntity.class);
        helper.assertTrue(Arrays.equals(decomposer.getSlotsForFace(Direction.UP), new int[] {DecomposerBlockEntity.INPUT_SLOT}),
            "the top of the decomposer does not reach the input slot");
        helper.assertTrue(Arrays.stream(decomposer.getSlotsForFace(Direction.NORTH)).noneMatch(slot -> slot == DecomposerBlockEntity.INPUT_SLOT),
            "a side of the decomposer reaches the input slot");
        helper.assertFalse(decomposer.canPlaceItemThroughFace(DecomposerBlockEntity.INPUT_SLOT, new ItemStack(Items.BARRIER), Direction.UP),
            "the decomposer takes an item without a recipe");
        helper.succeed();
    }

    /** The machines are block items in the primary creative tab. */
    private static void creativeTabHasMachines(GameTestHelper helper) {
        CreativeModeTab tab = CreativeTabRegistry.PRIMARY.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, false, helper.getLevel().registryAccess()));
        Collection<ItemStack> items = tab.getDisplayItems();
        for (BlockItem machine : List.of(BlockRegistry.SYNTHESIS_ITEM.get(), BlockRegistry.DECOMPOSER_ITEM.get(),
            BlockRegistry.MICROSCOPE_ITEM.get(), BlockRegistry.LEADED_CHEST_ITEM.get())) {
            helper.assertTrue(items.stream().anyMatch(stack -> stack.is(machine)), machine + " is not in the primary creative tab");
        }
        helper.succeed();
    }

    private static Identifier name(String path) {
        return Identifier.fromNamespaceAndPath(Compendium.Naming.id, path);
    }

    /**
     * An environment of its own for one test. The tests of one environment run in one batch, side by side with neighbours about
     * six blocks apart, so a test whose structure is larger than its area (the reactors) gets a batch of its own.
     */
    /**
     * Registers a test, unless the run names tests to run (system property minechem.gametests.only: comma-separated words, a test
     * runs when its path contains one of them). Dev runs only; with no property every test is registered.
     */
    private static void registerTest(RegisterGameTestsEvent event, Identifier name, GameTestInstance test) {
        String only = System.getProperty("minechem.gametests.only", "");
        if (only.isBlank()) {
            event.registerTest(name, test);
            return;
        }
        for (String word : only.split(",")) {
            if (!word.isBlank() && name.getPath().contains(word.trim())) {
                event.registerTest(name, test);
                return;
            }
        }
    }

    private static Holder<TestEnvironmentDefinition<?>> alone(RegisterGameTestsEvent event, String path) {
        return event.registerEnvironment(Identifier.fromNamespaceAndPath(Compendium.Naming.id, path));
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> data(Holder<TestEnvironmentDefinition<?>> environment) {
        return new TestData<>(environment, Identifier.parse(STRUCTURE), 100, 1, true);
    }

    private static void synthesisHasBlockEntity(GameTestHelper helper) {
        BlockPos pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, BlockRegistry.SYNTHESIS.get());
        helper.assertTrue(helper.getBlockEntity(pos, SynthesisBlockEntity.class) != null, "the synthesis machine has no block entity");
        helper.succeed();
    }

    /** Regression test for the floor holes: the machine must not occlude the block below its faces (the block is not opaque). */
    private static void machinesDoNotOcclude(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 0, 0), Blocks.STONE);
        helper.setBlock(new BlockPos(0, 1, 0), BlockRegistry.SYNTHESIS.get());
        helper.setBlock(new BlockPos(2, 1, 0), BlockRegistry.MICROSCOPE.get());
        helper.setBlock(new BlockPos(4, 1, 0), BlockRegistry.DECOMPOSER.get());
        helper.assertFalse(helper.getBlockState(new BlockPos(0, 1, 0)).canOcclude(), "the synthesis block occludes its neighbours");
        helper.assertFalse(helper.getBlockState(new BlockPos(2, 1, 0)).canOcclude(), "the microscope block occludes its neighbours");
        helper.assertFalse(helper.getBlockState(new BlockPos(4, 1, 0)).canOcclude(), "the decomposer block occludes its neighbours");
        helper.succeed();
    }

    /** The OreDictionary rules are applied when the tags are updated: iron ingots must have a synthesis recipe. */
    private static void oreRecipesBound(GameTestHelper helper) {
        helper.assertTrue(SynthesisRecipes.RECIPES.get("minecraft:iron_ingot") != null, "no synthesis recipe for minecraft:iron_ingot");
        helper.succeed();
    }

    /**
     * Each kept machine opens its menu on the server: the block entity exists, the menu builds its slots, and the player can
     * still use the block afterwards. The menu types are registered as well.
     */
    private static void machinesOpenMenus(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos pos = new BlockPos(0, 1, 0);

        helper.setBlock(pos, BlockRegistry.SYNTHESIS.get());
        SynthesisBlockEntity synthesis = helper.getBlockEntity(pos, SynthesisBlockEntity.class);
        helper.assertTrue(new SynthesisMenu(0, player.getInventory(), synthesis).slots.size() > 0, "synthesis menu has no slots");

        helper.setBlock(pos, BlockRegistry.MICROSCOPE.get());
        MicroscopeBlockEntity microscope = helper.getBlockEntity(pos, MicroscopeBlockEntity.class);
        helper.assertTrue(new MicroscopeMenu(0, player.getInventory(), microscope).slots.size() > 0, "microscope menu has no slots");

        helper.setBlock(pos, BlockRegistry.DECOMPOSER.get());
        DecomposerBlockEntity decomposer = helper.getBlockEntity(pos, DecomposerBlockEntity.class);
        helper.assertTrue(new DecomposerMenu(0, player.getInventory(), decomposer).slots.size() > 0, "decomposer menu has no slots");

        helper.setBlock(pos, BlockRegistry.LEADED_CHEST.get());
        LeadedChestBlockEntity chest = helper.getBlockEntity(pos, LeadedChestBlockEntity.class);
        helper.assertTrue(new LeadedChestMenu(0, player.getInventory(), chest).slots.size() > 0, "leaded chest menu has no slots");

        helper.assertTrue(MenuRegistry.MICROSCOPE.get() != null && MenuRegistry.SYNTHESIS.get() != null
            && MenuRegistry.DECOMPOSER.get() != null && MenuRegistry.LEADED_CHEST.get() != null, "a machine menu type is missing");
        helper.succeed();
    }

    /**
     * Vanilla hoppers see the synthesis machine as a container, so its side rules apply there: a
     * hopper above puts chemicals into the storage, not the recipe grid; a hopper below takes neither the grid's ghosts nor the
     * storage.
     */
    private static void synthesisHoppersFollowSides(GameTestHelper helper) {
        BlockPos below = new BlockPos(1, 1, 1);
        BlockPos pos = new BlockPos(1, 2, 1);
        BlockPos above = new BlockPos(1, 3, 1);
        helper.setBlock(pos, BlockRegistry.SYNTHESIS.get());
        SynthesisBlockEntity synthesis = helper.getBlockEntity(pos, SynthesisBlockEntity.class);
        synthesis.setItem(SynthesisBlockEntity.RECIPE_FIRST + 4, new ItemStack(Items.DIRT, 3));
        synthesis.setItem(SynthesisBlockEntity.STORAGE_FIRST + 5, ChemicalItem.stackFor(Jenkins.get("Oxygen")).copyWithCount(2));
        helper.setBlock(below, Blocks.HOPPER);
        helper.setBlock(above, Blocks.HOPPER.defaultBlockState());
        var top = helper.getBlockEntity(above, net.minecraft.world.level.block.entity.HopperBlockEntity.class);
        top.setItem(0, ChemicalItem.stackFor(Jenkins.get("Hydrogen")).copyWithCount(2));
        helper.runAfterDelay(40, () -> {
            for (int slot = SynthesisBlockEntity.RECIPE_FIRST; slot < SynthesisBlockEntity.STORAGE_FIRST; slot++) {
                ItemStack stack = synthesis.getItem(slot);
                helper.assertTrue(slot == SynthesisBlockEntity.RECIPE_FIRST + 4 ? stack.is(Items.DIRT) && stack.getCount() == 3 : stack.isEmpty(),
                    "the recipe grid slot " + slot + " was changed by a hopper: " + stack);
            }
            helper.assertTrue(synthesis.getItem(SynthesisBlockEntity.STORAGE_FIRST).getCount() == 2, "the hopper above should fill the storage");
            var bottom = helper.getBlockEntity(below, net.minecraft.world.level.block.entity.HopperBlockEntity.class);
            helper.assertTrue(bottom.isEmpty(), "the hopper below took an item it must not reach: " + bottom.getItem(0));
            helper.succeed();
        });
    }

    /**
     * The recipe in the grid and a full storage of iron: after the machine is saved and loaded again (a reload of the world or
     * of its chunk), the output must still be takeable.
     */
    private static void synthesisRecipeSurvivesReload(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, BlockRegistry.SYNTHESIS.get());
        SynthesisBlockEntity synthesis = helper.getBlockEntity(pos, SynthesisBlockEntity.class);
        BlockPos absolute = helper.absolutePos(pos);
        SynthesisRecipes.Recipe recipe = SynthesisRecipes.RECIPES.get("minecraft:iron_ingot");
        helper.assertTrue(recipe != null, "no synthesis recipe for minecraft:iron_ingot");
        synthesis.setRecipe(recipe);
        for (int i = 0; i < SynthesisBlockEntity.STORAGE_SLOTS; i++) {
            synthesis.setItem(SynthesisBlockEntity.STORAGE_FIRST + i, ChemicalItem.stackFor(Jenkins.get("Iron")).copyWithCount(64));
        }
        synthesis.getEnergy().set(SynthesisBlockEntity.ENERGY_CAPACITY);
        SynthesisBlockEntity.serverTick(helper.getLevel(), absolute, helper.getLevel().getBlockState(absolute), synthesis);
        helper.assertTrue(synthesis.canTakeOutputStack(false), "before the reload the output should be takeable");

        // The full save carries the block entity's id, which loadStatic reads to rebuild it (the update tag does not).
        net.minecraft.nbt.CompoundTag saved = synthesis.saveWithFullMetadata(helper.getLevel().registryAccess());
        var state = helper.getLevel().getBlockState(absolute);
        net.minecraft.world.level.block.entity.BlockEntity loaded =
            net.minecraft.world.level.block.entity.BlockEntity.loadStatic(absolute, state, saved, helper.getLevel().registryAccess());
        helper.getLevel().setBlockEntity(loaded);
        SynthesisBlockEntity reloaded = helper.getBlockEntity(pos, SynthesisBlockEntity.class);
        helper.assertTrue(reloaded != null, "the reloaded machine has no block entity");
        SynthesisBlockEntity.serverTick(helper.getLevel(), absolute, state, reloaded);
        helper.assertTrue(reloaded.canTakeOutputStack(false),
            "after the reload the output is not takeable: the recipe in the grid was not found again");
        helper.succeed();
    }

    /**
     * A hopper under the synthesis machine takes an output of more than one item. The NeoForge hopper asks for one item at a
     * time: the batch runs once and the rest of it follows.
     */
    private static void synthesisHopperTakesBatchOutput(GameTestHelper helper) {
        BlockPos hopperPos = new BlockPos(1, 1, 1);
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, BlockRegistry.SYNTHESIS.get());
        SynthesisBlockEntity synthesis = helper.getBlockEntity(pos, SynthesisBlockEntity.class);
        BlockPos absolute = helper.absolutePos(pos);
        SynthesisRecipes.Recipe chosen = null;
        for (SynthesisRecipes.Recipe recipe : SynthesisRecipes.RECIPES.values()) {
            if (recipe.count() < 2 || recipe.ingredients().stream().anyMatch(i -> i != null && i.amount() * 2 > 64)) {
                continue;
            }
            synthesis.setRecipe(recipe);
            SynthesisBlockEntity.serverTick(helper.getLevel(), absolute, helper.getLevel().getBlockState(absolute), synthesis);
            if (synthesis.getCurrentRecipe() == recipe && synthesis.getItem(SynthesisBlockEntity.OUTPUT).getCount() == recipe.count()) {
                chosen = recipe;
                break;
            }
        }
        helper.assertTrue(chosen != null, "no synthesis recipe with an output of more than one item");
        int slot = SynthesisBlockEntity.STORAGE_FIRST;
        for (SynthesisRecipes.Ingredient ingredient : chosen.ingredients()) {
            if (ingredient != null) {
                synthesis.setItem(slot++, ChemicalItem.stackFor(ingredient.chemical()).copyWithCount(ingredient.amount() * 2));
            }
        }
        synthesis.getEnergy().set(SynthesisBlockEntity.ENERGY_CAPACITY);
        net.minecraft.world.item.Item output = synthesis.getItem(SynthesisBlockEntity.OUTPUT).getItem();
        int batch = chosen.count();
        helper.setBlock(hopperPos, Blocks.HOPPER);
        helper.runAfterDelay(40, () -> {
            var hopper = helper.getBlockEntity(hopperPos, net.minecraft.world.level.block.entity.HopperBlockEntity.class);
            int taken = 0;
            for (int i = 0; i < hopper.getContainerSize(); i++) {
                if (hopper.getItem(i).is(output)) {
                    taken += hopper.getItem(i).getCount();
                }
            }
            helper.assertTrue(taken >= 2, "the hopper should have taken items of a batch of " + batch + ", it has " + taken);
            helper.assertTrue(taken <= 2 * batch, "the hopper took " + taken + " items from two batches of " + batch);
            helper.succeed();
        });
    }

    /**
     * No side reaches the recipe grid (ghost slots), so automation puts
     * chemicals into the storage only.
     */
    private static void synthesisAutomationSkipsGrid(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BlockRegistry.SYNTHESIS.get());
        SynthesisBlockEntity synthesis = helper.getBlockEntity(pos, SynthesisBlockEntity.class);
        net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> handler = synthesis.itemHandler(Direction.NORTH);
        var hydrogen = net.neoforged.neoforge.transfer.item.ItemResource.of(ChemicalItem.stackFor(Jenkins.get("Hydrogen")));
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertTrue(handler.insert(SynthesisBlockEntity.RECIPE_FIRST, hydrogen, 4, transaction) == 0, "automation put a chemical into the recipe grid");
            helper.assertTrue(handler.insert(hydrogen, 4, transaction) == 4, "automation should put the chemical into the storage");
            transaction.commit();
        }
        for (int slot = SynthesisBlockEntity.RECIPE_FIRST; slot < SynthesisBlockEntity.STORAGE_FIRST; slot++) {
            helper.assertTrue(synthesis.getItem(slot).isEmpty(), "the recipe grid slot " + slot + " got an item from automation");
        }
        helper.assertTrue(synthesis.getItem(SynthesisBlockEntity.STORAGE_FIRST).getCount() == 4, "the first storage slot should hold the 4 hydrogen");
        helper.succeed();
    }

    /**
     * Power use setting: with power use enabled (the default) a synthesis machine without energy cannot give its
     * output; with it disabled, the same machine gives it and uses no energy. The config value is restored before the test ends.
     */
    private static void powerUseConfigGatesSynthesis(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BlockRegistry.SYNTHESIS.get());
        SynthesisBlockEntity synthesis = helper.getBlockEntity(pos, SynthesisBlockEntity.class);
        SynthesisRecipes.Recipe recipe = SynthesisRecipes.RECIPES.get("minecraft:iron_ingot");
        helper.assertTrue(recipe != null, "no synthesis recipe for minecraft:iron_ingot");
        synthesis.setRecipe(recipe);
        int slot = SynthesisBlockEntity.STORAGE_FIRST;
        for (SynthesisRecipes.Ingredient ingredient : recipe.ingredients()) {
            if (ingredient != null) {
                synthesis.setItem(slot++, ChemicalItem.stackFor(ingredient.chemical()).copyWithCount(ingredient.amount()));
            }
        }
        BlockPos absolute = helper.absolutePos(pos);
        SynthesisBlockEntity.serverTick(helper.getLevel(), absolute, helper.getLevel().getBlockState(absolute), synthesis);
        helper.assertTrue(synthesis.getCurrentRecipe() == recipe, "the grid should match the iron ingot recipe");
        boolean before = minechem.Config.POWER_USE_ENABLED.get();
        try {
            minechem.Config.POWER_USE_ENABLED.set(true);
            helper.assertFalse(synthesis.canTakeOutputStack(false), "with power use enabled, a machine without energy gave its output");
            minechem.Config.POWER_USE_ENABLED.set(false);
            helper.assertTrue(synthesis.canTakeOutputStack(false), "with power use disabled, the machine should not need energy");
            helper.assertTrue(synthesis.energyNeeded() == 0, "with power use disabled, the recipe should need no energy");
        } finally {
            minechem.Config.POWER_USE_ENABLED.set(before);
        }
        helper.succeed();
    }

    /**
     * A click on the recipe grid
     * copies the cursor's stack (left: all of it, right: one more) and leaves the cursor as it was; a click with an empty
     * cursor clears the ghost and gives nothing. Breaking the machine drops the storage only, not the grid nor the output.
     */
    private static void synthesisGridHoldsGhosts(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BlockRegistry.SYNTHESIS.get());
        SynthesisBlockEntity synthesis = helper.getBlockEntity(pos, SynthesisBlockEntity.class);
        SynthesisMenu menu = new SynthesisMenu(0, player.getInventory(), synthesis);
        ItemStack hydrogen = ChemicalItem.stackFor(Jenkins.get("Hydrogen")).copyWithCount(5);
        net.minecraft.world.item.Item chemical = hydrogen.getItem();
        menu.setCarried(hydrogen.copy());
        menu.clicked(SynthesisMenu.GRID_START, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        helper.assertTrue(synthesis.getItem(SynthesisBlockEntity.RECIPE_FIRST).getCount() == 5, "a left click should copy the 5 hydrogen into the grid");
        helper.assertTrue(menu.getCarried().getCount() == 5, "the cursor should keep its 5 hydrogen, it has " + menu.getCarried().getCount());
        menu.clicked(SynthesisMenu.GRID_START, 1, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        helper.assertTrue(synthesis.getItem(SynthesisBlockEntity.RECIPE_FIRST).getCount() == 6, "a right click should add one to the ghost");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(SynthesisMenu.GRID_START, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        helper.assertTrue(synthesis.getItem(SynthesisBlockEntity.RECIPE_FIRST).isEmpty(), "a click with an empty cursor should clear the ghost");
        helper.assertTrue(menu.getCarried().isEmpty(), "the grid gave an item to the player");
        menu.clicked(SynthesisMenu.GRID_START, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, player);
        helper.assertTrue(player.getInventory().countItem(chemical) == 0, "a shift-click on the grid gave an item to the player");

        menu.setCarried(hydrogen.copy());
        menu.clicked(SynthesisMenu.GRID_START + 1, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        menu.setCarried(ItemStack.EMPTY);
        synthesis.setItem(SynthesisBlockEntity.STORAGE_FIRST, ChemicalItem.stackFor(Jenkins.get("Oxygen")).copyWithCount(3));
        helper.destroyBlock(pos);
        helper.assertItemEntityCountIs(chemical, pos, 3.0, 3);
        helper.succeed();
    }

    /**
     * Pouring a vial: in survival, eight vials of a molecule put its liquid in the air in front of
     * the clicked face and give back eight empty tubes. Water puts the vanilla water. A solid block in front is not replaced, and
     * no vial is used.
     */
    private static void vialPoursLiquid(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos toluene = new BlockPos(1, 1, 1);
        BlockPos water = new BlockPos(3, 1, 1);
        BlockPos solid = new BlockPos(5, 1, 1);
        helper.setBlock(toluene, Blocks.STONE);
        helper.setBlock(water, Blocks.STONE);
        helper.setBlock(solid, Blocks.STONE);
        helper.setBlock(solid.above(), Blocks.STONE);

        ItemStack toluenes = ChemicalItem.stackFor(Jenkins.get("Toluene"));
        toluenes.setCount(8);
        player.setItemInHand(InteractionHand.MAIN_HAND, toluenes);
        InteractionResult poured = pour(helper, player, toluene, toluenes);
        helper.assertTrue(poured == InteractionResult.SUCCESS, "a vial of toluene did not pour");
        helper.assertTrue(helper.getBlockState(toluene.above()).is(MoleculeFluids.blockFor("Toluene")), "toluene is not in the air above the stone");
        helper.assertTrue(toluenes.isEmpty(), "eight toluene vials were not used up");

        ItemStack waters = ChemicalItem.stackFor(Jenkins.get("Water"));
        waters.setCount(8);
        player.setItemInHand(InteractionHand.OFF_HAND, waters);
        helper.assertTrue(pour(helper, player, water, waters) == InteractionResult.SUCCESS, "a vial of water did not pour");
        helper.assertTrue(helper.getBlockState(water.above()).is(Blocks.WATER), "water did not place the vanilla water");

        ItemStack more = ChemicalItem.stackFor(Jenkins.get("Toluene"));
        more.setCount(8);
        player.setItemInHand(InteractionHand.OFF_HAND, more);
        helper.assertTrue(pour(helper, player, solid, more) == InteractionResult.PASS, "a solid block was replaced");
        helper.assertTrue(more.getCount() == 8, "vials were used against a solid block");
        helper.assertTrue(helper.getBlockState(solid.above()).is(Blocks.STONE), "the solid block above was changed");
        helper.assertTrue(player.getInventory().countItem(ItemRegistry.EMPTY_TUBE.get()) == 16, "two pours did not give 16 empty tubes");
        helper.succeed();
    }

    private static InteractionResult pour(GameTestHelper helper, ServerPlayer player, BlockPos clicked, ItemStack vials) {
        BlockPos absolute = helper.absolutePos(clicked);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        return vials.getItem().useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, vials, hit));
    }

    /**
     * Hydrogen is furnace fuel: the furnace fuel event gives it a burn time of 100 ticks. The event is posted the way
     * the furnace posts it, so the mod's listener sets the value.
     */
    private static void hydrogenIsFurnaceFuel(GameTestHelper helper) {
        ItemStack hydrogen = ChemicalItem.stackFor(Jenkins.get("Hydrogen"));
        FurnaceFuelBurnTimeEvent event = new FurnaceFuelBurnTimeEvent(hydrogen, 0, RecipeType.SMELTING, helper.getLevel().fuelValues());
        NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(event.getBurnTime() == 100, "hydrogen burns for " + event.getBurnTime() + " ticks, not 100");
        helper.succeed();
    }

    /**
     * Drinking a molecule uses one item and applies its pharmacology effects: cocaine gives
     * Nausea and Speed. An element is not drinkable, and the Delirium effect is registered.
     */
    private static void moleculeDrinkAppliesEffects(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack cocaine = ChemicalItem.stackFor(Jenkins.get("Cocaine"));
        cocaine.setCount(2);
        ChemicalItem item = (ChemicalItem) cocaine.getItem();
        item.finishUsingItem(cocaine, helper.getLevel(), player);
        helper.assertTrue(cocaine.getCount() == 1, "drinking did not use one cocaine item");
        helper.assertTrue(player.hasEffect(MobEffects.NAUSEA), "cocaine gave no Nausea");
        helper.assertTrue(player.hasEffect(MobEffects.SPEED), "cocaine gave no Speed");

        ItemStack hydrogen = ChemicalItem.stackFor(Jenkins.get("Hydrogen"));
        player.setItemInHand(InteractionHand.MAIN_HAND, hydrogen);
        helper.assertTrue(hydrogen.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND) == InteractionResult.PASS,
            "an element was drinkable");
        player.setItemInHand(InteractionHand.MAIN_HAND, cocaine);
        helper.assertTrue(cocaine.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME,
            "a molecule was not drinkable");
        helper.assertTrue(MobEffectRegistry.DELIRIUM.get() != null, "the Delirium mob effect is not registered");
        helper.succeed();
    }

    /**
     * The height of the multiblock tests' structures above the helper's origin. A structure is larger than the test's own area,
     * and the areas of the other tests of the batch are next to it, so the structures are raised: a neighbour's clearing or
     * blocks must not reach them.
     */
    private static final int REACTOR_HEIGHT = 20;

    /**
     * Places the walls and the manager of a reactor. The structure's own x and z are the helper's; its origin is raised by
     * {@link #REACTOR_HEIGHT}. The box is cleared first, so nothing left by an earlier run counts as part of the structure.
     */
    private static void placeReactor(GameTestHelper helper, ReactorBlueprint blueprint, Block manager) {
        for (int x = 0; x < blueprint.xSize(); x++) {
            for (int y = 0; y < blueprint.ySize(); y++) {
                for (int z = 0; z < blueprint.zSize(); z++) {
                    helper.setBlock(new BlockPos(x, REACTOR_HEIGHT + y, z), Blocks.AIR);
                }
            }
        }
        for (int x = 0; x < blueprint.xSize(); x++) {
            for (int y = 0; y < blueprint.ySize(); y++) {
                for (int z = 0; z < blueprint.zSize(); z++) {
                    BlockPos pos = new BlockPos(x, REACTOR_HEIGHT + y, z);
                    if (blueprint.isManager(x, y, z)) {
                        helper.setBlock(pos, manager);
                    } else if (blueprint.idAt(x, y, z) == ReactorBlueprint.WALL_1) {
                        helper.setBlock(pos, BlockRegistry.FUSION_WALL_1.get());
                    } else if (blueprint.idAt(x, y, z) == ReactorBlueprint.WALL_2) {
                        helper.setBlock(pos, BlockRegistry.FUSION_WALL_2.get());
                    }
                }
            }
        }
    }

    /**
     * Every roll of a dungeon chest's loot holds exactly one blueprint: the modifier adds a pool with one roll and the two
     * blueprints at weight 10 each.
     */
    private static void dungeonChestsHoldBlueprint(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LootTable table = level.getServer().reloadableRegistries().getLootTable(
            ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("minecraft", "chests/simple_dungeon")));
        // Estimate: the pool weights are 10 + 10 + 146 (the vanilla simple dungeon's first pool total),
        // so about 12 % of the dungeon chests get one blueprint. Counted over 2000 rolls: 190 to 290 expected.
        int rolls = 2000;
        int found = 0;
        for (int roll = 0; roll < rolls; roll++) {
            LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, helper.absolutePos(BlockPos.ZERO).getCenter())
                .create(LootContextParamSets.CHEST);
            long blueprints = table.getRandomItems(params).stream()
                .filter(stack -> stack.is(ItemRegistry.BLUEPRINT_FUSION.get()) || stack.is(ItemRegistry.BLUEPRINT_FISSION.get()))
                .count();
            found += blueprints > 0 ? 1 : 0;
        }
        helper.assertTrue(found >= 190 && found <= 290, "blueprints in " + found + " of " + rolls + " dungeon chests, not about 12 %");
        helper.succeed();
    }

    /**
     * The lens and projector recipes load, and the projector crafts from its ingredients: " I ", "GPL", " I " with a redstone lamp,
     * a glass pane and a projector lens.
     */
    private static void lensAndProjectorRecipes(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getServer().getRecipeManager();
        for (String id : List.of("concave_lens", "convex_lens", "microscope_lens", "projector_lens", "blueprint_projector",
            "blueprint_fusion", "blueprint_fission")) {
            helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(Compendium.Naming.id, id))).isPresent(),
                "no recipe " + id);
        }
        List<ItemStack> grid = List.of(
            ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY,
            new ItemStack(Items.REDSTONE_LAMP), new ItemStack(Items.GLASS_PANE), new ItemStack(ItemRegistry.PROJECTOR_LENS.get()),
            ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY);
        helper.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, grid), helper.getLevel()).isPresent(),
            "the projector does not craft from its ingredients");
        helper.succeed();
    }

    /**
     * The microscope (optical lens in the middle column, glass pane, iron) and the leaded chest (nine lead, chest in the
     * middle).
     */
    private static void microscopeAndChestRecipes(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getServer().getRecipeManager();
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack lead = ChemicalItem.stackFor(Jenkins.get(82));
        CraftingInput microscope = CraftingInput.of(3, 3, List.of(
            ItemStack.EMPTY, new ItemStack(ItemRegistry.MICROSCOPE_LENS.get()), iron,
            ItemStack.EMPTY, new ItemStack(Items.GLASS_PANE), iron,
            iron, iron, iron));
        CraftingInput chest = CraftingInput.of(3, 3, List.of(
            lead, lead, lead, lead, new ItemStack(Items.CHEST), lead, lead, lead, lead));
        helper.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, microscope, helper.getLevel()).isPresent(),
            "the microscope does not craft from its ingredients");
        helper.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, chest, helper.getLevel()).isPresent(),
            "the leaded chest does not craft from lead and a chest");
        ItemStack result = recipes.getRecipeFor(RecipeType.CRAFTING, chest, helper.getLevel()).get().value().assemble(chest);
        helper.assertTrue(result.is(BlockRegistry.LEADED_CHEST_ITEM.get()), "the leaded chest recipe gives " + result);
        helper.succeed();
    }

    /**
     * The atomic manipulator (pistons around an iron block) and the two machines that take it (iron, the
     * manipulator in the middle, redstone; the synthesis takes diamond in the bottom right corner instead of iron).
     */
    private static void machineRecipesCraft(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getServer().getRecipeManager();
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack redstone = new ItemStack(Items.REDSTONE);
        ItemStack manipulator = new ItemStack(ItemRegistry.ATOMIC_MANIPULATOR.get());
        CraftingInput manipulatorGrid = CraftingInput.of(3, 3, List.of(
            new ItemStack(Items.PISTON), new ItemStack(Items.PISTON), new ItemStack(Items.PISTON),
            new ItemStack(Items.PISTON), new ItemStack(Items.IRON_BLOCK), new ItemStack(Items.PISTON),
            new ItemStack(Items.PISTON), new ItemStack(Items.PISTON), new ItemStack(Items.PISTON)));
        CraftingInput decomposer = CraftingInput.of(3, 3, List.of(
            iron, iron, iron, iron, manipulator, iron, iron, redstone, iron));
        CraftingInput synthesis = CraftingInput.of(3, 3, List.of(
            iron, redstone, iron, iron, manipulator, iron, iron, new ItemStack(Items.DIAMOND), iron));
        helper.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, manipulatorGrid, helper.getLevel()).isPresent(),
            "the atomic manipulator does not craft from pistons and an iron block");
        helper.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, decomposer, helper.getLevel()).isPresent(),
            "the chemical decomposer does not craft from its ingredients");
        helper.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, synthesis, helper.getLevel()).isPresent(),
            "the synthesis does not craft from its ingredients");
        helper.succeed();
    }

    /**
     * The fusion wall recipes use element ingredients: 16 walls of each, with
     * iron and lead (wall 1), or iron, tungsten and beryllium (wall 2). A different element must not match.
     */
    private static void elementRecipesCraftWalls(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getServer().getRecipeManager();
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack lead = ChemicalItem.stackFor(Jenkins.get(82));
        ItemStack tungsten = ChemicalItem.stackFor(Jenkins.get(74));
        ItemStack beryllium = ChemicalItem.stackFor(Jenkins.get(4));
        ItemStack mercury = ChemicalItem.stackFor(Jenkins.get(80));

        CraftingInput wall1 = CraftingInput.of(3, 3, List.of(
            iron, tungsten, iron, iron, tungsten, iron, iron, tungsten, iron));
        CraftingInput leadWall1 = CraftingInput.of(3, 3, List.of(
            iron, lead, iron, iron, lead, iron, iron, lead, iron));
        CraftingInput wall2 = CraftingInput.of(3, 3, List.of(
            iron, tungsten, iron, iron, beryllium, iron, iron, tungsten, iron));
        CraftingInput wrongWall1 = CraftingInput.of(3, 3, List.of(
            iron, mercury, iron, iron, mercury, iron, iron, mercury, iron));

        helper.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, wall1, helper.getLevel()).isPresent(),
            "the first fusion wall does not craft from iron and tungsten");
        helper.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, leadWall1, helper.getLevel()).isEmpty(),
            "the first fusion wall still crafts from lead");
        helper.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, wall2, helper.getLevel()).isPresent(),
            "the second fusion wall does not craft from iron, tungsten and beryllium");
        helper.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, wrongWall1, helper.getLevel()).isEmpty(),
            "the first fusion wall crafts from mercury, which is not lead");
        // The lead ingredient of the hazmat suit and the leaded chest is shown as lead, not as the generic chemical item.
        SlotDisplay leadDisplay = new ElementIngredient(82).display();
        helper.assertTrue(leadDisplay instanceof SlotDisplay.ItemStackSlotDisplay shown
                && ChemicalItem.getChemicalBase(shown.stack().create()) != null
                && ChemicalItem.getChemicalBase(shown.stack().create()).fullName.equals(Jenkins.get(82).fullName),
            "the lead ingredient is not shown as lead");
        int count = recipes.getRecipeFor(RecipeType.CRAFTING, wall1, helper.getLevel()).get().value()
            .assemble(wall1).getCount();
        helper.assertTrue(count == 16, "the first fusion wall recipe gives " + count + " walls, not 16");
        helper.succeed();
    }

    /**
     * The projector's block state "blueprint" follows its slot: true while a blueprint is in, which
     * picks the lit texture of the model.
     */
    private static void projectorLitWithBlueprint(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, BlockRegistry.BLUEPRINT_PROJECTOR.get());
        BlueprintProjectorBlockEntity projector = helper.getBlockEntity(pos, BlueprintProjectorBlockEntity.class);
        helper.assertTrue(!helper.getBlockState(pos).getValue(BlueprintProjectorBlock.HAS_BLUEPRINT), "an empty projector is lit");
        projector.setItem(0, new ItemStack(ItemRegistry.BLUEPRINT_FUSION.get()));
        helper.assertTrue(helper.getBlockState(pos).getValue(BlueprintProjectorBlock.HAS_BLUEPRINT), "a projector with a blueprint is not lit");
        projector.setItem(0, ItemStack.EMPTY);
        helper.assertTrue(!helper.getBlockState(pos).getValue(BlueprintProjectorBlock.HAS_BLUEPRINT), "the projector stays lit after its blueprint is removed");
        helper.succeed();
    }

    /**
     * The projector's sound is registered under its own id, "minechem:projector", which sounds.json names and the file
     * sounds/projector.ogg holds. Playing it is client-side, not checked here.
     */
    private static void projectorSoundIsRegistered(GameTestHelper helper) {
        Identifier id = Identifier.fromNamespaceAndPath(Compendium.Naming.id, "projector");
        helper.assertTrue(SoundRegistry.PROJECTOR.get().location().equals(id), "the projector sound has another id");
        helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(id), "the projector sound is not in the sound registry");
        helper.succeed();
    }

    /** Stores energy in a manager (the test's stand-in for an energy source next to it). */
    private static void insertEnergy(SimpleEnergyHandler energy, int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            energy.insert(amount, transaction);
            transaction.commit();
        }
    }

    /**
     * A complete fission reactor is linked: its manager is complete after its first check, and a wall gives it the manager.
     */
    /**
     * Config: debugMode is off by default, and recreationalChemicalEffects (on by default) guards the brown mushroom
     * and nether wart decompositions. The config value is restored before the test ends.
     */
    private static void configDefaultsAndRecreationalRecipes(GameTestHelper helper) {
        helper.assertFalse(minechem.Config.DEBUG_MODE.getDefault(), "debugMode should be off by default");
        helper.assertTrue(minechem.Config.RECREATIONAL_CHEMICAL_EFFECTS.getDefault(), "recreationalChemicalEffects should be on by default");
        boolean before = minechem.Config.RECREATIONAL_CHEMICAL_EFFECTS.get();
        try {
            minechem.Config.RECREATIONAL_CHEMICAL_EFFECTS.set(false);
            minechem.apparatus.decomposer.DecomposerRecipes off = bundledDecomposerRecipes();
            helper.assertTrue(off.get("minecraft:brown_mushroom") == null && off.get("minecraft:nether_wart") == null,
                "with recreationalChemicalEffects off, the brown mushroom and the nether wart should not decompose");
            helper.assertTrue(off.get("minecraft:red_mushroom") != null, "the red mushroom is not a recreational recipe");
            minechem.Config.RECREATIONAL_CHEMICAL_EFFECTS.set(true);
            helper.assertTrue(bundledDecomposerRecipes().get("minecraft:brown_mushroom") != null, "with the setting on, the brown mushroom decomposes");
        } finally {
            minechem.Config.RECREATIONAL_CHEMICAL_EFFECTS.set(before);
        }
        helper.succeed();
    }

    private static minechem.apparatus.decomposer.DecomposerRecipes bundledDecomposerRecipes() {
        minechem.apparatus.decomposer.DecomposerRecipes table = new minechem.apparatus.decomposer.DecomposerRecipes();
        try (java.io.InputStream stream = MinechemGameTests.class.getResourceAsStream(minechem.apparatus.decomposer.DecomposerRecipeLoader.RESOURCE)) {
            minechem.apparatus.decomposer.DecomposerRecipeLoader.load(table,
                new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8), true);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        return table;
    }

    /**
     * The client's menu factory finds the block entity at the packet's position; when there is none (not synced yet), a blank
     * one of the right type stands in instead of a null machine.
     */
    private static void menuFactoryWithoutBlockEntity(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        net.minecraft.network.RegistryFriendlyByteBuf buffer = new net.minecraft.network.RegistryFriendlyByteBuf(
            io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        buffer.writeBlockPos(helper.absolutePos(new BlockPos(1, 1, 1)));
        SynthesisMenu menu = MenuRegistry.SYNTHESIS.get().create(0, player.getInventory(), buffer);
        helper.assertTrue(menu.machine() != null, "the synthesis menu was built with no machine");
        helper.succeed();
    }

    /** A journal opened from the off hand: a click on its list sets the active item of that journal, not of the main hand. */
    private static void journalMenuUsesItsHand(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack journal = new ItemStack(ItemRegistry.CHEMIST_JOURNAL.get());
        minechem.item.chemistjournal.ChemistJournalItem.addDiscovered(journal, "Hydrogen");
        player.setItemInHand(InteractionHand.OFF_HAND, journal);
        minechem.item.chemistjournal.ChemistJournalMenu menu = new minechem.item.chemistjournal.ChemistJournalMenu(
            0, player.getInventory(), List.of("Hydrogen"), "", InteractionHand.OFF_HAND);
        helper.assertTrue(menu.clickMenuButton(player, 0), "the click on the off-hand journal's list was refused");
        helper.assertTrue("Hydrogen".equals(minechem.item.chemistjournal.ChemistJournalItem.activeName(player.getOffhandItem())),
            "the off-hand journal should have Hydrogen active");
        helper.succeed();
    }

    /**
     * A fermented spider eye put in the microscope with a journal discovers the fermented spider eye and what it decomposes
     * into (polonium, ethyl alcohol).
     */
    private static void microscopeDiscoversProducts(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, BlockRegistry.MICROSCOPE.get());
        MicroscopeBlockEntity microscope = helper.getBlockEntity(pos, MicroscopeBlockEntity.class);
        ItemStack journal = new ItemStack(ItemRegistry.CHEMIST_JOURNAL.get());
        microscope.setItem(MicroscopeRules.JOURNAL_SLOT, journal);
        microscope.setItem(MicroscopeRules.INPUT_SLOT, new ItemStack(net.minecraft.world.item.Items.FERMENTED_SPIDER_EYE));
        List<String> discovered = journal.getOrDefault(ItemRegistry.JOURNAL_DISCOVERED.get(), List.of());
        helper.assertTrue(discovered.contains("Polonium"), "the microscope should discover polonium from a fermented spider eye: " + discovered);
        helper.assertTrue(discovered.contains("Ethyl Alcohol"), "and ethyl alcohol: " + discovered);
        helper.succeed();
    }

    /** A reactor wall takes items only while allowAutomation is on (restored after). */
    private static void reactorWallFollowsAllowAutomation(GameTestHelper helper) {
        placeReactor(helper, ReactorBlueprint.FISSION, BlockRegistry.FISSION_REACTOR.get());
        helper.runAfterDelay(3, () -> {
            ReactorWallBlockEntity wall = helper.getBlockEntity(new BlockPos(0, REACTOR_HEIGHT, 0), ReactorWallBlockEntity.class);
            var oxygen = net.neoforged.neoforge.transfer.item.ItemResource.of(ChemicalItem.stackFor(Jenkins.get(8)));
            boolean before = minechem.Config.ALLOW_AUTOMATION.get();
            try {
                minechem.Config.ALLOW_AUTOMATION.set(true);
                try (Transaction transaction = Transaction.openRoot()) {
                    helper.assertTrue(wall.itemHandler(Direction.NORTH).insert(oxygen, 1, transaction) == 1, "the wall should take an element with automation on");
                }
                minechem.Config.ALLOW_AUTOMATION.set(false);
                try (Transaction transaction = Transaction.openRoot()) {
                    helper.assertTrue(wall.itemHandler(Direction.NORTH).insert(oxygen, 1, transaction) == 0, "the wall took an element with automation off");
                }
            } finally {
                minechem.Config.ALLOW_AUTOMATION.set(before);
            }
            helper.succeed();
        });
    }

    private static void fissionReactorLinksWalls(GameTestHelper helper) {
        placeReactor(helper, ReactorBlueprint.FISSION, BlockRegistry.FISSION_REACTOR.get());
        helper.runAfterDelay(3, () -> {
            FissionReactorBlockEntity fission = helper.getBlockEntity(new BlockPos(2, REACTOR_HEIGHT + 2, 2), FissionReactorBlockEntity.class);
            helper.assertTrue(fission.isComplete(), "the complete fission reactor is not complete");
            ReactorWallBlockEntity wall = helper.getBlockEntity(new BlockPos(0, REACTOR_HEIGHT, 0), ReactorWallBlockEntity.class);
            helper.assertTrue(wall.linkedManager() == fission, "a wall of the reactor is not linked to its manager");
            helper.succeed();
        });
    }

    /**
     * Fission splits an element of atomic number 8 into two of atomic number 4, and uses 8 * 100 energy.
     */
    private static void fissionSplitsElement(GameTestHelper helper) {
        placeReactor(helper, ReactorBlueprint.FISSION, BlockRegistry.FISSION_REACTOR.get());
        FissionReactorBlockEntity fission = helper.getBlockEntity(new BlockPos(2, REACTOR_HEIGHT + 2, 2), FissionReactorBlockEntity.class);
        fission.setItem(FissionReactorBlockEntity.INPUT, ChemicalItem.stackFor(Jenkins.get(8)));
        insertEnergy(fission.getEnergy(), 1000);
        helper.runAfterDelay(5, () -> {
            ItemStack output = fission.getItem(FissionReactorBlockEntity.OUTPUT);
            helper.assertTrue(!output.isEmpty() && output.getCount() == 2, "the fission gave " + output.getCount() + " items, not 2");
            helper.assertTrue(output.isEmpty() || ChemicalItem.getChemicalBase(output).equals(Jenkins.get(4)),
                "the fission did not give element 4");
            helper.assertTrue(fission.getEnergy().getAmountAsInt() == 200, "the fission used the wrong energy: " + fission.getEnergy().getAmountAsInt());
            helper.succeed();
        });
    }

    /**
     * Fusion of hydrogen (1) and helium (2) gives lithium (3) and uses (1 + 2) * 100 energy.
     */
    private static void fusionFusesElements(GameTestHelper helper) {
        placeReactor(helper, ReactorBlueprint.FUSION, BlockRegistry.FUSION_REACTOR.get());
        FusionReactorBlockEntity fusion = helper.getBlockEntity(new BlockPos(6, REACTOR_HEIGHT + 1, 6), FusionReactorBlockEntity.class);
        fusion.setItem(FusionReactorBlockEntity.INPUT_LEFT, ChemicalItem.stackFor(Jenkins.get(1)));
        fusion.setItem(FusionReactorBlockEntity.INPUT_RIGHT, ChemicalItem.stackFor(Jenkins.get(2)));
        insertEnergy(fusion.getEnergy(), 1000);
        helper.runAfterDelay(5, () -> {
            ItemStack output = fusion.getItem(FusionReactorBlockEntity.OUTPUT);
            helper.assertTrue(!output.isEmpty() && output.getCount() == 1, "the fusion gave no lithium");
            helper.assertTrue(output.isEmpty() || ChemicalItem.getChemicalBase(output).equals(Jenkins.get(3)), "the fusion did not give element 3");
            helper.assertTrue(fusion.getItem(FusionReactorBlockEntity.INPUT_LEFT).isEmpty()
                && fusion.getItem(FusionReactorBlockEntity.INPUT_RIGHT).isEmpty(), "the fusion did not use its inputs");
            helper.assertTrue(fusion.getEnergy().getAmountAsInt() == 700, "the fusion used the wrong energy: " + fusion.getEnergy().getAmountAsInt());
            helper.succeed();
        });
    }

    /**
     * The projector with a fission blueprint puts ghost blocks where walls go (column by column), and when the walls are all
     * there, it builds the manager at its cell. The projector faces east, so the
     * blueprint's cells are the frame's: the manager is at (4, 2, 8) relative to the test.
     */
    private static void projectorBuildsReactor(GameTestHelper helper) {
        BlockPos projector = new BlockPos(0, REACTOR_HEIGHT, 8);
        helper.setBlock(projector, BlockRegistry.BLUEPRINT_PROJECTOR.get().defaultBlockState().setValue(BasicBlockContainer.FACING, Direction.EAST));
        BlueprintProjectorBlockEntity be = helper.getBlockEntity(projector, BlueprintProjectorBlockEntity.class);
        be.setItem(0, new ItemStack(ItemRegistry.BLUEPRINT_FISSION.get()));
        ProjectionFrame frame = ProjectionFrame.of(helper.absolutePos(projector), Direction.EAST, ReactorBlueprint.FISSION);
        for (int x = 0; x < ReactorBlueprint.FISSION.xSize(); x++) {
            for (int y = 0; y < ReactorBlueprint.FISSION.ySize(); y++) {
                for (int z = 0; z < ReactorBlueprint.FISSION.zSize(); z++) {
                    helper.getLevel().setBlock(frame.at(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        helper.runAfterDelay(2, () -> {
            BlockPos firstWall = frame.at(0, 0, 0);
            helper.assertTrue(helper.getLevel().getBlockState(firstWall).is(BlockRegistry.GHOST_BLOCK.get()),
                "no ghost block at the first wall of the blueprint: the cell holds " + helper.getLevel().getBlockState(firstWall)
                + ", the projector holds " + be.blueprint() + " at " + helper.absolutePos(projector));
            helper.assertTrue(helper.getLevel().getBlockState(firstWall).getValue(GhostBlock.WALL) == ReactorBlueprint.FISSION.idAt(0, 0, 0),
                "the ghost at the first wall does not stand for its wall");
            for (int x = 0; x < ReactorBlueprint.FISSION.xSize(); x++) {
                for (int y = 0; y < ReactorBlueprint.FISSION.ySize(); y++) {
                    for (int z = 0; z < ReactorBlueprint.FISSION.zSize(); z++) {
                        if (ReactorBlueprint.FISSION.idAt(x, y, z) == ReactorBlueprint.WALL_2) {
                            helper.getLevel().setBlock(frame.at(x, y, z), BlockRegistry.FUSION_WALL_2.get().defaultBlockState(), 3);
                        }
                    }
                }
            }
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(helper.getLevel().getBlockEntity(frame.at(2, 2, 2)) instanceof FissionReactorBlockEntity,
                    "the projector did not build the fission reactor");
                helper.succeed();
            });
        });
    }

    /** A code-only test instance: the body runs in the test's structure. */
    private static final class Check extends GameTestInstance {
        /**
         * Code-only: encodes as an empty map and refuses to decode. A codec built from a throwing supplier breaks the
         * client join, because the registry's codec string is built from it during the known-packs negotiation.
         */
        static final MapCodec<Check> CODEC = MapCodec.unit(Unit.INSTANCE).xmap(
            ignored -> new Check(PLACEHOLDER, helper -> helper.fail("minechem code test: run it on the gameTestServer")),
            check -> Unit.INSTANCE);

        private final Consumer<GameTestHelper> body;

        Check(TestData<Holder<TestEnvironmentDefinition<?>>> info, Consumer<GameTestHelper> body) {
            super(info);
            this.body = body;
        }

        @Override
        public void run(GameTestHelper helper) {
            body.accept(helper);
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("minechem code test");
        }
    }
}
