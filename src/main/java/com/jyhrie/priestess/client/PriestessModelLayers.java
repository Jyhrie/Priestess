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
 * Geometry for the mobs that are neither humanoid shapes nor GeckoLib models — currently
 * just the slug.
 *
 * <p>The numbers are model units (16 to a block), origin at the mob's feet, +Y up, −Z forward.
 *
 * <p>The UV origins matter more than they look: a cube of size {@code (x, y, z)} needs
 * {@code 2(x + z)} pixels of texture width and {@code y + z} of height from its UV origin, and
 * a cube running off the edge of the texture renders as garbage rather than as an error.
 */
public final class PriestessModelLayers {

    /** Texture 64x32. Low and long, with two stalks so you can see which end is the head. */
    public static final ModelLayerLocation ORIGINIUM_SLUG = layer("originium_slug");

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

    private PriestessModelLayers() {
    }
}
