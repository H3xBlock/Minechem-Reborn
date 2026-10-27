package minechem;

import minechem.apparatus.decomposer.DecomposerBlockEntity;
import minechem.apparatus.decomposer.DecomposerRecipeLoader;
import minechem.apparatus.decomposer.DecomposerOreBinder;
import minechem.apparatus.synthesis.OreSynthesisBinder;
import minechem.apparatus.synthesis.SynthesisRecipeLoader;
import minechem.apparatus.synthesis.SynthesisRecipes;
import minechem.handler.ElementHandler;
import minechem.handler.FuelHandler;
import minechem.fluid.MoleculeFluids;
import minechem.handler.MoleculeHandler;
import minechem.registry.MobEffectRegistry;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import minechem.registry.BlockEntityRegistry;
import minechem.registry.BlockRegistry;
import minechem.registry.CreativeTabRegistry;
import minechem.registry.ItemRegistry;
import minechem.registry.MenuRegistry;
import minechem.registry.RecipeRegistry;
import minechem.registry.SoundRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Minechem for NeoForge 26.1.2. Original by jakimfett, DBaJ non-commercial license, see LICENSE.md.
 * The value here must match modId in META-INF/neoforge.mods.toml.
 */
@Mod(Minechem.MODID)
public class Minechem {
    /** Mirrors Compendium.Naming.id. */
    public static final String MODID = Compendium.Naming.id;
    public static final Logger LOGGER = LogUtils.getLogger();

    public Minechem(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        ItemRegistry.COMPONENTS.register(modEventBus);
        ItemRegistry.ITEMS.register(modEventBus);
        BlockRegistry.BLOCKS.register(modEventBus);
        BlockEntityRegistry.BLOCK_ENTITY_TYPES.register(modEventBus);
        MenuRegistry.MENUS.register(modEventBus);
        MobEffectRegistry.MOB_EFFECTS.register(modEventBus);
        MoleculeFluids.register(modEventBus);
        CreativeTabRegistry.TABS.register(modEventBus);
        RecipeRegistry.SERIALIZERS.register(modEventBus);
        RecipeRegistry.INGREDIENT_TYPES.register(modEventBus);
        SoundRegistry.SOUNDS.register(modEventBus);
        minechem.registry.WorldgenRegistry.register(modEventBus);
        // ComputerCraft is optional: its classes are touched only when it is loaded (see compat/computercraft).
        if (net.neoforged.fml.ModList.get().isLoaded("computercraft")) {
            minechem.compat.computercraft.ComputerCraftCompat.register(modEventBus);
        }

        // GameTests: registered only when the run asks for them (system property minechem.gametests, set by the client, server
        // and gameTestServer runs of build.gradle), so a published jar registers no test and no test instance type. A client
        // and its server must agree: the test instances are synced to the client by their type.
        if (Boolean.getBoolean("minechem.gametests")) {
            minechem.gametest.MinechemGameTests.TYPES.register(modEventBus);
            modEventBus.addListener(RegisterGameTestsEvent.class, minechem.gametest.MinechemGameTests::register);
        }

        // Dev tools (a pickaxe that mines 5 x 5 and keeps the uranium ore, and /minechem commands): only the dev client run.
        if (Boolean.getBoolean("minechem.devtools")) {
            minechem.devtools.DevTools.register(modEventBus);
        }

        // Data loads at common setup, after the config is read (its useDefault* switches decide the file). Elements come
        // first: molecules reference elements by symbol.
        modEventBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(() -> {
            ElementHandler.init();
            MoleculeHandler.init();
            DecomposerRecipeLoader.loadBundled(DecomposerBlockEntity.RECIPES);
            SynthesisRecipeLoader.loadBundled(SynthesisRecipes.RECIPES);
            minechem.fluid.ChemicalBucketDispenseBehavior.register();
            minechem.fluid.FluidChemicalDispenseBehavior.register();
        }));

        modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerBlockEntity(
            Capabilities.Fluid.BLOCK,
            BlockEntityRegistry.DECOMPOSER.get(),
            (blockEntity, side) -> blockEntity.getFluidTank()));
        modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerBlockEntity(
            Capabilities.Energy.BLOCK,
            BlockEntityRegistry.DECOMPOSER.get(),
            (blockEntity, side) -> blockEntity.getEnergy()));
        modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerBlockEntity(
            Capabilities.Energy.BLOCK,
            BlockEntityRegistry.SYNTHESIS.get(),
            (blockEntity, side) -> blockEntity.getEnergy()));

        // OreDictionary synthesis recipes come from the item tags, which are bound when the tags are updated (server start,
        // reload). Each bind replaces the recipes of the previous one. The tables are static and, in single player, shared by
        // the server and the client threads, so only the update that owns static data rebinds them (shouldUpdateStaticData:
        // the server's own load, or a client connected to a remote server).
        NeoForge.EVENT_BUS.addListener(TagsUpdatedEvent.class, event -> {
            if (event.shouldUpdateStaticData()) {
                OreSynthesisBinder.bind();
            }
        });
        NeoForge.EVENT_BUS.addListener(TagsUpdatedEvent.class, event -> {
            if (event.shouldUpdateStaticData()) {
                DecomposerOreBinder.bind();
            }
        });

        // Furnace fuel: the elements and molecules that are registered as fuel.
        NeoForge.EVENT_BUS.addListener(FurnaceFuelBurnTimeEvent.class, FuelHandler::onFurnaceFuel);

        // Synthesis automation: hoppers and pipes reach the machine's slots through its item handlers.
        modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerBlockEntity(
            Capabilities.Item.BLOCK,
            BlockEntityRegistry.SYNTHESIS.get(),
            (blockEntity, side) -> blockEntity.itemHandler(side)));

        // Hoppers and pipes reach the other machines too. The decomposer uses side rules (top: input; other
        // sides: outputs); the microscope and the leaded chest are plain inventories with no side rules.
        modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerBlockEntity(
            Capabilities.Item.BLOCK,
            BlockEntityRegistry.DECOMPOSER.get(),
            (blockEntity, side) -> side == null
                ? VanillaContainerWrapper.of(blockEntity)
                : new WorldlyContainerWrapper(blockEntity, side)));
        modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerBlockEntity(
            Capabilities.Item.BLOCK,
            BlockEntityRegistry.MICROSCOPE.get(),
            (blockEntity, side) -> VanillaContainerWrapper.of(blockEntity)));
        modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerBlockEntity(
            Capabilities.Item.BLOCK,
            BlockEntityRegistry.LEADED_CHEST.get(),
            (blockEntity, side) -> VanillaContainerWrapper.of(blockEntity)));

        // Reactors: a wall shows its manager's items to its neighbours, and stores energy for the manager.
        // The managers take energy from any side.
        modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerBlockEntity(
            Capabilities.Item.BLOCK,
            BlockEntityRegistry.REACTOR_WALL.get(),
            (blockEntity, side) -> blockEntity.itemHandler(side)));
        modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerBlockEntity(
            Capabilities.Energy.BLOCK,
            BlockEntityRegistry.REACTOR_WALL.get(),
            (blockEntity, side) -> blockEntity.getEnergy()));
        modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerBlockEntity(
            Capabilities.Energy.BLOCK,
            BlockEntityRegistry.FUSION_REACTOR.get(),
            (blockEntity, side) -> blockEntity.getEnergy()));
        modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerBlockEntity(
            Capabilities.Energy.BLOCK,
            BlockEntityRegistry.FISSION_REACTOR.get(),
            (blockEntity, side) -> blockEntity.getEnergy()));

        LOGGER.info("Minechem: elements, molecules and chemical items registered");
    }
}
