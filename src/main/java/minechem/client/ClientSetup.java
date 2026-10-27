package minechem.client;

import minechem.apparatus.synthesis.SynthesisModel;
import minechem.apparatus.synthesis.SynthesisRenderer;
import minechem.apparatus.synthesis.SynthesisScreen;
import minechem.apparatus.decomposer.DecomposerItemRenderer;
import minechem.apparatus.leadedchest.LeadedChestItemRenderer;
import minechem.apparatus.microscope.MicroscopeItemRenderer;
import minechem.apparatus.synthesis.SynthesisItemRenderer;
import minechem.apparatus.microscope.MicroscopeModel;
import minechem.apparatus.microscope.MicroscopeRenderer;
import minechem.apparatus.microscope.MicroscopeScreen;
import minechem.apparatus.leadedchest.LeadedChestRenderer;
import minechem.apparatus.leadedchest.LeadedChestScreen;
import minechem.apparatus.multiblock.BlueprintProjectorScreen;
import minechem.apparatus.multiblock.FissionReactorScreen;
import minechem.apparatus.multiblock.FusionReactorScreen;
import minechem.item.chemistjournal.ChemistJournalScreen;
import minechem.polytool.PolytoolScreen;
import minechem.Compendium;
import minechem.apparatus.decomposer.DecomposerModel;
import minechem.apparatus.decomposer.DecomposerRenderer;
import minechem.apparatus.decomposer.DecomposerScreen;
import minechem.fluid.ElementFluids;
import minechem.fluid.MoleculeFluids;
import minechem.registry.BlockEntityRegistry;
import minechem.registry.MenuRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterTextureAtlasesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.fluid.FluidTintSources;

/**
 * Client-only registration: menu screens and the chemical item tint source.
 */
@EventBusSubscriber(modid = Compendium.Naming.id, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void registerAtlases(RegisterTextureAtlasesEvent event) {
        event.register(new AtlasManager.AtlasConfig(DeviceAtlas.TEXTURE, DeviceAtlas.DEFINITION, false));
    }

    /**
     * Each molecule liquid is drawn with the vanilla water textures, tinted with the molecule's colour.
     */
    @SubscribeEvent
    public static void registerFluidModels(RegisterFluidModelsEvent event) {
        Material still = new Material(Identifier.fromNamespaceAndPath("minecraft", "block/water_still"));
        Material flowing = new Material(Identifier.fromNamespaceAndPath("minecraft", "block/water_flow"));
        Material overlay = new Material(Identifier.fromNamespaceAndPath("minecraft", "block/water_overlay"));
        for (MoleculeFluids.Entry entry : MoleculeFluids.entries()) {
            FluidModel.Unbaked model = new FluidModel.Unbaked(still, flowing, overlay,
                FluidTintSources.constant(0xFF000000 | entry.colour()));
            event.register(model, entry.still(), entry.flowing());
        }
        for (ElementFluids.Entry entry : ElementFluids.entries()) {
            FluidModel.Unbaked model = new FluidModel.Unbaked(still, flowing, overlay,
                FluidTintSources.constant(0xFF000000 | entry.colour()));
            event.register(model, entry.still(), entry.flowing());
        }
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MenuRegistry.DECOMPOSER.get(), DecomposerScreen::new);
        event.register(MenuRegistry.LEADED_CHEST.get(), LeadedChestScreen::new);
        event.register(MenuRegistry.CHEMIST_JOURNAL.get(), ChemistJournalScreen::new);
        event.register(MenuRegistry.MICROSCOPE.get(), MicroscopeScreen::new);
        event.register(MenuRegistry.SYNTHESIS.get(), SynthesisScreen::new);
        event.register(MenuRegistry.FUSION_REACTOR.get(), FusionReactorScreen::new);
        event.register(MenuRegistry.FISSION_REACTOR.get(), FissionReactorScreen::new);
        event.register(MenuRegistry.BLUEPRINT_PROJECTOR.get(), BlueprintProjectorScreen::new);
        event.register(MenuRegistry.POLYTOOL.get(), PolytoolScreen::new);
    }

    @SubscribeEvent
    public static void registerSpecialModels(RegisterSpecialModelRendererEvent event) {
        event.register(Identifier.fromNamespaceAndPath(Compendium.Naming.id, "microscope"), MicroscopeItemRenderer.Unbaked.CODEC);
        event.register(Identifier.fromNamespaceAndPath(Compendium.Naming.id, "decomposer"), DecomposerItemRenderer.Unbaked.CODEC);
        event.register(Identifier.fromNamespaceAndPath(Compendium.Naming.id, "synthesis"), SynthesisItemRenderer.Unbaked.CODEC);
        event.register(Identifier.fromNamespaceAndPath(Compendium.Naming.id, "leaded_chest"), LeadedChestItemRenderer.Unbaked.CODEC);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DecomposerModel.LAYER, DecomposerModel::createLayer);
        event.registerLayerDefinition(SynthesisModel.LAYER, SynthesisModel::createLayer);
        event.registerLayerDefinition(MicroscopeModel.LAYER, MicroscopeModel::createLayer);
        event.registerLayerDefinition(LeadedChestRenderer.LAYER, ChestModel::createSingleBodyLayer);
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityRegistry.DECOMPOSER.get(), DecomposerRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.SYNTHESIS.get(), SynthesisRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.MICROSCOPE.get(), MicroscopeRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.LEADED_CHEST.get(), LeadedChestRenderer::new);
    }

    @SubscribeEvent
    public static void registerItemTints(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(Identifier.fromNamespaceAndPath(Compendium.Naming.id, "chemical"), ChemicalTint.MAP_CODEC);
        event.register(Identifier.fromNamespaceAndPath(Compendium.Naming.id, "chemical_secondary"), ChemicalSecondaryTint.MAP_CODEC);
    }

    @SubscribeEvent
    public static void registerSelectProperties(RegisterSelectItemModelPropertyEvent event) {
        event.register(Identifier.fromNamespaceAndPath(Compendium.Naming.id, "chemical_form"), ChemicalFormProperty.TYPE);
        event.register(Identifier.fromNamespaceAndPath(Compendium.Naming.id, "chemical_symbol"), ChemicalSymbolProperty.TYPE);
    }
}
