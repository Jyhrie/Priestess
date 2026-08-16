package com.jyhrie.priestess.client;

import com.jyhrie.priestess.block.BossSummonerBlock;
import com.jyhrie.priestess.block.entity.BossSummonerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.util.RenderUtils;

/**
 * Draws the boss altars, and turns the thing floating over them.
 *
 * <p>Bound to the block entity type rather than to either block, so both altars come through
 * here and {@link BossSummonerModel} decides what each one looks like.
 *
 * <p>The spin is code rather than a keyframed clip: a constant rotation is one line of
 * arithmetic, where an {@code .animation.json} is another file to keep in step with the model.
 * Real animations — a flourish on summon, a shudder on re-arm — are events and <em>would</em>
 * want clips; see {@code docs/BOSS_SPAWNERS.md}.
 *
 * <p>There is no per-altar phase offset, so a row of them turns in lockstep. Deliberate, since
 * they are meant to read as one installation.
 *
 * <p>A spent altar's core holds still, which is the second half of the armed/spent read — the
 * first being the darker texture. Motion catches the eye far more reliably than colour.
 */
public class BossSummonerRenderer extends GeoBlockRenderer<BossSummonerBlockEntity> {

    /** Degrees a second. Slow enough to read as hovering rather than as machinery. */
    private static final float SPIN_DEGREES_PER_SECOND = 24.0F;

    /** The bone the spin is applied to. Must match the name in the geo file. */
    private static final String CORE_BONE = "core";

    public BossSummonerRenderer() {
        super(new BossSummonerModel());
    }

    /**
     * A spent altar draws nothing at all.
     *
     * <p>It has stood down for the duration of the fight — no collision, no outline, and not an
     * obstacle to pathfinding either; see {@code BossSummonerBlock}. Drawing it would be the one
     * part of that left inconsistent, and an altar you can see but walk through is worse than
     * one that is simply gone.
     *
     * <p>Returning early here rather than hiding it in the model is deliberate: this skips the
     * whole GeckoLib render path — the bone walk and the vertex emission — rather than doing all
     * of it and producing invisible geometry.
     *
     * <p>This is {@code shouldRender} rather than an early return in {@code render} because
     * {@code render} cannot be overridden here — GeckoLib declares it with the raw
     * {@link net.minecraft.world.level.block.entity.BlockEntity} parameter rather than with the
     * renderer's own type variable, so any signature a subclass writes is a name clash rather
     * than an override. {@code shouldRender} is a default on
     * {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer} that GeckoLib leaves
     * alone, and the dispatcher consults it first, so it skips strictly more work anyway.
     *
     * <p>The distance test below is vanilla's default behaviour, restored by hand: the default
     * cannot be delegated to with {@code super} from here, because the class that implements the
     * interface is GeckoLib's rather than this one. Dropping it would leave the altars drawing
     * at any range their chunk is loaded at.
     */
    @Override
    public boolean shouldRender(BossSummonerBlockEntity altar, Vec3 cameraPos) {
        BlockState state = altar.getBlockState();
        // hasProperty guards a malformed world where this block entity ended up under
        // something that is not a summoner. Always true in a sane one.
        if (state.hasProperty(BossSummonerBlock.ARMED) && !state.getValue(BossSummonerBlock.ARMED)) {
            return false;
        }
        return Vec3.atCenterOf(altar.getBlockPos()).closerThan(cameraPos, getViewDistance());
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
