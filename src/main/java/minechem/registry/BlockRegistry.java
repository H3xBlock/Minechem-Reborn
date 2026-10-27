package minechem.registry;

import minechem.apparatus.synthesis.SynthesisBlock;
import minechem.apparatus.microscope.MicroscopeBlock;
import minechem.apparatus.leadedchest.LeadedChestBlock;
import minechem.apparatus.multiblock.BlueprintProjectorBlock;
import minechem.apparatus.multiblock.FissionReactorBlock;
import minechem.apparatus.multiblock.FusionReactorBlock;
import minechem.apparatus.multiblock.GhostBlock;
import minechem.apparatus.multiblock.ReactorWallBlock;
import minechem.Compendium;
import minechem.apparatus.decomposer.DecomposerBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry paths are snake_case; the camelCase names are kept in Compendium.
 */
public class BlockRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Compendium.Naming.id);

    public static final DeferredBlock<DecomposerBlock> DECOMPOSER = BLOCKS.registerBlock(
        "chemical_decomposer", DecomposerBlock::new, DecomposerBlock::createProperties);
    public static final DeferredItem<BlockItem> DECOMPOSER_ITEM = ItemRegistry.ITEMS.registerSimpleBlockItem(DECOMPOSER);
    public static final DeferredBlock<LeadedChestBlock> LEADED_CHEST = BLOCKS.registerBlock(
        "leaded_chest", LeadedChestBlock::new, LeadedChestBlock::createProperties);
    public static final DeferredItem<BlockItem> LEADED_CHEST_ITEM = ItemRegistry.ITEMS.registerSimpleBlockItem(LEADED_CHEST);
    public static final DeferredBlock<MicroscopeBlock> MICROSCOPE = BLOCKS.registerBlock(
        "microscope", MicroscopeBlock::new, MicroscopeBlock::createProperties);
    public static final DeferredItem<BlockItem> MICROSCOPE_ITEM = ItemRegistry.ITEMS.registerSimpleBlockItem(MICROSCOPE);
    public static final DeferredBlock<SynthesisBlock> SYNTHESIS = BLOCKS.registerBlock(
        "synthesis", SynthesisBlock::new, SynthesisBlock::createProperties);
    public static final DeferredItem<BlockItem> SYNTHESIS_ITEM = ItemRegistry.ITEMS.registerSimpleBlockItem(SYNTHESIS);

    /** The uranium ore: hardness and blast resistance of the diamond ore, and an iron pickaxe needed (see needs_iron_tool). Generated in the overworld by minechem.worldgen.UraniumOreFeature. */
    public static final DeferredBlock<Block> URANIUM_ORE = BLOCKS.registerBlock("uranium_ore", Block::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 3.0F).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> URANIUM_ORE_ITEM = ItemRegistry.ITEMS.registerSimpleBlockItem(URANIUM_ORE);

    /** The walls of the fusion chamber. Both are walls with a proxy block entity. */
    public static final DeferredBlock<ReactorWallBlock> FUSION_WALL_1 = BLOCKS.registerBlock(
        "fusion_wall_1", ReactorWallBlock::new, ReactorWallBlock::createProperties);
    public static final DeferredItem<BlockItem> FUSION_WALL_1_ITEM = ItemRegistry.ITEMS.registerSimpleBlockItem(FUSION_WALL_1);
    public static final DeferredBlock<ReactorWallBlock> FUSION_WALL_2 = BLOCKS.registerBlock(
        "fusion_wall_2", ReactorWallBlock::new, ReactorWallBlock::createProperties);
    public static final DeferredItem<BlockItem> FUSION_WALL_2_ITEM = ItemRegistry.ITEMS.registerSimpleBlockItem(FUSION_WALL_2);
    /** The managers of the reactors have no item: the blueprint projector makes them. */
    public static final DeferredBlock<FusionReactorBlock> FUSION_REACTOR = BLOCKS.registerBlock(
        "fusion_reactor", FusionReactorBlock::new, FusionReactorBlock::createProperties);
    public static final DeferredBlock<FissionReactorBlock> FISSION_REACTOR = BLOCKS.registerBlock(
        "fission_reactor", FissionReactorBlock::new, FissionReactorBlock::createProperties);
    public static final DeferredBlock<GhostBlock> GHOST_BLOCK = BLOCKS.registerBlock(
        "ghost_block", GhostBlock::new, GhostBlock::createProperties);
    public static final DeferredBlock<BlueprintProjectorBlock> BLUEPRINT_PROJECTOR = BLOCKS.registerBlock(
        "blueprint_projector", BlueprintProjectorBlock::new, BlueprintProjectorBlock::createProperties);
    public static final DeferredItem<BlockItem> BLUEPRINT_PROJECTOR_ITEM = ItemRegistry.ITEMS.registerSimpleBlockItem(BLUEPRINT_PROJECTOR);
}
