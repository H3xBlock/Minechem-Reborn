package minechem.registry;

import minechem.Compendium;
import minechem.chemical.ChemicalBase;
import minechem.fluid.ChemicalBuckets;
import minechem.helper.Jenkins;
import minechem.item.chemical.ChemicalItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The creative tabs. The chemicals tab holds every element and molecule.
 */
public class CreativeTabRegistry {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Compendium.Naming.id);

    /**
     * The primary tab: the block items of the machines, so that they are in the creative menu.
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PRIMARY = TABS.register("primary", () -> CreativeModeTab.builder()
        .title(Component.literal(Compendium.Naming.name))
        .icon(() -> BlockRegistry.MICROSCOPE_ITEM.get().getDefaultInstance())
        .displayItems((parameters, output) -> {
            output.accept(BlockRegistry.SYNTHESIS_ITEM.get());
            output.accept(BlockRegistry.DECOMPOSER_ITEM.get());
            output.accept(BlockRegistry.MICROSCOPE_ITEM.get());
            output.accept(BlockRegistry.LEADED_CHEST_ITEM.get());
            output.accept(BlockRegistry.URANIUM_ORE_ITEM.get());
            output.accept(BlockRegistry.FUSION_WALL_1_ITEM.get());
            output.accept(BlockRegistry.FUSION_WALL_2_ITEM.get());
            output.accept(BlockRegistry.BLUEPRINT_PROJECTOR_ITEM.get());
            output.accept(ItemRegistry.BLUEPRINT_FUSION.get());
            output.accept(ItemRegistry.BLUEPRINT_FISSION.get());
            output.accept(ItemRegistry.CONCAVE_LENS.get());
            output.accept(ItemRegistry.CONVEX_LENS.get());
            output.accept(ItemRegistry.MICROSCOPE_LENS.get());
            output.accept(ItemRegistry.PROJECTOR_LENS.get());
            output.accept(ItemRegistry.ATOMIC_MANIPULATOR.get());
            output.accept(ItemRegistry.POLYTOOL.get());
            output.accept(ItemRegistry.HAZMAT_HELMET.get());
            output.accept(ItemRegistry.HAZMAT_CHESTPLATE.get());
            output.accept(ItemRegistry.HAZMAT_LEGGINGS.get());
            output.accept(ItemRegistry.HAZMAT_BOOTS.get());
        })
        .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CHEMICALS = TABS.register("chemicals", () -> CreativeModeTab.builder()
        .title(Component.literal(Compendium.Naming.name + " Elements"))
        .icon(() -> ChemicalItem.stackFor(Jenkins.get("H")))
        .displayItems((parameters, output) -> {
            for (ChemicalBase chemical : Jenkins.getAll()) {
                ItemStack stack = ChemicalItem.stackFor(chemical);
                output.accept(stack);
            }
            output.accept(ItemRegistry.CHEMIST_JOURNAL.get());
            output.accept(ItemRegistry.EMPTY_TUBE.get());
        })
        .build());

    /** The buckets tab: the bucket of every chemical that has one. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BUCKETS = TABS.register("buckets", () -> CreativeModeTab.builder()
        .title(Component.literal(Compendium.Naming.name + " Buckets"))
        .icon(() -> ChemicalBuckets.bucketOf(Jenkins.get("U")))
        .displayItems((parameters, output) -> {
            for (ChemicalBase chemical : Jenkins.getAll()) {
                ItemStack stack = ChemicalBuckets.bucketOf(chemical);
                if (!stack.isEmpty()) {
                    output.accept(stack);
                }
            }
        })
        .build());
}
