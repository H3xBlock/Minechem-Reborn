package minechem.registry;

import minechem.apparatus.synthesis.SynthesisBlockEntity;
import minechem.apparatus.synthesis.SynthesisMenu;
import minechem.apparatus.microscope.MicroscopeBlockEntity;
import minechem.apparatus.microscope.MicroscopeMenu;
import minechem.apparatus.leadedchest.LeadedChestBlockEntity;
import minechem.apparatus.leadedchest.LeadedChestMenu;
import minechem.apparatus.multiblock.BlueprintProjectorBlockEntity;
import minechem.apparatus.multiblock.BlueprintProjectorMenu;
import minechem.apparatus.multiblock.FissionReactorBlockEntity;
import minechem.apparatus.multiblock.FissionReactorMenu;
import minechem.apparatus.multiblock.FusionReactorBlockEntity;
import minechem.apparatus.multiblock.FusionReactorMenu;
import minechem.Compendium;
import minechem.item.chemistjournal.ChemistJournalMenu;
import minechem.apparatus.decomposer.DecomposerBlockEntity;
import minechem.apparatus.decomposer.DecomposerMenu;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Menu types for the apparatus GUIs. The block position travels in the open packet; the client finds the block entity there.
 */
public class MenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Compendium.Naming.id);

    public static final DeferredHolder<MenuType<?>, MenuType<DecomposerMenu>> DECOMPOSER = MENUS.register(
        "chemical_decomposer",
        () -> IMenuTypeExtension.create((id, inventory, buffer) -> new DecomposerMenu(
            id, inventory, clientBlockEntity(inventory, buffer.readBlockPos(), DecomposerBlockEntity.class, DecomposerBlockEntity::new, BlockRegistry.DECOMPOSER))));

    public static final DeferredHolder<MenuType<?>, MenuType<minechem.polytool.PolytoolMenu>> POLYTOOL = MENUS.register(
        "polytool",
        () -> IMenuTypeExtension.create((id, inventory, buffer) -> new minechem.polytool.PolytoolMenu(id, inventory)));
    public static final DeferredHolder<MenuType<?>, MenuType<ChemistJournalMenu>> CHEMIST_JOURNAL = MENUS.register(
        "chemist_journal",
        () -> IMenuTypeExtension.create(ChemistJournalMenu::read));

    public static final DeferredHolder<MenuType<?>, MenuType<MicroscopeMenu>> MICROSCOPE = MENUS.register(
        "microscope",
        () -> IMenuTypeExtension.create((id, inventory, buffer) -> new MicroscopeMenu(
            id, inventory, clientBlockEntity(inventory, buffer.readBlockPos(), MicroscopeBlockEntity.class, MicroscopeBlockEntity::new, BlockRegistry.MICROSCOPE))));

    public static final DeferredHolder<MenuType<?>, MenuType<SynthesisMenu>> SYNTHESIS = MENUS.register(
        "synthesis",
        () -> IMenuTypeExtension.create((id, inventory, buffer) -> new SynthesisMenu(
            id, inventory, clientBlockEntity(inventory, buffer.readBlockPos(), SynthesisBlockEntity.class, SynthesisBlockEntity::new, BlockRegistry.SYNTHESIS))));
    public static final DeferredHolder<MenuType<?>, MenuType<LeadedChestMenu>> LEADED_CHEST = MENUS.register(
        "leaded_chest",
        () -> IMenuTypeExtension.create((id, inventory, buffer) -> new LeadedChestMenu(
            id, inventory, clientBlockEntity(inventory, buffer.readBlockPos(), LeadedChestBlockEntity.class, LeadedChestBlockEntity::new, BlockRegistry.LEADED_CHEST))));

    public static final DeferredHolder<MenuType<?>, MenuType<FusionReactorMenu>> FUSION_REACTOR = MENUS.register(
        "fusion_reactor",
        () -> IMenuTypeExtension.create((id, inventory, buffer) -> new FusionReactorMenu(
            id, inventory, clientBlockEntity(inventory, buffer.readBlockPos(), FusionReactorBlockEntity.class, FusionReactorBlockEntity::new, BlockRegistry.FUSION_REACTOR))));
    public static final DeferredHolder<MenuType<?>, MenuType<FissionReactorMenu>> FISSION_REACTOR = MENUS.register(
        "fission_reactor",
        () -> IMenuTypeExtension.create((id, inventory, buffer) -> new FissionReactorMenu(
            id, inventory, clientBlockEntity(inventory, buffer.readBlockPos(), FissionReactorBlockEntity.class, FissionReactorBlockEntity::new, BlockRegistry.FISSION_REACTOR))));
    public static final DeferredHolder<MenuType<?>, MenuType<BlueprintProjectorMenu>> BLUEPRINT_PROJECTOR = MENUS.register(
        "blueprint_projector",
        () -> IMenuTypeExtension.create((id, inventory, buffer) -> new BlueprintProjectorMenu(
            id, inventory, clientBlockEntity(inventory, buffer.readBlockPos(), BlueprintProjectorBlockEntity.class, BlueprintProjectorBlockEntity::new, BlockRegistry.BLUEPRINT_PROJECTOR))));

    /**
     * The block entity named by the open packet, on the client. When the client does not have it (not synced yet, or another
     * block there by then), a blank one of the right type stands in, so the menu still opens instead of failing on a null or a
     * wrong cast; the menu's slots are then filled by the container sync.
     */
    private static <T extends BlockEntity> T clientBlockEntity(Inventory inventory, BlockPos pos, Class<T> type,
                                                               BiFunction<BlockPos, BlockState, T> blank, Supplier<? extends Block> block) {
        BlockEntity found = inventory.player.level().getBlockEntity(pos);
        return type.isInstance(found) ? type.cast(found) : blank.apply(pos, block.get().defaultBlockState());
    }
}
