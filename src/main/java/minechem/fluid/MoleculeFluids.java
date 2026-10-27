package minechem.fluid;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import minechem.Compendium;
import minechem.radiation.RadiationEnum;
import minechem.radiation.RadiationRules;
import minechem.registry.ItemRegistry;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The molecule liquids: every molecule except water gets a fluid, a flowing fluid and a block. Water is the vanilla water,
 * which places flowing water for it.
 *
 * <p>The list is read from the bundled molecule data when the mod is constructed, because fluids are registered then. A
 * molecule that is not in the bundled data has no fluid. Each of these has a bucket item (see {@link #entries()}).
 */
public final class MoleculeFluids {
    private static final String DATA = "/assets/minechem/data/moleculesData.json";
    private static final String WATER = "Water";

    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Compendium.Naming.id);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Compendium.Naming.id);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Compendium.Naming.id);
    public static final DeferredHolder<FluidType, FluidType> MOLECULE_TYPE = FLUID_TYPES.register("molecule",
        () -> new FluidType(FluidType.Properties.create().descriptionId("fluid.minechem.molecule")
            .canSwim(true).canDrown(true).isWaterLike(true).motionScale(0.014).canExtinguish(true).canConvertToSource(true).supportsBoating(true)
            .pathType(net.minecraft.world.level.pathfinder.PathType.WATER).adjacentPathType(null)));

    /** The type of a gaseous molecule: the liquid's, but lighter than air (density -10), so the gas rises. */
    public static final DeferredHolder<FluidType, FluidType> MOLECULE_GAS_TYPE = FLUID_TYPES.register("molecule_gas",
        () -> new FluidType(FluidType.Properties.create().descriptionId("fluid.minechem.molecule")
            .canSwim(true).canDrown(true).isWaterLike(true).motionScale(0.014).canExtinguish(true).canConvertToSource(true).supportsBoating(true)
            .pathType(net.minecraft.world.level.pathfinder.PathType.WATER).adjacentPathType(null).density(-10)));

    /** One molecule's fluid: its colour (0xRRGGBB) and its registered objects. */
    public record Entry(String molecule, int colour, DeferredItem<BucketItem> bucket, DeferredHolder<Fluid, MoleculeFluid.Source> still,
                        DeferredHolder<Fluid, MoleculeFluid.Flowing> flowing, DeferredHolder<Block, MoleculeFluidBlock> block) {
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final Map<String, Entry> BY_MOLECULE = new HashMap<>();

    private MoleculeFluids() {
    }

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        BLOCKS.register(modEventBus);
        for (Map.Entry<String, MoleculeData> molecule : readMolecules().entrySet()) {
            if (!WATER.equals(molecule.getKey())) {
                addEntry(molecule.getKey(), molecule.getValue());
            }
        }
        ElementFluids.register();
    }

    /** A molecule of the data: its colour, its formula (the formula gives the radioactivity) and its form (solid, liquid, gas). */
    private record MoleculeData(int colour, String formula, String form) {
        boolean solid() {
            return "solid".equals(form);
        }
    }

    private static void addEntry(String molecule, MoleculeData data) {
        String slug = slug(molecule);
        // The holders exist before their objects are built, so the properties can refer to all three.
        DeferredHolder<Fluid, MoleculeFluid.Source> still = DeferredHolder.create(Registries.FLUID, id("molecule_" + slug));
        DeferredHolder<Fluid, MoleculeFluid.Flowing> flowing = DeferredHolder.create(Registries.FLUID, id("flowing_molecule_" + slug));
        DeferredHolder<Block, MoleculeFluidBlock> block = DeferredHolder.create(Registries.BLOCK, id("molecule_" + slug + "_block"));
        // The bucket is the fluid's bucket, so picking the fluid up with a bucket gives it (vanilla LiquidBlock.pickupBlock).
        DeferredHolder<Item, BucketItem> bucketHolder = DeferredHolder.create(Registries.ITEM, id("bucket_" + slug));
        BaseFlowingFluid.Properties properties = fluidProperties("gas".equals(data.form()) ? MOLECULE_GAS_TYPE : MOLECULE_TYPE,
            still, flowing).block(block).bucket(bucketHolder);
        FLUIDS.register("molecule_" + slug, () -> new MoleculeFluid.Source(properties, data.solid()));
        FLUIDS.register("flowing_molecule_" + slug, () -> new MoleculeFluid.Flowing(properties, data.solid()));
        RadiationEnum radioactivity = RadiationRules.ofFormula(data.formula());
        BLOCKS.registerBlock("molecule_" + slug + "_block", props -> new MoleculeFluidBlock(still.get(),
            props.replaceable().noCollision().strength(100.0F).noLootTable().liquid(), molecule, null, radioactivity,
            MoleculeFluidBlock.slownessOf(data.form())));
        DeferredItem<BucketItem> bucket = ItemRegistry.ITEMS.registerItem("bucket_" + slug,
            props -> new RadioactiveBucketItem(still.get(), props.craftRemainder(Items.BUCKET).stacksTo(1)));
        Entry entry = new Entry(molecule, data.colour(), bucket, still, flowing, block);
        ENTRIES.add(entry);
        BY_MOLECULE.put(molecule, entry);
    }

    /** The block of a molecule's liquid, or null when the molecule has no fluid (water, or a molecule not in the data). */
    public static Block blockFor(String molecule) {
        Entry entry = BY_MOLECULE.get(molecule);
        return entry == null ? null : entry.block().get();
    }

    /** Every molecule fluid, for the client's fluid models. */
    public static List<Entry> entries() {
        return ENTRIES;
    }

    /**
     * The properties of a fluid. Every fluid, gas included, keeps the water's rates (the engine's defaults: a spread every 5 ticks,
     * one level lost a block). A gas differs by its rising and its type, not by its speed.
     */
    public static BaseFlowingFluid.Properties fluidProperties(java.util.function.Supplier<? extends FluidType> type,
                                                              java.util.function.Supplier<? extends Fluid> still,
                                                              java.util.function.Supplier<? extends Fluid> flowing) {
        return new BaseFlowingFluid.Properties(type, still, flowing);
    }

    /** The slug used in registry paths: lower case, with each run of other characters as one underscore. */
    static String slug(String molecule) {
        return molecule.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Compendium.Naming.id, path);
    }

    private static Map<String, MoleculeData> readMolecules() {
        Map<String, MoleculeData> molecules = new LinkedHashMap<>();
        try (InputStream stream = MoleculeFluids.class.getResourceAsStream(DATA)) {
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> molecule : root.entrySet()) {
                JsonObject fields = molecule.getValue().getAsJsonObject();
                molecules.put(molecule.getKey(), new MoleculeData(fields.get("Colour").getAsInt(), fields.get("Formula").getAsString(),
                    fields.get("Form").getAsString()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read the molecule data for the fluids", e);
        }
        return molecules;
    }
}
