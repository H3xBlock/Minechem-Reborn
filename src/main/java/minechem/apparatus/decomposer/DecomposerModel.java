package minechem.apparatus.decomposer;

import minechem.Compendium;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;

/**
 * Model of the decomposer: 24 boxes, texture 128 x 64. The six windmill parts are one group that turns
 * about its pivot (0, 12, 0) while the decomposer is active. Boxes are mirrored.
 */
public class DecomposerModel extends Model<Unit> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(Compendium.Naming.id, "decomposer"), "main");

    private final ModelPart windmill;

    public DecomposerModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.windmill = root.getChild("windmill");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        part(root, "backface", 64, 14, 0.0F, 0.0F, 0.0F, 8, 8, 1, -4.0F, 12.0F, 7.0F);
        part(root, "frontface", 64, 23, 0.0F, 0.0F, 0.0F, 8, 8, 1, -4.0F, 12.0F, -8.0F);
        part(root, "leftface", 64, 48, 0.0F, 0.0F, 0.0F, 1, 8, 8, -8.0F, 12.0F, -4.0F);
        part(root, "rightface", 64, 32, 0.0F, 0.0F, 0.0F, 1, 8, 8, 7.0F, 12.0F, -4.0F);
        part(root, "col1", 0, 0, 0.0F, 0.0F, 0.0F, 3, 9, 3, 3.0F, 14.0F, 3.0F);
        part(root, "col2", 0, 0, 0.0F, 0.0F, 0.0F, 3, 9, 3, 3.0F, 14.0F, -6.0F);
        part(root, "col3", 0, 0, 0.0F, 0.0F, 0.0F, 3, 9, 3, -6.0F, 14.0F, 3.0F);
        part(root, "col4", 0, 0, 0.0F, 0.0F, 0.0F, 3, 9, 3, -6.0F, 14.0F, -6.0F);
        part(root, "col5", 0, 0, 0.0F, 0.0F, 0.0F, 3, 9, 3, -6.0F, 14.0F, -1.5F);
        part(root, "col6", 0, 0, 0.0F, 2.0F, 0.0F, 3, 9, 3, -1.5F, 12.0F, -1.5F);
        part(root, "col7", 0, 0, 0.0F, 0.0F, 0.0F, 3, 9, 3, -1.5F, 14.0F, 3.0F);
        part(root, "col8", 0, 0, 0.0F, 1.0F, 0.0F, 3, 9, 3, -1.5F, 13.0F, -6.0F);
        part(root, "col9", 0, 0, 0.0F, 1.0F, 0.0F, 3, 9, 3, 3.0F, 13.0F, -1.5F);
        part(root, "base", 0, 47, 0.0F, 0.0F, 0.0F, 16, 1, 16, -8.0F, 23.0F, -8.0F);
        part(root, "middlebase", 0, 31, 0.0F, 0.0F, 0.0F, 14, 2, 14, -7.0F, 15.0F, -7.0F);
        part(root, "topface", 24, 22, 0.0F, 0.0F, 0.0F, 8, 1, 8, -4.0F, 8.0F, -4.0F);
        part(root, "Shape7", 0, 22, 0.0F, 0.0F, 0.0F, 6, 2, 6, -3.0F, 9.0F, -3.0F);
        part(root, "Shape8", 0, 12, 0.0F, 0.0F, 0.0F, 2, 3, 2, -1.0F, 11.0F, -1.0F);
        PartDefinition windmill = root.addOrReplaceChild("windmill", CubeListBuilder.create(), PartPose.offset(0F, 12F, 0F));
        part(windmill, "windmillspoke1", 12, 0, -0.5F, 0.0F, -3.0F, 1, 1, 6, 0F, 0F, 0F);
        part(windmill, "windmillspoke2", 12, 7, -3.0F, 0.0F, -0.5F, 6, 1, 1, 0F, 0F, 0F);
        part(windmill, "windmillbox1", 12, 9, -2.0F, -0.5F, 3.0F, 4, 2, 2, 0F, 0F, 0F);
        part(windmill, "windmillbox2", 12, 9, -2.0F, -0.5F, -5.0F, 4, 2, 2, 0F, 0F, 0F);
        part(windmill, "windmillbox3", 12, 13, 3.0F, -0.5F, -2.0F, 2, 2, 4, 0F, 0F, 0F);
        part(windmill, "windmillbox4", 12, 13, -5.0F, -0.5F, -2.0F, 2, 2, 4, 0F, 0F, 0F);
        return LayerDefinition.create(mesh, 128, 64);
    }

    /**
     * Does not reset the pose. In 26.1 {@code ModelFeatureRenderer} calls {@code setupAnim} before each draw, and the default
     * {@code setupAnim} calls {@code resetPose}, which put the windmill back at rest and hid the turn. The angle is set in
     * {@link #setWindmillAngle}, so the pose must survive until the draw.
     */
    @Override
    public void setupAnim(Unit unit) {
    }

    /**
     * Turns the windmill. The angle is in radians, and the wrap at 360 happens after 360 radians (about 57 turns).
     */
    public void setWindmillAngle(float angle) {
        this.windmill.yRot = angle;
    }

    private static void part(PartDefinition group, String name, int u, int v, float x, float y, float z, int w, int h, int d,
                             float px, float py, float pz) {
        group.addOrReplaceChild(name,
            CubeListBuilder.create().texOffs(u, v).mirror().addBox(x, y, z, w, h, d),
            PartPose.offset(px, py, pz));
    }
}
