package com.jyhrie.priestess.weapons.client;

import com.jyhrie.priestess.weapons.entity.DevilsProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Draws both of Devil's Devastation's projectiles.
 *
 * <p>One class for both, for the same reason {@link com.jyhrie.priestess.client.PriestessGeoRenderer}
 * is one class for fourteen mobs: they differ only in which {@link GeoModel} they hand up.
 *
 * <h2>Why it aims itself</h2>
 * These entities are moved by hand — {@code DevilsProjectile.tick} steps the position and
 * never calls {@code super.tick()} — so nothing ever writes their yaw and pitch fields, and
 * the default renderer would draw every one of them facing due south while it flew sideways.
 * {@link #applyRotations} derives the facing from the velocity vector instead, which is the
 * only place that information exists.
 */
public class DevilsProjectileRenderer<T extends DevilsProjectile> extends GeoEntityRenderer<T> {

    public DevilsProjectileRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
    }

    @Override
    protected void applyRotations(T entity, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTick) {
        super.applyRotations(entity, poseStack, ageInTicks, rotationYaw, partialTick);

        double dx = entity.getDeltaMovement().x;
        double dy = entity.getDeltaMovement().y;
        double dz = entity.getDeltaMovement().z;

        float horizontal = Mth.sqrt((float) (dx * dx + dz * dz));
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0F / Math.PI)) - 90.0F;
        float pitch = (float) (Mth.atan2(dy, horizontal) * (180.0F / Math.PI));

        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
    }

    /** Translucent, so the blade reads as a projected afterimage rather than a thrown object. */
    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(this.getTextureLocation(animatable));
    }

    /** Full-bright: it is supposed to glow, and a scythe that dims in a corridor looks broken. */
    @Override
    protected int getBlockLightLevel(T entity, BlockPos pos) {
        return 15;
    }
}
