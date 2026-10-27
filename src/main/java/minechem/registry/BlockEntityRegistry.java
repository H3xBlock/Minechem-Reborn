package minechem.registry;

import minechem.apparatus.synthesis.SynthesisBlockEntity;
import minechem.apparatus.microscope.MicroscopeBlockEntity;
import minechem.apparatus.leadedchest.LeadedChestBlockEntity;
import minechem.apparatus.multiblock.BlueprintProjectorBlockEntity;
import minechem.apparatus.multiblock.FissionReactorBlockEntity;
import minechem.apparatus.multiblock.FusionReactorBlockEntity;
import minechem.apparatus.multiblock.GhostBlockEntity;
import minechem.apparatus.multiblock.ReactorWallBlockEntity;
import minechem.Compendium;
import minechem.apparatus.decomposer.DecomposerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The block entity types. Each block entity type is paired with its block.
 */
public class BlockEntityRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Compendium.Naming.id);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DecomposerBlockEntity>> DECOMPOSER = BLOCK_ENTITY_TYPES.register(
        "chemical_decomposer",
        () -> new BlockEntityType<>(DecomposerBlockEntity::new, java.util.Set.of(BlockRegistry.DECOMPOSER.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LeadedChestBlockEntity>> LEADED_CHEST = BLOCK_ENTITY_TYPES.register(
        "leaded_chest",
        () -> new BlockEntityType<>(LeadedChestBlockEntity::new, java.util.Set.of(BlockRegistry.LEADED_CHEST.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MicroscopeBlockEntity>> MICROSCOPE = BLOCK_ENTITY_TYPES.register(
        "microscope",
        () -> new BlockEntityType<>(MicroscopeBlockEntity::new, java.util.Set.of(BlockRegistry.MICROSCOPE.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SynthesisBlockEntity>> SYNTHESIS = BLOCK_ENTITY_TYPES.register(
        "synthesis",
        () -> new BlockEntityType<>(SynthesisBlockEntity::new, java.util.Set.of(BlockRegistry.SYNTHESIS.get())));

    /** The reactor walls: both fusion walls are proxies. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorWallBlockEntity>> REACTOR_WALL = BLOCK_ENTITY_TYPES.register(
        "reactor_wall",
        () -> new BlockEntityType<>(ReactorWallBlockEntity::new, java.util.Set.of(BlockRegistry.FUSION_WALL_1.get(), BlockRegistry.FUSION_WALL_2.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionReactorBlockEntity>> FUSION_REACTOR = BLOCK_ENTITY_TYPES.register(
        "fusion_reactor",
        () -> new BlockEntityType<>(FusionReactorBlockEntity::new, java.util.Set.of(BlockRegistry.FUSION_REACTOR.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FissionReactorBlockEntity>> FISSION_REACTOR = BLOCK_ENTITY_TYPES.register(
        "fission_reactor",
        () -> new BlockEntityType<>(FissionReactorBlockEntity::new, java.util.Set.of(BlockRegistry.FISSION_REACTOR.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GhostBlockEntity>> GHOST_BLOCK = BLOCK_ENTITY_TYPES.register(
        "ghost_block",
        () -> new BlockEntityType<>(GhostBlockEntity::new, java.util.Set.of(BlockRegistry.GHOST_BLOCK.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlueprintProjectorBlockEntity>> BLUEPRINT_PROJECTOR = BLOCK_ENTITY_TYPES.register(
        "blueprint_projector",
        () -> new BlockEntityType<>(BlueprintProjectorBlockEntity::new, java.util.Set.of(BlockRegistry.BLUEPRINT_PROJECTOR.get())));
}
