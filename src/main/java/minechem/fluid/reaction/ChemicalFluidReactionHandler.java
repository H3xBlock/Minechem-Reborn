package minechem.fluid.reaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import minechem.Compendium;
import minechem.Config;
import minechem.chemical.ChemicalBase;
import minechem.chemical.Element;
import minechem.fluid.ChemicalBuckets;
import minechem.fluid.ElementFluids;
import minechem.fluid.MoleculeFluidBlock;
import minechem.fluid.MoleculeFluids;
import minechem.helper.ItemHelper;
import minechem.helper.Jenkins;
import minechem.item.chemical.ChemicalItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The reactions of the chemicals: a bucket that falls into a liquid, two liquids that touch, and the explosive liquids that
 * catch fire.
 *
 * <p>The rules, with their products, order and powers, are listed in initReaction. A product of water is lost (there is no
 * fluid for water, so the cell is cleared and nothing is placed), and a reaction in a flowing liquid places its products
 * flowing. The rules are built on the first reaction, when the chemicals are loaded.
 */
@EventBusSubscriber(modid = Compendium.Naming.id)
public final class ChemicalFluidReactionHandler {
    /** Layers a product can fill: the one below the reaction, the reaction's own and the one above. */
    static final int FLUIDS_GENERATE_SPACE = 3;
    /** The amount of a product dropped as items when no cell is free. */
    private static final int DROPPED_AMOUNT = 8;
    private static final String WATER = "Water";
    /** The explosive liquids and the power of their explosion: TNT, 2. */
    private static final Map<String, Float> EXPLOSIVE_FLUIDS = Map.of("Trinitrotoluene", 2F);

    private static Map<ChemicalFluidReactionRule, ChemicalFluidReactionOutput> rules;

    private ChemicalFluidReactionHandler() {
    }

