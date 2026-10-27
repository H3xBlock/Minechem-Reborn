package minechem.apparatus.microscope;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import minechem.Compendium;
import minechem.client.DeviceAtlas;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;

/**
 * The microscope in the hand and in the inventory, drawn with the same model
 * and transform as the block entity renderer ({@link MicroscopeRenderer}). The item model names this renderer as
 * {@code minechem:microscope} (assets/minechem/items/microscope.json).
 */
public class MicroscopeItemRenderer implements NoDataSpecialModelRenderer {
    private static final SpriteId TEXTURE = new SpriteId(DeviceAtlas.TEXTURE, Identifier.fromNamespaceAndPath(Compendium.Naming.id, "models/microscope"));

    private final MicroscopeModel model;
    private final SpriteGetter sprites;

    MicroscopeItemRenderer(MicroscopeModel model, SpriteGetter sprites) {
        this.model = model;
        this.sprites = sprites;
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean foil, int color) {
        pose.pushPose();
        pose.translate(0.5, 1.5, 0.5);
        pose.mulPose(Axis.ZP.rotationDegrees(180F));
        // The item's colour argument is 0 (no tint) here, and 0 is transparent black in submitModel; draw opaque white.
        collector.submitModel(model, Unit.INSTANCE, pose, light, overlay, -1, TEXTURE, sprites, 0, null);
        pose.popPose();
    }

    /** The item's extent in item space: the whole block, as the block entity renderer's model fits in it. */
    @Override
    public void getExtents(java.util.function.Consumer<org.joml.Vector3fc> output) {
        output.accept(new org.joml.Vector3f(0F, 0F, 0F));
        output.accept(new org.joml.Vector3f(1F, 1F, 1F));
    }

    /** The model's description in an item model file: a renderer with no data, baked from the entity model set. */
    public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

        @Override
        public NoDataSpecialModelRenderer bake(BakingContext context) {
            return new MicroscopeItemRenderer(new MicroscopeModel(context.entityModelSet().bakeLayer(MicroscopeModel.LAYER)), context.sprites());
        }

        @Override
        public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() {
            return CODEC;
        }
    }
}
