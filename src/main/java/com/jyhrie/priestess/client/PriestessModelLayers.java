package com.jyhrie.priestess.client;

import com.jyhrie.priestess.Priestess;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Geometry for the three mobs that are not humanoid shapes.
 *
 * <p>Each is a handful of cubes chosen to give a readable silhouette and nothing more —
 * see {@link PriestessEntityModel} for why placeholders stop there. The numbers are model
 * units (16 to a block), origin at the mob's feet, +Y up, −Z forward.
 *
 * <p>The UV origins matter more than they look: a cube of size {@code (x, y, z)} needs
 * {@code 2(x + z)} pixels of texture width and {@code y + z} of height starting at its UV
 * origin, and a cube that runs off the edge of the texture renders as garbage rather than
 * as an error. Every entry below is sized against the texture
 * {@code generate_placeholder_art.py} writes for it.
 */
public final class PriestessModelLayers {

    /** Texture 64x32. Low and long, with two stalks so you can see which end is the head. */
    public static final ModelLayerLocation ORIGINIUM_SLUG = layer("originium_slug");

    /** Texture 32x32. A body and a lens; the hover is in the model, not the geometry. */
    public static final ModelLayerLocation RHINE_SECURITY_DRONE = layer("rhine_security_drone");

    /** Texture 128x64. A mass with four nodes growing off it. */
    public static final ModelLayerLocation FAILED_VISION = layer("failed_vision");

    /** Texture 128x64. One cube, filling the hitbox exactly. */
    public static final ModelLayerLocation AWAKEN = layer("awaken");

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(new ResourceLocation(Priestess.MOD_ID, name), "main");
    }

    public static LayerDefinition slug() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-5.0F, -5.0F, -7.0F, 10.0F, 5.0F, 14.0F),
                // Hung from y=8 so the model sits on the ground given the 0.5-block hitbox.
                PartPose.offset(0.0F, 8.0F, 0.0F));

        root.addOrReplaceChild("left_stalk", CubeListBuilder.create()
                        .texOffs(0, 20)
                        .addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(2.0F, 3.0F, -6.0F));

        root.addOrReplaceChild("right_stalk", CubeListBuilder.create()
                        .texOffs(8, 20)
                        .addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(-2.0F, 3.0F, -6.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition drone() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -3.0F, -4.0F, 8.0F, 6.0F, 8.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));

        // The lens hangs off the front face, which is the only asymmetry the drone has and
        // therefore the only way to tell where it is aiming.
        body.addOrReplaceChild("lens", CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.0F, -2.0F, -1.0F, 4.0F, 4.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, -4.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    public static LayerDefinition failedVision() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition core = root.addOrReplaceChild("core", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-12.0F, -24.0F, -12.0F, 24.0F, 24.0F, 24.0F),
                // Grown out of the floor: the top of the cube is 24 units up, which fills
                // the 3-block hitbox with a little air above it.
                PartPose.offset(0.0F, 24.0F, 0.0F));

        // Four nodes on the corners. They do not disappear as they are destroyed — that
        // would need a per-node synched flag, which is more state than a placeholder should
        // carry; the boss bar counts them instead.
        node(core, "node_ne", 10.0F, -20.0F, -10.0F);
        node(core, "node_nw", -10.0F, -20.0F, -10.0F);
        node(core, "node_se", 10.0F, -20.0F, 10.0F);
        node(core, "node_sw", -10.0F, -20.0F, 10.0F);

        return LayerDefinition.create(mesh, 128, 64);
    }

    /**
     * A single 24-unit cube — 1.5 blocks, which is exactly the hitbox.
     *
     * <p>{@code PartPose.offset(0, 24, 0)} puts the part origin at the entity's feet (model
     * Y runs downward from an origin the renderer has already lifted 1.5 blocks), and the
     * box then runs from -24 to 0, i.e. straight up from there. So the cube occupies the
     * hitbox and nothing else, which is what makes it obvious where you can hit it.
     */
    public static LayerDefinition awaken() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("cube", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-12.0F, -24.0F, -12.0F, 24.0F, 24.0F, 24.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static void node(PartDefinition parent, String name, float x, float y, float z) {
        parent.addOrReplaceChild(name, CubeListBuilder.create()
                        .texOffs(96, 0)
                        .addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                PartPose.offset(x, y, z));
    }

    private PriestessModelLayers() {
    }
}
