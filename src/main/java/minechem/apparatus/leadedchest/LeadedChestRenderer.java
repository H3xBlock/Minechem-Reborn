package minechem.apparatus.leadedchest;

import com.mojang.blaze3d.vertex.PoseStack;
import minechem.Compendium;
import minechem.apparatus.prefab.block.BasicBlockContainer;
import minechem.client.DeviceAtlas;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * The leaded chest's block entity renderer. It draws a chest model with the lid closed, turned by the facing. The model
 * uses the 26.1 vanilla chest box layout, turned by the vanilla chest transform for the facing.
 */
public class LeadedChestRenderer implements BlockEntityRenderer<LeadedChestBlockEntity, LeadedChestRenderer.RenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(Compendium.Naming.id, "leaded_chest"), "main");
    private static final SpriteId TEXTURE = new SpriteId(DeviceAtlas.TEXTURE, Identifier.fromNamespaceAndPath(Compendium.Naming.id, "models/leaded_chest"));

    private final SpriteGetter sprites;
    private final ChestModel model;

    public LeadedChestRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.model = new ChestModel(context.bakeLayer(LAYER));
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(LeadedChestBlockEntity blockEntity, RenderState state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakProgress);
        state.facing = blockEntity.getBlockState().getValue(BasicBlockContainer.FACING);
    }

    @Override
    public void submit(RenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.mulPose(ChestRenderer.modelTransformation(state.facing));
        collector.submitModel(model, 0F, pose, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, TEXTURE, sprites, 0, state.breakProgress);
        pose.popPose();
    }

    public static class RenderState extends BlockEntityRenderState {
        public Direction facing = Direction.SOUTH;
    }
}
