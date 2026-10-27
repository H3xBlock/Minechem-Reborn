package minechem.apparatus.leadedchest;

import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.serialization.MapCodec;
import minechem.Compendium;
import minechem.client.DeviceAtlas;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;


/**
 * The leaded chest in the hand, in the inventory and on the hotbar: drawn with the same model and transform as the block entity
 * renderer ({@link LeadedChestRenderer}), so the item is the machine and not a flat cube. The item model names this renderer as
 * {@code minechem:leaded_chest} (assets/minechem/items/leaded_chest.json).
 */
public class LeadedChestItemRenderer implements NoDataSpecialModelRenderer {
    private static final SpriteId TEXTURE = new SpriteId(DeviceAtlas.TEXTURE, Identifier.fromNamespaceAndPath(Compendium.Naming.id, "models/leaded_chest"));

    private final ChestModel model;
    private final SpriteGetter sprites;

    LeadedChestItemRenderer(ChestModel model, SpriteGetter sprites) {
        this.model = model;
        this.sprites = sprites;
    }

    /**
     * Like the vanilla chest item (ChestSpecialRenderer): no transform here, the item's display transform places the model. The
     * block entity's translation and facing are for the world, and moved the model out of frame in the hotbar.
     */
    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean foil, int color) {
        // The item's colour argument is 0 here, and 0 is transparent black in submitModel; draw opaque white.
        collector.submitModel(model, 0F, pose, light, overlay, -1, TEXTURE, sprites, 0, null);
    }

    /** The model's own extents in item space, as the vanilla chest item (ChestSpecialRenderer.getExtents). */
    @Override
    public void getExtents(java.util.function.Consumer<org.joml.Vector3fc> output) {
        model.setupAnim(0F);
        model.root().getExtentsForGui(new PoseStack(), output);
    }

    /** The model's description in an item model file: a renderer with no data, baked from the entity model set. */
    public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

        @Override
        public NoDataSpecialModelRenderer bake(BakingContext context) {
            return new LeadedChestItemRenderer(new ChestModel(context.entityModelSet().bakeLayer(LeadedChestRenderer.LAYER)), context.sprites());
        }

        @Override
        public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() {
            return CODEC;
        }
    }
}
