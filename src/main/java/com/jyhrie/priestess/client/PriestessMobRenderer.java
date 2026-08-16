package com.jyhrie.priestess.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * One renderer for every mob drawn from a vanilla {@code EntityModel}.
 *
 * <p>A renderer per mob is the usual shape and the right one once mobs have particle trails,
 * glowing eyes and death animations. None of these do — they differ in three values, so they
 * get one class taking those as arguments. When a mob grows something a renderer has to know
 * about, give <em>that</em> mob its own subclass and leave the rest here.
 *
 * <p>The GeckoLib mobs cannot come here, because {@code MobRenderer} cannot host a
 * {@code GeoModel}; see {@link PriestessGeoRenderer}.
 */
public class PriestessMobRenderer<T extends Mob> extends MobRenderer<T, EntityModel<T>> {

    private final ResourceLocation texture;
    private final float scale;

    public PriestessMobRenderer(EntityRendererProvider.Context context, EntityModel<T> model,
                                float shadowRadius, ResourceLocation texture) {
        this(context, model, shadowRadius, texture, 1.0F);
    }

    public PriestessMobRenderer(EntityRendererProvider.Context context, EntityModel<T> model,
                                float shadowRadius, ResourceLocation texture, float scale) {
        super(context, model, shadowRadius);
        this.texture = texture;
        this.scale = scale;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }

    /**
     * Scaling here rather than in the geometry keeps the humanoid mobs on vanilla's zombie
     * mesh — a Rogue Power Armour is a zombie model at 1.25, which is a two-metre-forty
     * silhouette without a bespoke model to maintain.
     */
    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        if (scale != 1.0F) {
            poseStack.scale(scale, scale, scale);
        }
    }
}
