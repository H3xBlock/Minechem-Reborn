package minechem.fluid;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import minechem.radiation.RadiationEnum;
import minechem.radiation.RadiationRules;
import minechem.registry.ItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The element liquids: each element gets a fluid, a flowing fluid, a block and a bucket. The colour is set by the element's
 * classification.
 *
 * <p>A gaseous element uses the gas type (lighter than air, so it rises, as density -10 does); a solid or liquid one
 * uses the liquid type. The fluids share the registers of {@link MoleculeFluids}.
 */
public final class ElementFluids {
    private static final String DATA = "/assets/minechem/data/elementsData.json";

    public static final DeferredHolder<FluidType, FluidType> ELEMENT_TYPE = MoleculeFluids.FLUID_TYPES.register("element",
        () -> new FluidType(FluidType.Properties.create().descriptionId("fluid.minechem.element")
            .canSwim(true).canDrown(true).isWaterLike(true).motionScale(0.014).canExtinguish(true).canConvertToSource(true).supportsBoating(true)
            .pathType(net.minecraft.world.level.pathfinder.PathType.WATER).adjacentPathType(null)));

    /** The type of a gaseous element: the liquid's, but lighter than air (density -10), so the gas rises. */
    public static final DeferredHolder<FluidType, FluidType> ELEMENT_GAS_TYPE = MoleculeFluids.FLUID_TYPES.register("element_gas",
        () -> new FluidType(FluidType.Properties.create().descriptionId("fluid.minechem.element")
            .canSwim(true).canDrown(true).isWaterLike(true).motionScale(0.014).canExtinguish(true).canConvertToSource(true).supportsBoating(true)
            .pathType(net.minecraft.world.level.pathfinder.PathType.WATER).adjacentPathType(null).density(-10)));

    /** One element's fluid: its colour (0xRRGGBB) and its registered objects. */
    public record Entry(String symbol, int colour, DeferredItem<BucketItem> bucket, DeferredHolder<Fluid, MoleculeFluid.Source> still,
                        DeferredHolder<Fluid, MoleculeFluid.Flowing> flowing, DeferredHolder<Block, MoleculeFluidBlock> block) {
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final Map<String, DeferredHolder<Block, MoleculeFluidBlock>> BLOCK_BY_SYMBOL = new HashMap<>();

    private ElementFluids() {
    }

    /** Adds the element fluids to the registers of {@link MoleculeFluids}; called while the mod is constructed. */
    static void register() {
        for (ElementData element : readElements()) {
            addEntry(element);
        }
    }

    private static void addEntry(ElementData element) {
        String slug = slug(element.longName());
        DeferredHolder<Fluid, MoleculeFluid.Source> still = DeferredHolder.create(Registries.FLUID, id("element_" + slug));
        DeferredHolder<Fluid, MoleculeFluid.Flowing> flowing = DeferredHolder.create(Registries.FLUID, id("flowing_element_" + slug));
        DeferredHolder<Block, MoleculeFluidBlock> block = DeferredHolder.create(Registries.BLOCK, id("element_" + slug + "_block"));
        // The bucket is the fluid's bucket, so picking the fluid up with a bucket gives it (vanilla LiquidBlock.pickupBlock).
        DeferredHolder<Item, BucketItem> bucketHolder = DeferredHolder.create(Registries.ITEM, id("bucket_element_" + slug));
        BaseFlowingFluid.Properties properties = MoleculeFluids.fluidProperties("gas".equals(element.form()) ? ELEMENT_GAS_TYPE : ELEMENT_TYPE,
            still, flowing).block(block).bucket(bucketHolder);
        MoleculeFluids.FLUIDS.register("element_" + slug, () -> new MoleculeFluid.Source(properties, element.solid()));
        MoleculeFluids.FLUIDS.register("flowing_element_" + slug, () -> new MoleculeFluid.Flowing(properties, element.solid()));
        // An element's liquid has no pharmacology (molecule null); its radioactivity comes from its symbol.
        RadiationEnum radioactivity = RadiationRules.ofElement(element.symbol());
        MoleculeFluids.BLOCKS.registerBlock("element_" + slug + "_block", props -> new MoleculeFluidBlock(still.get(),
            props.replaceable().noCollision().strength(100.0F).noLootTable().liquid(), null, element.symbol(), radioactivity,
            MoleculeFluidBlock.slownessOf(element.form())));
        DeferredItem<BucketItem> bucket = ItemRegistry.ITEMS.registerItem("bucket_element_" + slug,
            props -> new RadioactiveBucketItem(still.get(), props.craftRemainder(Items.BUCKET).stacksTo(1)));
        ENTRIES.add(new Entry(element.symbol(), colourOf(element.classification()), bucket, still, flowing, block));
        BLOCK_BY_SYMBOL.put(element.symbol(), block);
    }

    /** The block of an element's liquid by its symbol, or null when there is none. */
    public static Block blockFor(String symbol) {
        DeferredHolder<Block, MoleculeFluidBlock> block = BLOCK_BY_SYMBOL.get(symbol);
        return block == null ? null : block.get();
    }

    /** Every element fluid, in the order of the element data. */
    public static List<Entry> entries() {
        return ENTRIES;
    }

    /**
     * The colour of an element's classification: the red, green and blue parts, each 0, 0.5 or 1,
     * as java.awt.Color rounds them. A classification not listed here is black.
     */
    public static int colourOf(String classification) {
        float red = 0F;
        float green = 0F;
        float blue = 0F;
        switch (classification) {
            case "actinide" -> red = 1;
            case "alkaliMetal" -> green = 1;
            case "alkalineEarth" -> blue = 1;
            case "halogen" -> {
                red = 1;
                green = 1;
            }
            case "nobleGas" -> {
                green = 1;
                blue = 1;
            }
            case "lanthanide" -> {
                red = 1;
                blue = 1;
            }
            case "nonMetal" -> {
                red = 1;
                green = 0.5F;
            }
            case "basicMetal" -> {
                red = 0.5F;
                green = 1;
            }
            case "semiMetal" -> {
                green = 1;
                blue = 0.5F;
            }
            case "transitionMetal" -> {
                green = 0.5F;
                blue = 1;
            }
            default -> {
            }
        }
        return new Color(red, green, blue).getRGB();
    }

    /** The slug used in registry paths: lower case, with each run of other characters as one underscore. */
    static String slug(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(minechem.Compendium.Naming.id, path);
    }

    /** One element of the data: its symbol, its name, its classification (the data's "type") and its form (solid, liquid, gas). */
    record ElementData(String symbol, String longName, String classification, String form) {
        boolean solid() {
            return "solid".equals(form);
        }
    }

    private static List<ElementData> readElements() {
        List<ElementData> elements = new ArrayList<>();
        try (InputStream stream = ElementFluids.class.getResourceAsStream(DATA)) {
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> element : root.entrySet()) {
                JsonObject fields = element.getValue().getAsJsonObject();
                elements.add(new ElementData(fields.get("shortName").getAsString(), fields.get("longName").getAsString(),
                    fields.get("type").getAsString(), fields.get("form").getAsString()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read the element data for the fluids", e);
        }
        return elements;
    }
}
