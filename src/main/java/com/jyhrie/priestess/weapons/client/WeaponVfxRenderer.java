package com.jyhrie.priestess.weapons.client;

import com.jyhrie.priestess.weapons.entity.AegirWhirlpool;
import com.jyhrie.priestess.weapons.entity.WeaponVfx;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Draws every animated ability effect — every {@link WeaponVfx}, and {@link AegirWhirlpool},
 * which is not one. See {@link WeaponVfxModel} for why the pair is generic rather than fixed to
 * {@code WeaponVfx}; the precedent for a generic renderer in this package is
 * {@link DevilsProjectileRenderer}.
 *
 * <p>Rotation is applied from the entity's own yaw and pitch, which were set once at spawn and
 * never change. {@code super.applyRotations} is deliberately <b>not</b> called: its job is the
 * body-yaw and death-spin handling a living entity needs, none of which applies here, and
 * letting it run would rotate the mesh a second time on top of this.
 */
public class WeaponVfxRenderer<T extends Entity & GeoEntity> extends GeoEntityRenderer<T> {

    public WeaponVfxRenderer(EntityRendererProvider.Context context) {
        super(context, new WeaponVfxModel<>());
    }

    @Override
    protected void applyRotations(T entity, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTick) {
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
    }

    /** Translucent, so a slash reads as light rather than as a thrown lump of metal. */
    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(this.getTextureLocation(animatable));
    }

    /** Full-bright: these are fire and water, and either dimming in a corridor looks broken. */
    @Override
    protected int getBlockLightLevel(T entity, BlockPos pos) {
        return 15;
    }
}