    /** Each bucket item in a liquid is checked, once per server tick. */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !Config.reactionItemMeetFluid()) {
            return;
        }
        List<ItemEntity> items = new ArrayList<>();
        for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
            if (entity instanceof ItemEntity item) {
                items.add(item);
            }
        }
        for (ItemEntity item : items) {
            if (item.isAlive()) {
                checkEntityItem(level, item);
            }
        }
    }

    /**
     * A bucket of a chemical in a water-like liquid reacts with the liquid at its feet. The bucket is used up
     * and an empty bucket is dropped where the reaction was. The vanilla water bucket is not one of the mod's buckets, so it never reacts.
     */
    public static void checkEntityItem(ServerLevel level, ItemEntity entity) {
        ItemStack stack = entity.getItem();
        if (stack.isEmpty() || stack.is(Items.WATER_BUCKET)) {
            return;
        }
        ChemicalBase chemicalA = ChemicalBuckets.chemicalOfBucket(stack);
        if (chemicalA == null || !inWaterLike(level, entity.getBoundingBox())) {
            return;
        }
        BlockPos pos = entity.blockPosition();
        BlockState state = level.getBlockState(pos);
        ChemicalBase chemicalB = chemicalOfBlock(state);
        if (chemicalB == null) {
            return;
        }
        ChemicalFluidReactionRule rule = new ChemicalFluidReactionRule(chemicalA, chemicalB);
        if (!rules().containsKey(rule)) {
            return;
        }
        react(level, pos, rule, !canDrain(state));
        stack.shrink(1);
        if (stack.isEmpty()) {
            entity.discard();
        } else {
            entity.setItem(stack);
        }
        ItemHelper.throwItemStack(level, new ItemStack(Items.BUCKET), pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Run when a neighbour of the liquid at pos changes: the liquid reacts with the first
     * neighbour it has a rule for, and only when nothing reacted does an explosive liquid next to fire explode.
     */
    public static void checkStatus(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof MoleculeFluidBlock)) {
            return;
        }
        if (Config.reactionFluidMeetFluid()) {
            for (Direction face : Direction.values()) {
                if (checkToReact(level, pos.relative(face), pos)) {
                    return;
                }
            }
        }
        checkToExplode(level, pos, state);
    }

    /**
     * The liquid at source and the block at destination react if there is a rule for the pair. Both blocks are cleared and
     * the products are made at the destination.
     */
    static boolean checkToReact(ServerLevel level, BlockPos destination, BlockPos source) {
        BlockState sourceState = level.getBlockState(source);
        BlockState destinationState = level.getBlockState(destination);
        ChemicalBase chemicalA = chemicalOfBlock(sourceState);
        ChemicalBase chemicalB = chemicalOfBlock(destinationState);
        if (chemicalA == null || chemicalB == null) {
            return false;
        }
        ChemicalFluidReactionRule rule = new ChemicalFluidReactionRule(chemicalA, chemicalB);
        if (!rules().containsKey(rule)) {
            return false;
        }
        boolean popFlowing = !(canDrain(sourceState) && canDrain(destinationState));
        level.setBlock(source, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(destination, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        react(level, destination, rule, popFlowing);
        return true;
    }

    /** An explosive liquid with fire or lava beside it is removed and explodes. */
    private static void checkToExplode(ServerLevel level, BlockPos pos, BlockState state) {
        ChemicalBase chemical = chemicalOfBlock(state);
        Float power = chemical == null ? null : EXPLOSIVE_FLUIDS.get(chemical.fullName);
        if (power == null) {
            return;
        }
        boolean fireBeside = false;
        for (Direction face : Direction.values()) {
            BlockState neighbour = level.getBlockState(pos.relative(face));
            if (neighbour.is(Blocks.FIRE) || neighbour.is(Blocks.LAVA)) {
                fireBeside = true;
                break;
            }
        }
        if (!fireBeside) {
            return;
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.explode(null, pos.getX(), pos.getY(), pos.getZ(), power, false, Level.ExplosionInteraction.TNT);
    }

    /**
     * The reaction explodes if it has a power, then each product takes a free cell of the three layers
     * around pos. A gas searches from the top layer, the others from the bottom. A product with no free cell is dropped as items, unless
     * the reaction was in a flowing liquid, where it is lost. A product that is solid is not placed in a flowing reaction.
     */
    private static void react(ServerLevel level, BlockPos pos, ChemicalFluidReactionRule rule, boolean popFlowing) {
        ChemicalFluidReactionOutput output = rules().get(rule);
        if (output == null) {
            return;
        }
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (!Float.isNaN(output.explosionLevel())) {
            level.explode(null, x, y, z, output.explosionLevel(), false, Level.ExplosionInteraction.TNT);
        }

        int halfSpace = FLUIDS_GENERATE_SPACE / 2;
        List<List<BlockPos>> availableSpaces = new ArrayList<>();
        for (int i = 0; i < FLUIDS_GENERATE_SPACE; i++) {
            availableSpaces.add(findAvailableSpacesAtCrossSection(level, x, y - halfSpace + i, z, 1));
        }

        for (ChemicalBase chemical : output.outputs()) {
            boolean hasFlowingStatus = chemical.form != ChemicalBase.Form.solid;
            BlockPos target = null;
            if (hasFlowingStatus || !popFlowing) {
                target = takeSpace(availableSpaces, chemical.form == ChemicalBase.Form.gas);
            }
            if (target == null) {
                if (!popFlowing) {
                    ItemStack stack = ChemicalItem.stackFor(chemical);
                    stack.setCount(DROPPED_AMOUNT);
                    ItemHelper.throwItemStack(level, stack, x, y, z);
                }
                continue;
            }
            level.destroyBlock(target, true);
            level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            Block fluidBlock = fluidBlockOf(chemical);
            if (fluidBlock != null) {
                // Level 1 for a flowing product, 0 for a source.
                level.setBlock(target, fluidBlock.defaultBlockState().setValue(LiquidBlock.LEVEL, popFlowing ? 1 : 0), Block.UPDATE_ALL);
            }
        }
    }

    /** Takes one free cell out of the layers: the top layer first for a gas, the bottom layer first otherwise. */
    private static BlockPos takeSpace(List<List<BlockPos>> layers, boolean gas) {
        for (int i = 0; i < layers.size(); i++) {
            List<BlockPos> layer = layers.get(gas ? layers.size() - 1 - i : i);
            if (!layer.isEmpty()) {
                return layer.remove(layer.size() - 1);
            }
        }
        return null;
    }

    /** The cells of a square layer (size 1, so 3 by 3) that are air or not solid. */
    static List<BlockPos> findAvailableSpacesAtCrossSection(ServerLevel level, int centerX, int centerY, int centerZ, int size) {
        List<BlockPos> spaces = new ArrayList<>();
        for (int xOffset = -size; xOffset <= size; xOffset++) {
            for (int zOffset = -size; zOffset <= size; zOffset++) {
                BlockPos pos = new BlockPos(centerX + xOffset, centerY, centerZ + zOffset);
                if (!level.getBlockState(pos).blocksMotion()) {
                    spaces.add(pos);
                }
            }
        }
        return spaces;
    }

    /** The material check for a bucket: any cell the box touches holds water or a water-like liquid (not lava). */
    private static boolean inWaterLike(ServerLevel level, AABB box) {
        return BlockPos.betweenClosedStream(box).anyMatch(pos -> level.getFluidState(pos).is(FluidTags.WATER));
    }

    /** The chemical of a block: the mod's liquid, or vanilla water as the water molecule. Null for other blocks. */
    static ChemicalBase chemicalOfBlock(BlockState state) {
        if (state.getBlock() instanceof MoleculeFluidBlock liquid) {
            return liquid.chemical();
        }
        if (state.is(Blocks.WATER)) {
            return Jenkins.get(WATER);
        }
        return null;
    }

    /** A whole source block (not a flowing one) can be drained, so a reaction in it makes no flowing products. */
    private static boolean canDrain(BlockState state) {
        return state.getFluidState().isSource();
    }

    /** The liquid block of a chemical: an element's or a molecule's. Null for water, which has no fluid. */
    private static Block fluidBlockOf(ChemicalBase chemical) {
        if (chemical instanceof Element element) {
            return ElementFluids.blockFor(element.shortName);
        }
        return MoleculeFluids.blockFor(chemical.fullName);
    }

    /** The reaction rules, built once by initReaction when the first reaction is checked. */
    static Map<ChemicalFluidReactionRule, ChemicalFluidReactionOutput> rules() {
        if (rules == null) {
            // Built aside and kept only once complete: a call before the chemicals are loaded throws (IllegalStateException)
            // and leaves the table to be built on a later call, instead of keeping an empty table for good.
            Map<ChemicalFluidReactionRule, ChemicalFluidReactionOutput> built = new HashMap<>();
            initReaction(built);
            rules = built;
        }
        return rules;
    }

    private static void initReaction(Map<ChemicalFluidReactionRule, ChemicalFluidReactionOutput> map) {
        ChemicalBase water = molecule(WATER);
        ChemicalBase sulfuricAcid = molecule("Sulfuric Acid");
        ChemicalBase hcl = molecule("Hydrogen Chloride");
        ChemicalBase salt = molecule("Salt");
        ChemicalBase sodiumHydroxide = molecule("Sodium Hydroxide");
        ChemicalBase sodiumBisulfate = molecule("Sodium Bisulfate");
        ChemicalBase hydrogenSulfide = molecule("Hydrogen Sulfide");
        ChemicalBase hydrogen = element(1);

        // H2O+Li==LiOH+H
        add(map, water, element(3), 0.1F, hydrogen, molecule("Lithium Hydroxide"));
        // H2O+Na==NaOH+H
        add(map, water, element(11), 0.15F, hydrogen, molecule("Sodium Hydroxide"));
        // H2O+K==KOH+H
        add(map, water, element(19), 0.2F, hydrogen, molecule("Potassium Hydroxide"));
        // H2O+Rb==RbOH+H
        add(map, water, element(37), 0.25F, hydrogen, molecule("Rubidium Hydroxide"));
        // H2O+Cs==CsOH+H
        add(map, water, element(55), 0.3F, hydrogen, molecule("Cesium Hydroxide"));
        // H2O+Fr==FrOH+H
        add(map, water, element(87), 0.4F, hydrogen, molecule("Francium Hydroxide"));
        // H2SO4+Cu==CuSO4+2H
        add(map, sulfuricAcid, element(29), 0.1F, hydrogen, hydrogen, molecule("Copper(II) Sulfate"));
        // H2SO4+S==2SO2+2H
        add(map, sulfuricAcid, element(16), 0.1F, hydrogen, hydrogen, molecule("Sulfur Dioxide"));
        // H2SO4+H2S==S+SO2+2H2O
        add(map, sulfuricAcid, hydrogenSulfide, 0.1F, element(16), molecule("Sulfur Dioxide"), water, water);
        // HCl+NaOH==H2O+NaCl
        add(map, hcl, sodiumHydroxide, 0.1F, salt, water);
        // H+Cl==HCl
        add(map, hydrogen, element(17), 0.1F, hcl);
        // NaCl+H2SO4==NaHSO4+HCl
        add(map, salt, sulfuricAcid, 0.1F, sodiumBisulfate, hcl);
        // NaHSO4+NaCl==Na2SO4+2HCl
        add(map, salt, sodiumBisulfate, 0.1F, molecule("Sodium Sulfate"), hcl, hcl);
    }

    /**
     * Adds a reaction of another mod. A rule already there for the same pair is replaced.
     */
    public static void addRule(ChemicalBase a, ChemicalBase b, float explosionLevel, List<ChemicalBase> outputs) {
        rules().put(new ChemicalFluidReactionRule(a, b), new ChemicalFluidReactionOutput(List.copyOf(outputs), explosionLevel));
    }

    private static void add(Map<ChemicalFluidReactionRule, ChemicalFluidReactionOutput> map, ChemicalBase a, ChemicalBase b, float explosionLevel,
                            ChemicalBase... outputs) {
        map.put(new ChemicalFluidReactionRule(a, b), new ChemicalFluidReactionOutput(List.of(outputs), explosionLevel));
    }

    private static ChemicalBase element(int atomicNumber) {
        ChemicalBase element = Jenkins.get(atomicNumber);
        if (element == null) {
            throw new IllegalStateException("no element " + atomicNumber + " (are the chemicals loaded?)");
        }
        return element;
    }

    private static ChemicalBase molecule(String fullName) {
        ChemicalBase molecule = Jenkins.<ChemicalBase>get(fullName);
        if (molecule == null) {
            throw new IllegalStateException("no molecule " + fullName + " (are the chemicals loaded?)");
        }
        return molecule;
    }
}
