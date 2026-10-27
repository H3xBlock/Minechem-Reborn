package minechem.apparatus.microscope;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import minechem.Compendium;
import minechem.apparatus.prefab.block.BasicBlockContainer;
import minechem.client.DeviceAtlas;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.phys.Vec3;

/**
 * Block entity renderer of the microscope: the model up 1.5 blocks, turned 180 degrees about Z, scale 1/16,
 * turned by the machine's facing.
 *
 * <p>The model is drawn without blending, since the sprite pipeline does not need it. The facing
 * angles come from {@link BasicBlockContainer#modelYRotation}.
 */
public class MicroscopeRenderer implements BlockEntityRenderer<MicroscopeBlockEntity, MicroscopeRenderer.RenderState> {
    /** Block entity models sample an atlas sprite; a raw texture renders white in 26.1. */
    private static final SpriteId TEXTURE = new SpriteId(DeviceAtlas.TEXTURE, Identifier.fromNamespaceAndPath(Compendium.Naming.id, "models/microscope"));

    private final SpriteGetter sprites;
    private final MicroscopeModel model;

    public MicroscopeRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.model = new MicroscopeModel(context.bakeLayer(MicroscopeModel.LAYER));
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(MicroscopeBlockEntity blockEntity, RenderState state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakProgress);
        state.yRotation = BasicBlockContainer.modelYRotation(blockEntity.getBlockState().getValue(BasicBlockContainer.FACING));
    }

    @Override
    public void submit(RenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 1.5, 0.5);
        pose.mulPose(Axis.ZP.rotationDegrees(180F));
        pose.mulPose(Axis.YP.rotationDegrees(state.yRotation));
        collector.submitModel(model, Unit.INSTANCE, pose, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, TEXTURE, sprites, 0, state.breakProgress);
        pose.popPose();
    }

    public static class RenderState extends BlockEntityRenderState {
        public float yRotation;
    }
}
