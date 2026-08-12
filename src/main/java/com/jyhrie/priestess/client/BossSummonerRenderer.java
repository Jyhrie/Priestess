package com.jyhrie.priestess.client;

import com.jyhrie.priestess.block.BossSummonerBlock;
import com.jyhrie.priestess.block.entity.BossSummonerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.util.RenderUtils;

/**
 * Draws the boss altars, and turns the thing floating over them.
 *
 * <p>Bound to the block entity type rather than to either block, so both altars come through
 * here and {@link BossSummonerModel} decides what each one looks like.
 *
 * <h2>The spin is code, not a keyframed clip</h2>
 * A constant rotation is one line of arithmetic. An {@code .animation.json} would be another
 * file to keep in step with the model for the same result, so the core bone's rotation is
 * written directly in {@link #preRender} instead. Real animations — a flourish on summon, a
 * shudder on re-arm — are events rather than a constant, and those <em>would</em> want clips;
 * see {@code docs/BOSS_SPAWNERS.md}.
 *
 * <p>Driven off {@link RenderUtils#getCurrentTick()} plus the partial tick, so it is smooth
 * between ticks and identical on every altar in view. There is no per-altar phase offset,
 * which means a row of them turns in lockstep — deliberate, since they are meant to read as
 * one installation, and the place to change it if that looks wrong is here.
 *
 * <h2>A spent altar stops</h2>
 * The core holds still once the altar has been used, which is the second half of the
 * armed/spent read — the first being the darker texture. Motion catches the eye across a room
 * far more reliably than a colour does.
 */
public class BossSummonerRenderer extends GeoBlockRenderer<BossSummonerBlockEntity> {

    /** Degrees a second. Slow enough to read as hovering rather than as machinery. */
    private static final float SPIN_DEGREES_PER_SECOND = 24.0F;

    /** The bone the spin is applied to. Must match the name in the geo file. */
    private static final String CORE_BONE = "core";

    public BossSummonerRenderer() {
        super(new BossSummonerModel());
    }

    @Override
    public void preRender(PoseStack poseStack, BossSummonerBlockEntity altar, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        super.preRender(poseStack, altar, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha);

        if (!altar.getBlockState().getValue(BossSummonerBlock.ARMED)) {
            return;
        }

        double ticks = RenderUtils.getCurrentTick() + partialTick;
        float degrees = (float) (ticks * SPIN_DEGREES_PER_SECOND / 20.0) % 360.0F;

        // Absent when the model has no bone by that name — a hand-drawn replacement that
        // renamed it, say. Nothing turns and nothing crashes, which is the right failure for
        // something purely cosmetic.
        getGeoModel().getBone(CORE_BONE)
                .ifPresent(bone -> bone.setRotY((float) Math.toRadians(degrees)));
    }
}
