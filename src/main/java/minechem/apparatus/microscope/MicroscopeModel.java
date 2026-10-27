package minechem.apparatus.microscope;

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
 * Model of the microscope: 13 boxes, texture 64 x 32. The third lens takes its rotation from the render method,
 * which uses the opposite sign of Z from the construction.
 */
public class MicroscopeModel extends Model<Unit> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(Compendium.Naming.id, "microscope"), "main");

    public MicroscopeModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        part(root, "base", 8, 16, 0F, 0F, 0F, 14, 2, 14, -7F, 22F, -7F, 0F, 0F, 0F, true);
        part(root, "neck_lower", 16, 6, 0F, 0F, 0F, 4, 6, 4, -2F, 16F, -7F, 0F, 0F, 0F, true);
        part(root, "platter", 32, 7, 0F, 0F, 0F, 8, 1, 8, -4F, 18F, -4F, 0F, 0F, 0F, true);
        part(root, "light", 0, 28, 0F, 0F, 0F, 2, 2, 2, -1F, 20F, -1F, 0F, 0F, 0F, true);
        part(root, "neck_upper", 8, 8, 0F, 0F, 0F, 2, 6, 2, -1F, 11F, -3F, -0.506146F, 0F, 0F, true);
        part(root, "top", 46, 0, 0F, 0F, 0F, 4, 2, 5, -2F, 11F, -3F, 0F, 0F, 0F, true);
        part(root, "eyepiece", 0, 4, 0F, 0F, 0F, 1, 4, 1, -0.5F, 9F, -6F, 0.802852F, 0F, 0F, true);
        part(root, "lens1", 6, 0, 0F, 0F, 0F, 1, 3, 1, -0.5F, 13F, -0.5F, 0F, 0F, 0F, true);
        part(root, "lens2", 6, 0, 0F, 0F, 0F, 1, 3, 1, 0.366667F, 11.9333F, 1.1F, 0.371786F, 1.07818F, 0.185893F, true);
        part(root, "lens3", 6, 0, 0F, 0F, 0F, 1, 3, 1, -0.2F, 11.7333F, 1.2F, -0.111536F, -2.76753F, 0.334608F, false);
        part(root, "focus_right", 0, 0, 0F, 0F, 0F, 1, 2, 2, 3F, 17F, -7F, 0F, 0F, 0F, true);
        part(root, "focus_left", 0, 0, 0F, 0F, 0F, 1, 2, 2, -4F, 17F, -7F, 0F, 0F, 0F, true);
        part(root, "axle", 10, 0, 0F, 0F, 0F, 6, 1, 1, -3F, 17.5F, -6.5F, 0F, 0F, 0F, true);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void part(PartDefinition group, String name, int u, int v, float x, float y, float z, int w, int h, int d,
                             float px, float py, float pz, float rx, float ry, float rz, boolean mirror) {
        CubeListBuilder cube = CubeListBuilder.create().texOffs(u, v);
        if (mirror) {
            cube = cube.mirror();
        }
        group.addOrReplaceChild(name, cube.addBox(x, y, z, w, h, d), PartPose.offsetAndRotation(px, py, pz, rx, ry, rz));
    }
}
