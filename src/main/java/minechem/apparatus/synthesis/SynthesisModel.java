package minechem.apparatus.synthesis;

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
 * The synthesis machine's model: 16 boxes, texture 128 x 64. The two arm parts (toparm and bottomarm) move in a 200-step
 * cycle, and each step sets the arms' pivot. The timer adds 0.5 a render and wraps after 200.
 *
 * <p>The part pivots are the rotation points here. The arms' rotation points are set each render, so their base offsets
 * are the pivots.
 */
public class SynthesisModel extends Model<Unit> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(Compendium.Naming.id, "synthesis"), "main");

    private final ModelPart toparm;
    private final ModelPart bottomarm;
    private float animationTimer;

    public SynthesisModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.toparm = root.getChild("toparm");
        this.bottomarm = root.getChild("bottomarm");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        part(root, "base", 0, 46, 0, 0, 0, 16, 2, 16, -8F, 22F, -8F);
        part(root, "back", 0, 32, 0, -1, 0, 14, 9, 5, -7F, 14F, 2F);
        part(root, "topframeleft", 38, 21, 0, 0, 0, 1, 1, 9, -7F, 13F, -7F);
        part(root, "topframefront", 38, 38, 0, 0, 0, 12, 1, 1, -6F, 13F, -7F);
        part(root, "topframeright", 38, 21, 0, 0, 0, 1, 1, 9, 6F, 13F, -7F);
        part(root, "dish1", 0, 0, 0, 0, 0, 3, 1, 3, -6F, 21F, -6F);
        part(root, "dish2", 0, 4, 0, 0, 0, 3, 1, 3, -1.5F, 21F, -6F);
        part(root, "dish3", 0, 8, 0, 0, 0, 3, 1, 3, 3F, 21F, -6F);
        part(root, "toparm", 0, 17, 0, 0, 0, 3, 2, 13, -6F, 11F, -8F);
        part(root, "bottomarm", 32, 23, 1, 0, 0, 1, 8, 1, -6F, 13F, -5F);
        part(root, "leftpanel", 64, 23, 0, 0, 0, 1, 8, 8, -8F, 12F, -4F);
        part(root, "rightpanel", 64, 39, 0, 0, 0, 1, 8, 8, 7F, 12F, -4F);
        part(root, "backpanel", 64, 14, 0, 0, 0, 8, 8, 1, -4F, 12F, 7F);
        part(root, "backbox", 38, 40, 0, 0, 0, 10, 3, 3, -5F, 19F, -1F);
        part(root, "toppanel", 64, 55, 0, 0, 0, 8, 1, 8, -4F, 8F, -4F);
        root.addOrReplaceChild("paneljoint", CubeListBuilder.create().texOffs(38, 31).mirror().addBox(0, 0, 0, 4, 1, 6),
            PartPose.offsetAndRotation(-2F, 8F, 4F, -0.8551081F, 0F, 0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /**
     * Does not reset the pose: in 26.1 {@code ModelFeatureRenderer} calls {@code setupAnim} before each draw, and the default
     * one calls {@code resetPose}, which would put the arms back at rest on every frame.
     */
    @Override
    public void setupAnim(Unit unit) {
    }

    /** One step of the arm's cycle. */
    public void updateArm() {
        animationTimer += 0.5F;
        armVertical(0, 10, 13, 11.2F);
        armHorizontal(10, 40, -6, -1.5F);
        armVertical(40, 50, 11.2F, 13);
        armVertical(50, 60, 13, 11.2F);
        armHorizontal(60, 90, -1.5F, 3F);
        armVertical(90, 100, 11.2F, 13);
        armVertical(100, 110, 13, 11.2F);
        armHorizontal(110, 140, 3F, -1.5F);
        armVertical(140, 150, 11.2F, 13);
        armVertical(150, 160, 13, 11.2F);
        armHorizontal(160, 190, -1.5F, -6F);
        armVertical(190, 200, 11.2F, 13);
        if (animationTimer > 200) {
            animationTimer = 0;
        }
    }

    private void armHorizontal(float start, float end, float xStart, float xEnd) {
        if (animationTimer > start && animationTimer <= end) {
            float x = translate(animationTimer - start, 0.0F, end - start, xStart, xEnd);
            toparm.x = x;
            bottomarm.x = x;
        }
    }

    private void armVertical(float start, float end, float yStart, float yEnd) {
        if (animationTimer > start && animationTimer <= end) {
            bottomarm.y = translate(animationTimer - start, 0.0F, end - start, yStart, yEnd);
        }
    }

    /** Maps a value from one range to another. */
    static float translate(float value, float leftMin, float leftRange, float rightMin, float rightMax) {
        return rightMin + ((value - leftMin) / leftRange) * (rightMax - rightMin);
    }

    private static void part(PartDefinition group, String name, int u, int v, float x, float y, float z, int w, int h, int d,
                             float px, float py, float pz) {
        group.addOrReplaceChild(name,
            CubeListBuilder.create().texOffs(u, v).mirror().addBox(x, y, z, w, h, d),
            PartPose.offset(px, py, pz));
    }
}
