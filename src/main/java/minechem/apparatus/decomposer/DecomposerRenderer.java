package minechem.apparatus.decomposer;

import java.util.Map;
import java.util.WeakHashMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import minechem.Compendium;
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
 * Renderer of the decomposer. The model is drawn up 1.5 blocks, turned 180 degrees about Z, with no 1/16 scale
 * (26.1 model parts are in pixels; see SynthesisRenderer). The windmill turns only while the decomposer is
 * active, and its angle advances once per render.
 *
 * Only the "on" texture is used. The facing is not used: the model is not turned by metadata.
 */
public class DecomposerRenderer implements BlockEntityRenderer<DecomposerBlockEntity, DecomposerRenderer.RenderState> {
    /** Block entity models sample an atlas sprite; a raw texture renders white in 26.1. */
    private static final SpriteId TEXTURE = new SpriteId(DeviceAtlas.TEXTURE, Identifier.fromNamespaceAndPath(Compendium.Naming.id, "models/decomposer"));

    private final SpriteGetter sprites;
    private final BlockEntityRendererProvider.Context context;
    /**
     * One model per decomposer: a model keeps its windmill angle until it is drawn, so two decomposers cannot share one.
     * Client only, and weak, so a removed decomposer's model is dropped.
     */
    private final Map<DecomposerBlockEntity, DecomposerModel> models = new WeakHashMap<>();

    public DecomposerRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.context = context;
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(DecomposerBlockEntity blockEntity, RenderState state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakProgress);
        if (blockEntity.getMachineState() == DecomposerMachine.State.active) {
            blockEntity.advanceWindmill();
        }
        state.windmillAngle = blockEntity.windmillAngle();
        state.model = models.computeIfAbsent(blockEntity, machine -> new DecomposerModel(context.bakeLayer(DecomposerModel.LAYER)));
    }

    @Override
    public void submit(RenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 1.5, 0.5);
        pose.mulPose(Axis.ZP.rotationDegrees(180F));
        state.model.setWindmillAngle(state.windmillAngle);
        collector.submitModel(state.model, Unit.INSTANCE, pose, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, TEXTURE, sprites, 0, state.breakProgress);
        pose.popPose();
    }

    public static class RenderState extends BlockEntityRenderState {
        public float windmillAngle;
        public DecomposerModel model;
    }
}
