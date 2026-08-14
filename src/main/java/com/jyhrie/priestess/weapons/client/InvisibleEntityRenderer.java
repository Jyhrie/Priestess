package com.jyhrie.priestess.weapons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Draws nothing at all.
 *
 * <p>For entities whose entire appearance is the particles they emit — {@code AegirTide} is the
 * one so far. Every entity type must have a renderer bound or the game logs a missing-renderer
 * error and falls back, so "invisible" has to be said explicitly rather than by omission.
 *
 * <p>{@link #render} is overridden to a no-op rather than left to {@link EntityRenderer}'s: the
 * inherited one still draws name tags and leash lines, neither of which a jet of water has.
 */
public class InvisibleEntityRenderer<T extends Entity> extends EntityRenderer<T> {

    public InvisibleEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
    }

    /**
     * Never sampled, since nothing is drawn — but the contract is non-null, and the block atlas
     * is the one texture guaranteed to be loaded.
     */
    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
