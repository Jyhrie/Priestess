package com.jyhrie.priestess.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * The one model class every non-humanoid placeholder in this mod uses.
 *
 * <p>It is a bag of cubes with an optional hover, and that is all it will ever be. The
 * mobs it draws are stand-ins; giving each of them a hand-written model with real
 * animation would be several hundred lines of work thrown away the moment somebody makes
 * actual art. What is worth having now is a distinct silhouette per mob — which comes from
 * the {@link net.minecraft.client.model.geom.builders.LayerDefinition} in
 * {@link PriestessModelLayers}, not from this class — and something that visibly moves for
 * the ones that fly, so a hovering drone does not read as a floating block.
 *
 * <p>Humanoid mobs deliberately do <em>not</em> come through here: they use vanilla's
 * {@code HumanoidModel} on the zombie layer, which already walks, swings and looks around.
 * Reimplementing that badly would be the worst of both worlds.
 */
public class PriestessEntityModel<T extends Entity> extends HierarchicalModel<T> {

    private final ModelPart root;

    /** Blocks of vertical bob, or 0 for a model that sits still. */
    private final float bobHeight;
    private final float restingY;

    public PriestessEntityModel(ModelPart root) {
        this(root, 0.0F);
    }

    public PriestessEntityModel(ModelPart root, float bobHeight) {
        this.root = root;
        this.bobHeight = bobHeight;
        this.restingY = root.y;
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        if (bobHeight != 0.0F) {
            // Driven by ageInTicks rather than by the walk cycle, so it keeps hovering while
            // standing still — which is the whole point of a hover.
            root.y = restingY + Mth.cos(ageInTicks * 0.15F) * bobHeight;
        }
    }
}
