package minechem.apparatus.synthesis;

import java.util.Map;
import java.util.WeakHashMap;
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
 * The synthesis machine's renderer: the model is up 1.5 blocks and turned 180 degrees about Z, then turned by the machine's
 * facing. No 1/16 scale: model parts are in pixels in 26.1, and a 1/16 scale drew the machine at 1/256 of its size. The arm
 * moves while the machine is active (a recipe whose ingredients are in the storage), and a full cycle plays for each craft.
 *
 * <p>The arm's timer is one per machine.
 */
public class SynthesisRenderer implements BlockEntityRenderer<SynthesisBlockEntity, SynthesisRenderer.RenderState> {
    /** Block entity models sample an atlas sprite; a raw texture renders white in 26.1. */
    private static final SpriteId TEXTURE = new SpriteId(DeviceAtlas.TEXTURE, Identifier.fromNamespaceAndPath(Compendium.Naming.id, "models/synthesis"));

    private final SpriteGetter sprites;
    private final BlockEntityRendererProvider.Context context;
    /**
     * One arm model per machine: a model keeps its arm pose and timer until it is drawn, so two machines cannot share one.
     * Client only, and weak, so a removed machine's model is dropped.
     */
    private final Map<SynthesisBlockEntity, SynthesisModel> models = new WeakHashMap<>();

    public SynthesisRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.context = context;
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(SynthesisBlockEntity blockEntity, RenderState state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakProgress);
        // The arm moves while the machine is active, and a full cycle plays for each craft, whatever the craft's time.
        SynthesisModel model = models.computeIfAbsent(blockEntity,
            machine -> new SynthesisModel(context.bakeLayer(SynthesisModel.LAYER)));
        boolean craftArm = blockEntity.advanceCraftArm();
        if (craftArm || blockEntity.hasRecipeRunning()) {
            model.updateArm();
        }
        state.model = model;
        state.yRotation = BasicBlockContainer.modelYRotation(blockEntity.getBlockState().getValue(BasicBlockContainer.FACING));
    }

    @Override
    public void submit(RenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 1.5, 0.5);
        pose.mulPose(Axis.ZP.rotationDegrees(180F));
        pose.mulPose(Axis.YP.rotationDegrees(state.yRotation));
        collector.submitModel(state.model, Unit.INSTANCE, pose, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, TEXTURE, sprites, 0, state.breakProgress);
        pose.popPose();
    }

    public static class RenderState extends BlockEntityRenderState {
        public float yRotation;
        public SynthesisModel model;
    }
}
